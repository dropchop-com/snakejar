package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.Interpreter;
import com.dropchop.snakejar.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 29. 10. 21.
 */
public class SnakeJarEmbedded extends SnakeJarBase {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarEmbedded.class);
  private static final String LIB_NAME = "snakejar";

  /*
  protected static final ThreadLocal<Interpreter> interpreter = ThreadLocal.withInitial(() -> {
    LOG.trace("Interpreter creating...");
    Interpreter interpreter = null;
    try {
      interpreter = new EmbeddedInterpreter();
      LOG.debug("Interpreter created.");
    } catch (Exception e) {
      LOG.debug("Unable to create interpreter.", e);
    }
    return interpreter;
  });
  */
  private final EmbeddedInterpreter embeddedInterpreter = new EmbeddedInterpreter();

  SnakeJarEmbedded() {
  }



  @Override
  public com.dropchop.snakejar.Interpreter getInterpreter() {
    return embeddedInterpreter;
  }

  @Override
  public void destroyInterpreter(Interpreter interpreter) {
    if (interpreter instanceof EmbeddedInterpreter) {
      ((EmbeddedInterpreter) interpreter).reset();
    }
  }

  @Override
  protected String getDefaultThreadPoolName() {
    return "SnakeJarEmbedded-ThreadPool";
  }

  @Override
  protected Invoker.Params getDefaultInvokerParams() {
    return new Invoker.Params(1, 1, 300L, TimeUnit.SECONDS);
  }

  @Override
  protected void _load() {
    try {
      System.loadLibrary(LibraryLoader.getLibraryBaseName(LIB_NAME));
    } catch (UnsatisfiedLinkError e) {
      LOG.warn("Unable to load library with message: {}!", e.getMessage());
      LOG.info("Will try to load from jar!");
      try {
        File f = LibraryLoader.fromJarToTemp(LIB_NAME, Thread.currentThread().getContextClassLoader());
        try {
          System.load(f.getAbsolutePath());
        } catch (UnsatisfiedLinkError ex) {
          throw new RuntimeException("Unable to load library file from temp path [" + f + "]!", ex);
        }
      } catch (IOException ex) {
        throw new RuntimeException("Unable to load library file from jar!", ex);
      }
    }
  }

  @Override
  protected native void _initialize();

  @Override
  protected native void _destroy();

  @Override
  public void _unload() {
  }
}
