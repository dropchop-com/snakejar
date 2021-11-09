package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.Interpreter;
import com.dropchop.snakejar.InterpreterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 15. 10. 21.
 */
public class Thread extends java.lang.Thread {
  final Logger log = LoggerFactory.getLogger(Thread.class);
  private final ThreadFactory threadFactory;

  public Thread(ThreadFactory threadFactory, Runnable target, String name) {
    super(target, name);
    this.threadFactory = threadFactory;
  }

  @Override
  public void run() {
    log.trace("Running thread...");
    try {
      super.run();
    } finally {
      log.debug("Stopping thread.");
      log.trace("Destroying interpreter.");
      InterpreterFactory interpreterFactory = threadFactory.getInterpreterFactory();
      Interpreter interpreter = interpreterFactory.getInterpreter();
      try {
        interpreterFactory.destroyInterpreter(interpreter);
      } catch (Exception e) {
        log.error("Error destroying interpreter!", e);
      }
      this.threadFactory.signalTermination();
      log.debug("Stopped thread.");
    }
  }
}
