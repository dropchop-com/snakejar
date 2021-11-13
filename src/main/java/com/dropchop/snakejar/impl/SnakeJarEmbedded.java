package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.Interpreter;
import com.dropchop.snakejar.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 29. 10. 21.
 */
public class SnakeJarEmbedded extends SnakeJarBase {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarEmbedded.class);
  public static final String LIB_NAME = "snakejar";
  public static final String PY_VER_SYS_PROP = "snakejar.python.version";
  private static final String PY_VER_CMD = "python --version";
  private static final Pattern PY_VER_PATT = Pattern.compile("[\\w]*([\\d]+\\.[\\d]+)");

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

  protected static String getPythonVersion(boolean forceFromProgram) {
    String pythonVersion = System.getProperty(PY_VER_SYS_PROP);
    if (forceFromProgram || pythonVersion == null || pythonVersion.isEmpty()) {
      StringBuilder result = new StringBuilder();
      try {
        String line;
        Process p = Runtime.getRuntime().exec(PY_VER_CMD);
        p.waitFor();
        if (p.exitValue() == 0) {
          BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
          while ((line = bri.readLine()) != null) {
            result.append(line);
          }
        } else {
          LOG.error("Got [{}] for [{}}] execution. Unable to detect python version and system property [{}] is also not given!",
            p.exitValue(), PY_VER_CMD, PY_VER_SYS_PROP);
        }

      } catch (Exception err) {
        err.printStackTrace();
      }
      Matcher matcher = PY_VER_PATT.matcher(result);
      if (matcher.find() && matcher.groupCount() > 0) {
        pythonVersion = matcher.group(1);
      } else {
        pythonVersion = result.toString();
      }

      LOG.debug("Got python version [{}] from execution [{}] result [{}].", pythonVersion, PY_VER_CMD, result);
    } else {
      LOG.debug("Got python version [{}] from execution [{}].", pythonVersion, PY_VER_CMD);
    }
    return pythonVersion;
  }

  @Override
  protected void _load() {
    String pythonVersion = getPythonVersion(false);
    try {
      System.loadLibrary(LibraryLoader.getLibraryBaseName(LIB_NAME, pythonVersion));
    } catch (UnsatisfiedLinkError e) {
      LOG.warn("Unable to load library with message: {}!", e.getMessage());
      LOG.info("Will try to load from jar!");
      try {
        File f = LibraryLoader.fromJarToTemp(LIB_NAME, pythonVersion, Thread.currentThread().getContextClassLoader());
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
