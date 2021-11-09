package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.SnakeJar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 30. 10. 21.
 */
public class SnakeJarFactory {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJar.class);
  private static final Map<String, SnakeJar> instances = new HashMap<>();

  /*static {
    synchronized(ClassLoader.getSystemClassLoader()) {
      LOG.trace("Loading SnakeJarFactory instance with [{}]", Thread.currentThread().getContextClassLoader());
      Properties sysProps = System.getProperties();
      SnakeJarFactory singleton = (SnakeJarFactory) sysProps.get(SnakeJarFactory.class.getName());

      if (singleton != null) {
        INSTANCE = singleton;
        LOG.debug("Loaded SnakeJarFactory instance from properties with [{}]",
          Thread.currentThread().getContextClassLoader());
      } else {
        INSTANCE = new SnakeJarFactory();
        System.getProperties().put(SnakeJarFactory.class.getName(), INSTANCE);
        LOG.debug("Created SnakeJarFactory instance with [{}] from [{}]",
          INSTANCE.getClass().getClassLoader(),
          Thread.currentThread().getContextClassLoader());
      }
    }
  }*/

  public static SnakeJar get(String className) {
    return instances.computeIfAbsent(className, n -> {
      synchronized(ClassLoader.getSystemClassLoader()) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        LOG.trace("Loading SnakeJarFactory instance with [{}]...", cl);
        Properties sysProps = System.getProperties();
        SnakeJar singleton = (SnakeJar) sysProps.get(className);
        if (singleton != null) {
          LOG.debug("Loaded SnakeJar instance from properties with [{}].", cl);
          return singleton;
        } else {
          try {
            Class<?> cls = cl.loadClass(className);
            Constructor<?> ctor = cls.getDeclaredConstructor();
            singleton = (SnakeJar) ctor.newInstance();
            System.getProperties().put(className, singleton);
            LOG.debug("Created SnakeJarFactory instance with [{}] from [{}]", singleton.getClass().getClassLoader(), cl);
            return singleton;
          } catch (Exception e) {
            throw new RuntimeException("Unable to create [" + className + "] instance!", e);
          }
        }
      }
    });
  }
}
