package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.InterpreterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public class ThreadFactory implements java.util.concurrent.ThreadFactory {

  final Logger log = LoggerFactory.getLogger(ThreadFactory.class);

  private static final long SLEEP_INTERVAL = 50;
  private static final AtomicInteger nextId  = new AtomicInteger(0);

  private final AtomicInteger counter = new AtomicInteger(0);
  private final long terminationTimeout;
  private final InterpreterFactory interpreterFactory;

  public ThreadFactory(long terminationTimeout, InterpreterFactory interpreterFactory) {
    this.terminationTimeout = terminationTimeout;
    this.interpreterFactory = interpreterFactory;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    String name = "SnakeJar-" + nextId.getAndIncrement();
    log.trace("Creating [{}] thread [{}]...", interpreterFactory, name);
    Thread thread = new Thread(this, runnable, name);
    counter.incrementAndGet();
    log.debug("Created [{}] thread [{}].", interpreterFactory, name);
    return thread;
  }

  public InterpreterFactory getInterpreterFactory() {
    return interpreterFactory;
  }

  protected void signalTermination() {
    counter.decrementAndGet();
  }

  public boolean allThreadsTerminated() {
    return counter.get() <= 0;
  }

  public void blockUntilAllTerminated() {
    long numRuns = 0;
    while (!allThreadsTerminated()) {
      try {
        Thread.sleep(SLEEP_INTERVAL);
        numRuns++;
      } catch (InterruptedException ignored) {
      }
      if (numRuns * SLEEP_INTERVAL >= this.terminationTimeout) {
        break;
      }
    }
  }
}
