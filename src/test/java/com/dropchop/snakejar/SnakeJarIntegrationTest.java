package com.dropchop.snakejar;

import com.dropchop.snakejar.impl.SnakeJarFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * yay -Sy python-conda
 * conda create -n venv python=3.10
 * conda activate venv
 * git clone --recursive https://github.com/pytorch/pytorch
 * cd pytorch
 * git checkout tags/v1.11.0
 * git submodule sync
 * git submodule update --init --recursive --jobs 0
 * export CMAKE_PREFIX_PATH=${CONDA_PREFIX:-"$(dirname $(which conda))/../"}
 * conda install astunparse numpy ninja pyyaml setuptools cmake cffi typing_extensions future six requests dataclasses
 * conda install mkl mkl-include
 * pacman -Q cuda
 * conda install -c pytorch magma-cuda116
 * python setup.py install
 * #  vim third_party/breakpad/src/client/linux/handler/exception_handler.cc
 * python setup.py install
 *
 * cd /home/nikola/projects/snakejar
 * pip install -v /home/nikola/tmp/pydebug/pytorch/
 *
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 21. 08. 22.
 */
//@Disabled
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
