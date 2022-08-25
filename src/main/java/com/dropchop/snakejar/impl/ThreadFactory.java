package com.dropchop.snakejar.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public class ThreadFactory implements java.util.concurrent.ThreadFactory {

  private final Logger log = LoggerFactory.getLogger(ThreadFactory.class);
  private static final long SLEEP_INTERVAL = 50;
  private static final AtomicInteger nextId  = new AtomicInteger(0);
  private final AtomicInteger counter = new AtomicInteger(0);
  private final long terminationTimeout;
  public ThreadFactory(long terminationTimeout) {
    this.terminationTimeout = terminationTimeout;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    String name = "SnakeJar-" + nextId.getAndIncrement();
    log.trace("Creating thread [{}]...", name);
    Thread thread = new Thread(runnable, name);
    counter.incrementAndGet();
    log.debug("Created thread [{}].", name);
    return thread;
  }

  @SuppressWarnings("unused")
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
        //noinspection BusyWait
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
