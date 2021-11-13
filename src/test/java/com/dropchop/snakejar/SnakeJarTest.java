package com.dropchop.snakejar;

import com.dropchop.snakejar.Invoker.Params;
import com.dropchop.snakejar.impl.SnakeJarEmbedded;
import com.dropchop.snakejar.impl.SnakeJarFactory;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 31. 10. 21.
 */
@TestMethodOrder(OrderAnnotation.class)
class SnakeJarTest {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarTest.class);

  private SnakeJar snakeJar;

  @Test
  @Order(1)
  void load() {
    LOG.info("Lib path set at [{}]", System.getProperty("java.library.path"));
    snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.load();
  }

  @Test
  @Order(2)
  void initialize() {
    snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.initialize();
  }

  @Test
  @Order(3)
  void destroy() {
    snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.destroy();
  }

  @Test
  @Order(4)
  void unload() {
    snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.unload();
  }

  @Test
  @Order(5)
  void run() throws Exception {
    snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.load();
    snakeJar.initialize();

    int numThreads = 1;
    int numCalls = 5;

    List<Source<?>> list = new ArrayList<>();
    list.addAll(Example.FUNC_SOURCES);
    list.addAll(Example.CLASS_SOURCES);
    Invoker invoker = snakeJar.prep(
      list
    );

    List<Future<HashMap<String, Double>>> futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(
        invoker.apply(
          Example.LANG_ID_FUNC,
          () -> new Object[]{"Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.", 3}
        )
      );
    }

    long time = System.currentTimeMillis();
    for (Future<HashMap<String, Double>> future : futures) {
      try {
        System.out.println(future.get());
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      }
    }
    LOG.info("Done [{}] calls with [{}] max threads in [{}]ms", numCalls, numThreads, System.currentTimeMillis() - time);

    futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(
        invoker.apply(
          Example.LANG_ID_CLASS,
          () -> new Object[]{"Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.", 3}
        )
      );
    }

    time = System.currentTimeMillis();
    for (Future<HashMap<String, Double>> future : futures) {
      try {
        HashMap<String, Double> result = future.get();
        System.out.println(result);
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      }
    }

    LOG.info("Done [{}] calls with [{}] max threads in [{}]ms", numCalls, numThreads, System.currentTimeMillis() - time);
    snakeJar.destroy();
    snakeJar.unload();
  }
}