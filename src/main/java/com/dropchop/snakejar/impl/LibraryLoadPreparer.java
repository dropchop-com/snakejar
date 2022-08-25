package com.dropchop.snakejar.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 6. 11. 21.
 */
public class LibraryLoadPreparer {
  private static final String OS = System.getProperty("os.name").toLowerCase();
  private static final String ARCH = System.getProperty("os.arch").toLowerCase();

  public static boolean isWindows() {
    return (OS.contains("win"));
  }

  public static boolean isFreeBSD() {
    return (OS.contains("freebsd"));
  }

  public static boolean isMac() {
    return (OS.contains("mac"));
  }

  public static boolean isUnix() {
    return OS.contains("nix") ||
      OS.contains("nux");
  }

  public static boolean isOpenBSD() {
    return (OS.contains("openbsd"));
  }

  public static boolean is64Bit() {
    return (ARCH.indexOf("64") > 0);
  }

  public static String osArchSuffix(final String name, final String pythonVersion) {
    if (isUnix()) {
      return String.format("%s-linux-py%s-%s", name, pythonVersion, is64Bit() ? "x64" : "");
    } else if (isWindows()) {
      return String.format("%s-win-py%s-%s", name, pythonVersion, is64Bit() ? "x64" : "");
    } else if (isMac()) {
      return String.format("%s-osx-py%s", name, pythonVersion);
    }

    throw new UnsupportedOperationException(
      String.format("Cannot determine JNI library name for ARCH='%s' OS='%s' name='%s'", ARCH, OS, name)
    );
  }

  public static String osArchPrefix() {
    if (isUnix()) {
      return "lib";
    } else if (isWindows()) {
      return "";
    } else if (isMac()) {
      return "";
    }

    throw new UnsupportedOperationException(
      String.format("Cannot determine JNI library name for ARCH='%s' OS='%s'", ARCH, OS)
    );
  }

  public static String getLibraryBaseName(final String name, final String pythonVersion) {
    return osArchSuffix(name, pythonVersion);
  }

  private static String osExtension() {
    if (isUnix() || isFreeBSD() || isOpenBSD()) {
      return  ".so";
    } else if (isMac()) {
      return ".dylib";
    } else if (isWindows()) {
      return ".dll";
    }
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unused")
  public static String getLibraryFileName(final String name, final String pythonVersion) {
    return  getLibraryBaseName(name, pythonVersion) + osExtension();
  }

  static void fromJar(final File temp, final String fileName, final ClassLoader classLoader) throws IOException {
    try (final InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (is == null) {
        throw new RuntimeException("Missing [" + fileName + "] in jar.");
      } else {
        Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  @SuppressWarnings("SameParameterValue")
  static File fromJarToTemp(final String baseName, final String pythonVersion, final ClassLoader classLoader)
    throws IOException {
    final File temp;
    temp = File.createTempFile(osArchPrefix() + getLibraryBaseName(baseName, pythonVersion), osExtension());

    if (!temp.exists()) {
      throw new RuntimeException("File " + temp.getAbsolutePath() + " does not exist.");
    } else {
      temp.deleteOnExit();
    }
    String libFileName = osArchPrefix() + getLibraryBaseName(baseName, pythonVersion) + osExtension();

    fromJar(temp, libFileName, classLoader);
    return temp;
  }
}
