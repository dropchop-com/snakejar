package com.dropchop.snakejar.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 13. 11. 21.
 */
class SnakeJarEmbeddedTest {

  @Test
  void getPythonVersion() {
    String pythonVersion = SnakeJarEmbedded.getPythonVersion(true);
    assertNotNull(pythonVersion);
    System.setProperty(SnakeJarEmbedded.PY_VER_SYS_PROP, "4.1");
    pythonVersion = SnakeJarEmbedded.getPythonVersion(false);
    assertEquals("4.1", pythonVersion);
  }
}