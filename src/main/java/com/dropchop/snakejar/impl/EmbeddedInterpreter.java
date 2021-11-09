package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.ModuleSource;
import com.dropchop.snakejar.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public class EmbeddedInterpreter implements com.dropchop.snakejar.Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(EmbeddedInterpreter.class);

  private static final String PREFIX_CP = "classpath://";
  private static final int PREFIX_CP_LEN = PREFIX_CP.length();

  EmbeddedInterpreter() {
  }

  private final Map<String, ByteBuffer> compiledModules = new HashMap<>();

  protected native void _compile(String moduleName, String fileName, String moduleSource);
  protected native void _free_module(String moduleName, ByteBuffer module);

  synchronized void reset() {
    LOG.trace("Resetting interpreter...");
    Map<String, ByteBuffer> modules = new HashMap<>(this.compiledModules);
    this.compiledModules.clear();
    for (Map.Entry<String, ByteBuffer> modEntry : modules.entrySet()) {
      this._free_module(modEntry.getKey(), modEntry.getValue());
    }
    LOG.debug("Interpreter reset.");
  }

  protected void registerCompiledModule(String moduleName, ByteBuffer compiledModulePtr) {
    LOG.trace("Registering compiled module [{}]...", moduleName);
    this.compiledModules.put(moduleName, compiledModulePtr);
  }

  protected ByteBuffer getCompiledModule(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Missing compiled module name parameter!");
    }
    ByteBuffer compiledModule = compiledModules.get(name);
    LOG.trace("Returning compiled module [{}] for name [{}].", compiledModule, name);
    return compiledModule;
  }

  protected String readFromInputStream(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }

  protected String readFromClasspath(String path) throws IOException {
    String tmp = path.toLowerCase();
    if (tmp.startsWith(PREFIX_CP) && path.length() > PREFIX_CP_LEN) {
      path = path.substring(PREFIX_CP_LEN);
      path = path.replaceAll("^[/]+", "");
    } else {
      throw new IOException("Invalid class path [" + path + "] specified!");
    }
    ClassLoader cl = java.lang.Thread.currentThread().getContextClassLoader();
    InputStream is = cl.getResourceAsStream(path);
    if (is == null) {
      throw new IOException("Unable to locate class path [" + path + "] resource!");
    }
    return readFromInputStream(is);
  }

  protected String getSourceAsString(Source<?> source) throws IOException {
    String sourceString;
    Object src = source.get();
    if (src instanceof String) {
      String tmp = ((String) src).trim().toLowerCase();
      if (tmp.startsWith("classpath://")) {
        try {
          sourceString = readFromClasspath((String) src);
        } catch (Exception e) {
          throw new IOException("Unable to read [" + src + "] source classpath!", e);
        }
      } else {
        sourceString = (String) src;
      }
    } else if (src instanceof Path) {
      try {
        sourceString = Files.readString((Path) src, StandardCharsets.UTF_8);
      } catch (Exception e) {
        throw new IOException("Unable to read [" + src + "] source Path!", e);
      }
    } else if (src instanceof File) {
      try {
        sourceString = Files.readString(((File) src).toPath(), StandardCharsets.UTF_8);
      } catch (Exception e) {
        throw new IOException("Unable to read [" + src + "] source File!", e);
      }
    } else {
      throw new IllegalArgumentException("Supplied source [" + src + "] is invalid type use String, Path, File!");
    }
    if (sourceString == null || sourceString.isEmpty()) {
      throw new IllegalArgumentException("Resolved supplied source is null or empty!");
    }
    return sourceString;
  }

  @Override
  public synchronized void compile(List<Source<?>> sources) throws IOException {
    for (Source<?> source : sources) {
      String moduleName = source.getModuleName();
      if (moduleName == null || moduleName.isEmpty()) {
        throw new IllegalArgumentException("Supplied module name is null or empty!");
      }
      if (source instanceof ModuleSource) {
        Object src = ((ModuleSource<?>) source).get();
        if (src == null) {
          throw new IllegalArgumentException("Supplied source is null!");
        }
        String sourceString = this.getSourceAsString(source);
        String fileName = moduleName + ".py";
        if (src instanceof Path) {
          fileName = ((Path) src).normalize().toAbsolutePath().toString();
        } else if (src instanceof File) {
          fileName = ((File) src).toPath().normalize().toAbsolutePath().toString();
        } else if (sourceString.startsWith("classpath://")) {
          fileName = Path.of(
            System.getProperty("user.dir") +
              FileSystems.getDefault().getSeparator() +
              sourceString.trim().substring(12).replace("/", FileSystems.getDefault().getSeparator()))
          .normalize().toAbsolutePath().toString();
        }
        LOG.trace("Will try to compile [{}][{}][{}]", moduleName, moduleName + ".py", sourceString);
        try {
          this._compile(moduleName, fileName, sourceString);
        } catch (Exception e) {
          LOG.error("Unable to compile [{}][{}][{}]", moduleName, moduleName + ".py", sourceString, e);
          throw new IOException(e);
        }
      }
    }
  }

  protected native <V> V _invoke_func(ByteBuffer module, String moduleName, String functionName,
                                      Class<V> retType, Map<String, ?> kwargs, Object... args);

  @Override
  public <V> V invoke(String moduleName, String functionName,
                      Class<V> retType, Map<String, ?> kwargs, Object... args) {
    return this._invoke_func(this.getCompiledModule(moduleName), moduleName, functionName, retType, kwargs, args);
  }

  @Override
  public <V> V invoke(String moduleName, String functionName,
                      Class<V> retType, Object... args) {
    return this.invoke(moduleName, functionName, retType, new HashMap<>(), args);
  }


  protected native <V> V _invoke_class(ByteBuffer module, String moduleName, String className, String functionName,
                                       Class<V> retType, Map<String, ?> kwargs, Object... args);

  @Override
  public <V> V invoke(String moduleName, String className, String functionName,
                      Class<V> retType, Map<String, ?> kwargs, Object... args) {
    return this._invoke_class(this.getCompiledModule(moduleName), moduleName, className, functionName, retType, kwargs, args);
  }

  @Override
  public <V> V invoke(String moduleName, String className, String functionName,
                      Class<V> retType, Object... args) {
    return this.invoke(moduleName, className, functionName, retType, new HashMap<>(), args);
  }

  protected native <V> V _invoke_object(ByteBuffer module, String moduleName, String className, String objectName, String functionName,
                                        Class<V> retType, Map<String, ?> kwargs, Object... args);

  @Override
  public <V> V invoke(String moduleName, String className, String objectName, String functionName,
                      Class<V> retType, Map<String, ?> kwargs, Object... args) {
    return this._invoke_object(this.getCompiledModule(moduleName), moduleName, className, objectName, functionName, retType, kwargs, args);
  }

  @Override
  public <V> V invoke(String moduleName, String className, String objectName, String functionName, Class<V> retType, Object... args) {
    return this.invoke(moduleName, className, objectName, functionName, retType, new HashMap<>(), args);
  }
}
