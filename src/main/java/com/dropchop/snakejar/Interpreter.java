package com.dropchop.snakejar;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public interface Interpreter {

  boolean supportsMultithreading();

  void compile(List<Source<?>> sources) throws IOException;

  <V> V invoke(String moduleName, String functionName,
               Class<V> retType, Object... args);
  <V> V invoke(String moduleName, String functionName,
               Class<V> retType, Map<String, ?> kwargs, Object... args);

  <V> V invoke(String moduleName, String className, String functionName,
               Class<V> retType, Object... args);
  <V> V invoke(String moduleName, String className, String functionName,
               Class<V> retType, Map<String, ?> kwargs, Object... args);

  <V> V invoke(String moduleName, String className, String objectName, String functionName,
               Class<V> retType, Object... args);
  <V> V invoke(String moduleName, String className, String objectName, String functionName,
               Class<V> retType, Map<String, ?> kwargs, Object... args);
}
