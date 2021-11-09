package com.dropchop.snakejar;

import com.dropchop.snakejar.impl.SnakeJarFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 2. 11. 21.
 */
public class Example {

  private static final Logger LOG = LoggerFactory.getLogger(Example.class);

  public static class InvokeLangIdFunc extends InvokeFunction<HashMap<String, Double>> {
    @SuppressWarnings("unchecked")
    public InvokeLangIdFunc() {
      super("func_lang_detect", "lang_id", (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
    }
  }

  public static class InvokeLangIdClass extends InvokeClass<HashMap<String, Double>> {
    @SuppressWarnings("unchecked")
    public InvokeLangIdClass() {
      super("class_lang_detect", "lang_id", "LanguageDetect", (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
    }
  }

  public static final Invocation<HashMap<String, Double>> LANG_ID_FUNC = new InvokeLangIdFunc();
  public static final List<Source<?>> FUNC_SOURCES = List.of(
      new ModuleSource<>("func_lang_detect_model", () -> Paths.get("src", "main", "python", "func_lang_detect_model.py")),
      new ModuleSource<>("func_lang_detect", () -> Paths.get("src", "main", "python", "func_lang_detect.py"))
    );

  public static final Invocation<HashMap<String, Double>> LANG_ID_CLASS = new InvokeLangIdClass();
  public static final List<Source<?>> CLASS_SOURCES = List.of(
    new ModuleSource<>("class_lang_detect_model", () -> Paths.get("src", "main", "python", "class_lang_detect_model.py")),
    new ModuleSource<>("class_lang_detect", () -> Paths.get("src", "main", "python", "class_lang_detect.py"))
  );

  public static void exec(List<Future<HashMap<String, Double>>> futures, int numCalls) {
    long time = System.currentTimeMillis();
    for (Future<HashMap<String, Double>> future : futures) {
      try {
        System.out.print(future.get() + " ");
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      } catch (InterruptedException ex) {
        LOG.error("Interrupted!", ex);
      }
    }
    LOG.info("Done [{}] calls in [{}]ms", numCalls, System.currentTimeMillis() - time);
  }

  public static void main(String[] args) throws Exception {
    SnakeJar snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    snakeJar.load();
    snakeJar.initialize();

    int numCalls = 10000;

    Invoker funcInvoker = snakeJar.prep(
      "test_thread_pool", new Invoker.Params(1, 1), FUNC_SOURCES
    );
    Invoker classInvoker = snakeJar.prep(CLASS_SOURCES);

    List<Future<HashMap<String, Double>>> futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(funcInvoker.apply(LANG_ID_FUNC, () ->
        new Object[]{"Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.", 3}
      ));
    }
    exec(futures, numCalls);

    futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(classInvoker.apply(LANG_ID_CLASS, () ->
        new Object[]{"Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.", 3}
      ));
    }
    exec(futures, numCalls);

    snakeJar.destroy();
    snakeJar.unload();
  }


  public static void docs() throws Exception {
    // Prepare phase
    SnakeJar snakeJar = SnakeJarFactory  // Factory is subject to change (I'm not happy with impl.)
      // Future implementations might be "sub-process" also, but for now we use "embedded"
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded"); // Late load class
    snakeJar.load(); // Load library with same ClassLoader
    snakeJar.initialize(); // Initialize Python and internal state

    Invoker myFuncInvoker = snakeJar.prep(//prep ThreadPool and compile
      "my_thread_pool", // Thread pool name.
      new Invoker.Params(1, 1), // Num core threads and max threads in thread pool
      List.of(// compile...
        new ModuleSource<>("func_lang_detect_model", // Custom Python module name - any unique is fine
          () -> Paths.get("src", "main", "python", "func_lang_detect_model.py")
        ),
        new ModuleSource<>("func_lang_detect", // This module imports "func_lang_detect_model" module
          () -> Paths.get("src", "main", "python", "func_lang_detect.py")
        )
      )
    ); //There can be more thread pools

    Invoker myDefaultInvoker = snakeJar.prep(//prep ThreadPool and compile
      List.of(//again compile...
        new ModuleSource<>("class_lang_detect_model",
          // We also support loading source direcly as string or from classpath,
          // but then __file__ builtin in Python doesn't work
          () -> Paths.get("src", "main", "python", "class_lang_detect_model.py")
        ),
        new ModuleSource<>("class_lang_detect",
          () -> Paths.get("src", "main", "python", "class_lang_detect.py")
        )
      )
    );  // Single thread. (works best due to nature of CPython implementation)
        // Here we use "default" thread pool since we omitted the ThreadPool specs

    // Execute phase
    myFuncInvoker
      .apply(
        // type of invocation is function "lang_id" which returns a HashMap
        // and is located in Python module "func_lang_detect"
        new InvokeFunction<>("func_lang_detect", "lang_id", HashMap.class) {},
        // we will invoke it with two positional arguments (string, int)
        () -> new Object[]{
          "Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.",
          3
        }
      ) //create Future<HashMap>
      .get();// invoke now

    myDefaultInvoker
      .apply(
        // type of invocation is class static method "LanguageDetect.lang_id" which returns a HashMap
        // (Python dict) and is located in Python module "class_lang_detect"
        new InvokeClass<>("class_lang_detect", "lang_id", "LanguageDetect", HashMap.class) {},
        // we will invoke it with two positional arguments (string, int)
        () -> new Object[]{
          "Kaj nam pa morejo? Ali pa pač ne dojamejo! Ali pa še en daljši članek.",
          3
        }
      ) //create Future<HashMap>
      .get(); // invoke now

    // Cleanup phase
    snakeJar.destroy();
    snakeJar.unload();
  }
}
