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

  public static class PythonEnvironment {
    public final String version;
    public final String libPath;

    public PythonEnvironment(String version, String libPath) {
      this.version = version;
      this.libPath = libPath;
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarEmbedded.class);

  private static final String PY_CMD = "python";
  private static final String PY_CMD_RET_DELIM = ";;";
  private static final String PY_CMD_SCRIPT_NAME = "pyverlib";
  private static final String PY_CMD_SCRIPT_EXT = ".py";
  private static final String PY_CMD_SCRIPT = PY_CMD_SCRIPT_NAME + PY_CMD_SCRIPT_EXT;

  private static final Pattern PY_VER_PATT = Pattern.compile("[\\w]*([\\d]+\\.[\\d]+)");

  public static final String LIB_NAME = "snakejar";
  public static final String PY_VER_SYS_PROP = "snakejar.python.version";
  public static final String PY_LIB_SYS_PROP = "snakejar.pylib.location";

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

  protected static PythonEnvironment getPythonEnvironment(boolean forceFromProgram) {
    String pythonVersion = System.getProperty(PY_VER_SYS_PROP);
    String pythonLibPath = System.getProperty(PY_LIB_SYS_PROP);
    if (forceFromProgram || pythonVersion == null || pythonVersion.isEmpty()
        || pythonLibPath == null || pythonLibPath.isEmpty()) {
      StringBuilder result = new StringBuilder();
      try {
        File temp = File.createTempFile(PY_CMD_SCRIPT_NAME, PY_CMD_SCRIPT_EXT);
        LibraryLoadPreparer.fromJar(temp, PY_CMD_SCRIPT, Thread.currentThread().getContextClassLoader());

        String line;
        LOG.info("Running python environment detection [{}]", PY_CMD + " " + temp.getAbsolutePath());
        Process p = Runtime.getRuntime().exec(new String[]{PY_CMD, temp.getAbsolutePath()});
        p.waitFor();
        if (p.exitValue() == 0) {
          BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
          while ((line = bri.readLine()) != null) {
            result.append(line);
          }
        } else {
          LOG.error("Got [{}] for [{}] execution. Unable to detect python version and system property [{}] is also not given!",
            p.exitValue(), PY_CMD, PY_VER_SYS_PROP);
        }

      } catch (Exception err) {
        err.printStackTrace();
      }
      String resultStr = result.toString();
      if (!resultStr.contains(PY_CMD_RET_DELIM)) {
        LOG.error("Got invalid result [{}]. Unable to parse version and lib location!", result);
        return null;
      }
      String[] results = resultStr.split(PY_CMD_RET_DELIM);
      if (results.length != 2) {
        LOG.error("Got invalid result [{}]. Unable to parse version and lib location!", result);
        return null;
      }
      Matcher matcher = PY_VER_PATT.matcher(results[0]);// parse out just major.minor
      if (matcher.find() && matcher.groupCount() > 0) {
        pythonVersion = matcher.group(1);
      } else {
        pythonVersion = results[0];
      }
      pythonLibPath = results[1];
      LOG.debug("Got python version [{}] and lib path [{}] from execution [{}] result [{}].",
        pythonVersion, pythonLibPath, PY_CMD, result);
      System.setProperty(PY_LIB_SYS_PROP, pythonLibPath);
    } else {
      LOG.debug("Got python version [{}] and lib path [{}] from system propreties.",
        pythonVersion, pythonLibPath);
    }
    return new PythonEnvironment(pythonVersion, pythonLibPath);
  }

  @Override
  protected void _load() {
    PythonEnvironment pythonEnvironment = getPythonEnvironment(false);
    if (pythonEnvironment == null) {
      throw new RuntimeException("Unable to detect Python runtime environment version and dynamic library path!");
    }
    try {
      System.loadLibrary(LibraryLoadPreparer.getLibraryBaseName(LIB_NAME, pythonEnvironment.version));
    } catch (UnsatisfiedLinkError e) {
      LOG.warn("Unable to load library with message: {}!", e.getMessage());
      LOG.info("Will try to load from jar!");
      try {
        File f = LibraryLoadPreparer.fromJarToTemp(LIB_NAME, pythonEnvironment.version,
          Thread.currentThread().getContextClassLoader());
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
