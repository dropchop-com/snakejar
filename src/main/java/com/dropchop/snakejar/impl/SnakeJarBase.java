package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.*;
import com.dropchop.snakejar.Invoker.Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 29. 10. 21.
 */
public abstract class SnakeJarBase implements SnakeJar, InterpreterFactory {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarBase.class);

  private final ThreadFactory threadFactory = new ThreadFactory(60, this);

  private final Map<String, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();
  protected volatile State state = State.UNLOADED;

  SnakeJarBase() {
  }

  protected abstract String getDefaultThreadPoolName();
  protected abstract Params getDefaultInvokerParams();

  protected abstract void _load();
  protected abstract void _initialize();
  protected abstract void _destroy();
  protected abstract void _unload();

  public boolean isLoaded() {
    return this.state.atLeast(State.LOADED);
  }

  public boolean isInitialized() {
    return this.state.atLeast(State.READY);
  }

  @Override
  public synchronized SnakeJar load() {
    LOG.trace("Loading ...");
    if (this.state == State.UNLOADED) {
      try {
        this._load();
      } catch (Exception e) {
        throw new RuntimeException("Unable to load Python [" + e.getMessage() + "]!", e);
      }
      this.state = State.LOADED;
      LOG.info("Loaded");
    } else {
      LOG.trace("Skipping loading ...");
    }
    return this;
  }

  @Override
  public synchronized SnakeJar initialize() {
    LOG.trace("Initializing ...");
    if (this.state == State.LOADED) {
      try {
        this._initialize();
      } catch (Exception e) {
        throw new RuntimeException("Unable to initialize Python [" + e.getMessage() + "]!", e);
      }
      this.state = State.READY;
      LOG.info("Initialized");
    } else {
      LOG.trace("Skipping initialization ...");
    }
    return this;
  }

  void shutdownPool(ExecutorService threadPool) {
    threadPool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
          LOG.error("Pool did not terminate");
        }
      }
      LOG.info("Thread pool {}", threadPool.isTerminated());
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      threadPool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public synchronized SnakeJar destroy() {
    if (this.state == State.READY) {
      this.state = State.DESTROYED;
      LOG.trace("Shutting down interpreter thread pools...");
      for (Map.Entry<String, ExecutorService> entry : this.executorServiceMap.entrySet()) {
        LOG.trace("Shutting down interpreter thread pool [{}] ...", entry.getKey());
        this.shutdownPool(entry.getValue());
        LOG.debug("Interpreter thread pool [{}] stopped.", entry.getKey());
      }
      this.executorServiceMap.clear();
      LOG.debug("Interpreter thread pools stopped.");
      //this is very important all threads must call interpreter destroy before Python cleanup
      this.threadFactory.blockUntilAllTerminated();
      LOG.debug("Interpreter threads terminated.");
      LOG.trace("Starting Python cleanup...");
      try {
        this._destroy();
      } catch (Exception e) {
        throw new RuntimeException("Unable to destroy Python [" + e.getMessage() + "]!", e);
      }
      LOG.debug("Python cleanup done.");
    } else {
      LOG.trace("Skipping destroying ...");
    }
    return this;
  }

  @Override
  public SnakeJar unload() {
    LOG.trace("Unloading ...");
    if (this.state == State.LOADED || this.state == State.DESTROYED) {
      try {
        this._unload();
      } catch (Exception e) {
        throw new RuntimeException("Unable to unload Python [" + e.getMessage() + "]!", e);
      }
      this.state = State.UNLOADED;
      LOG.info("Unloaded");
    } else {
      LOG.trace("Skipping unloading ...");
    }
    return this;
  }

  public Invoker prep(String poolName, Params params, List<Source<?>> sources) throws ExecutionException, InterruptedException {
    LOG.trace("Preparing ...");
    if (poolName == null) {
      poolName = getDefaultThreadPoolName();
    }

    ExecutorService pool;
    if (params.getNumCoreThreads() == 1 && params.getMaxThreads() == 1) {
      pool = executorServiceMap.computeIfAbsent(poolName,
        s -> Executors.newSingleThreadExecutor(this.threadFactory)
      );
      LOG.debug("Created newSingleThreadExecutor.");
    } else {
      pool = executorServiceMap.computeIfAbsent(poolName,
        s -> {
          BlockingQueue<Runnable> queue;
          if (params.getNumCoreThreads() >= params.getMaxThreads()) {
            queue = new LinkedBlockingQueue<>();
          } else {
            queue = new SynchronousQueue<>();
          }
          return new ThreadPoolExecutor(
            params.getNumCoreThreads(),
            params.getMaxThreads(),
            params.getKeepAliveTimeout(),
            params.getUnit(),
            queue,
            this.threadFactory
          );
        }
      );
      LOG.debug("Created ThreadPoolExecutor.");
    }
    new CompileInvoker(this, pool, sources).apply((Invocation<?>)null, null).get();

    LOG.trace("Prepared [{}].", this.executorServiceMap);
    return new Invoker(this, pool);
  }

  @Override
  public Invoker prep(String poolName, Params params, Source<?> source) throws ExecutionException, InterruptedException {
    return prep(poolName, params, List.of(source));
  }

  @Override
  public Invoker prep(String poolName, List<Source<?>> sources) throws ExecutionException, InterruptedException {
    return prep(poolName, getDefaultInvokerParams(), sources);
  }

  @Override
  public Invoker prep(String poolName, Source<?> source) throws ExecutionException, InterruptedException {
    return prep(poolName, getDefaultInvokerParams(), List.of(source));
  }

  @Override
  public com.dropchop.snakejar.Invoker prep(Params params, List<Source<?>> sources) throws ExecutionException, InterruptedException {
    return prep(getDefaultThreadPoolName(), params, sources);
  }

  @Override
  public com.dropchop.snakejar.Invoker prep(Params params, Source<?> source) throws ExecutionException, InterruptedException {
    return prep(getDefaultThreadPoolName(), params, source);
  }

  @Override
  public Invoker prep(List<Source<?>> sources) throws ExecutionException, InterruptedException {
    return prep(getDefaultThreadPoolName(), getDefaultInvokerParams(), sources);
  }

  @Override
  public Invoker prep(Source<?> source) throws ExecutionException, InterruptedException {
    return prep(getDefaultThreadPoolName(), getDefaultInvokerParams(), List.of(source));
  }
}
