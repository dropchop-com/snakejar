package com.dropchop.snakejar;

import com.dropchop.snakejar.impl.SnakeJarFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 21. 08. 22.
 */
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SnakeJarIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(SnakeJarTest.class);

  public static final List<Source<?>> SOURCES = List.of(
    new ModuleSource<>("fasttext-lid", () -> Paths.get("src", "main", "python", "fasttext-lid.py")),
    new ModuleSource<>("cclassla", () -> Paths.get("src", "main", "python", "cclassla.py")),
    new ModuleSource<>("sstanza", () -> Paths.get("src", "main", "python", "sstanza.py"))
  );

  public static class ModelReloadFunction extends InvokeFunction<Void> {
    public ModelReloadFunction(String pyModule) {
      super(pyModule, "reload_model", Void.class);
    }
  }

  @Test
  @Order(5)
  void run() throws Exception {
    SnakeJar snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    assertNotNull(snakeJar);
    snakeJar.load();
    snakeJar.initialize();

    Invoker invoker = snakeJar.prep(
      SOURCES
    );

    long time = System.currentTimeMillis();
    /*invoker.apply(
      new ModelReloadFunction("fasttext-lid"),
      () -> new Object[]{"wiki-compressed", "models/fasttext-lid/lid.176.ftz.wiki.fasttext"}
    ).get();
    invoker.apply(
      new ModelReloadFunction("cclassla"),
      () -> new Object[]{"sl-standard", "models/classla"}
    ).get();*/
    invoker.apply(
      new ModelReloadFunction("sstanza"),
      () -> new Object[]{"sl-ssj", "models/stanza"}
    ).get();
    LOG.info("Done in [{}]ms",
      System.currentTimeMillis() - time);
  }
}
