package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.impl.SnakeJarEmbedded.PythonEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 13. 11. 21.
 */
class SnakeJarEmbeddedTest {

  @Test
  void getPythonVersion() {
    PythonEnvironment pythonEnvironment = SnakeJarEmbedded.getPythonEnvironment(true);
    assertNotNull(pythonEnvironment);
    assertNotNull(pythonEnvironment.version);
    System.setProperty(SnakeJarEmbedded.PY_VER_SYS_PROP, "4.1");
    System.setProperty(SnakeJarEmbedded.PY_LIB_SYS_PROP, "/usr/lib/libpython3.9.so");
    pythonEnvironment = SnakeJarEmbedded.getPythonEnvironment(false);
    assertEquals("4.1", pythonEnvironment.version);
    assertEquals("/usr/lib/libpython3.9.so", pythonEnvironment.libPath);
  }
}