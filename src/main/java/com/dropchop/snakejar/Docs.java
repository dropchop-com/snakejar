package com.dropchop.snakejar;

import com.dropchop.snakejar.impl.SnakeJarFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 10. 11. 21.
 */
class Docs {

  private static void minimal() throws Exception {
    // Prepare (usually we would do this once)
    SnakeJar snakeJar = SnakeJarFactory
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
      .load()
      .initialize();

    Invoker invoker = snakeJar.prep(
      new ModuleSource<>("my_add_module",
        () -> Paths.get("path", "to", "actual", "add.py")
      )
    );

    // Execute phase (many times)
    for (int i = 0; i < 100; i++) {
      Integer result = invoker.apply(
        new InvokeFunction<>("my_add_module", "add", Integer.class) {
        },
        () -> new Object[]{15, 27}
      ).get();
    }

    // Cleanup (usually we would also do this once)
    snakeJar.destroy();
    snakeJar.unload();
  }


  private static void load() throws Exception {
    // First we get desired type of SnakeJar
    SnakeJar snakeJar = SnakeJarFactory
      // Future implementations might support "sub-process" also,
      // but for now we use the only existing "SnakeJarEmbedded"
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    // We load the JNI library
    // In native code we try to find and load Python's library
    // based on invoking Python executable.
    snakeJar.load();
    // Initialize Python interpreter and SnakeJar internal state with optional thread pool parameters.
    // Again we detect Python's path in JNI lib by invoking actual python,
    // so any Python environment settings should work.
    snakeJar.initialize();
  }

  private static void prepare() throws Exception {
    SnakeJar snakeJar = SnakeJarFactory
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
      .load()
      .initialize(
        // Thread pool name, num core threads and max threads in thread pool.
        // (More than one in embedded CPython will produce exception)
        new Invoker.Params("my_thread_pool", 1, 1)
      );

    // We prepare the code to load in to Python interpreter.
    // Currently the interpreter is invoked from single thread,
    // but if we would implement sub-process invocation we
    // would need more threads, hence we can define the thread-pool
    // for the interpreter.

    // We can define as many invokers as we wish - binding them to
    // any thread-pool:
    Invoker myFuncInvoker = snakeJar.prep(
      // Thread pool name.
      "my_thread_pool",
      // We can define an ordered list of sources to compile
      List.of(
        new ModuleSource<>(
          // Custom Python module name - any unique is fine, but if its
          // imported from other modules (in Python code), then be sure
          // to give it correct name.
          "func_lang_detect_model",
          // the path (here we use relative path to current dir)
          () -> Paths.get("src", "python", "func_lang_detect_model.py")
        ),
        new ModuleSource<>(
          // Second module imports "func_lang_detect_model" module
          "func_lang_detect",
          // Here we load it from classpath
          // Beware that we load such scripts with setting its
          // actual path in Python to current working directory.
          // This is important if you use __file__ builtins.
          () -> "classpath://func_lang_detect.py"
        )
      )
    );
  }

  private static void execute() throws Exception {
    SnakeJar snakeJar = SnakeJarFactory
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
      .load().initialize();
    Invoker myFuncInvoker = snakeJar.prep(
      List.of(
        new ModuleSource<>("func_lang_detect_model",
          () -> Paths.get("src", "python", "func_lang_detect_model.py")
        ),
        new ModuleSource<>("func_lang_detect",
          () -> "classpath://func_lang_detect.py"
        )
      )
    );

    @SuppressWarnings("unchecked")
    Future<HashMap<String, Double>> future = myFuncInvoker
      .apply(
        // Type of invocation is function invocation.
        // We will invoke Python function named "lang_id" which returns a
        // Python dict[str, float], so we expect to get back
        // HashMap<String, Double> in Java.
        // Function was declared in "func_lang_detect" Python module
        // when we compiled it.
        new InvokeFunction<>("func_lang_detect", "lang_id",
          (Class<HashMap<String, Double>>)(Class<?>)HashMap.class) {},
        // We will invoke it with two positional arguments (string, int)
        () -> new Object[]{
          "Blah Blah.",
          3
        }
      );
    // Actualy invoke the function:
    HashMap<String, Double> result = future.get();
  }


  private static void destroy() throws Exception {
    SnakeJar snakeJar = SnakeJarFactory
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
      .load().initialize();
    // We stop all thread pools and wait for termination.
    // We wait for all threads in all thread pools to terminate
    // We release the Python interpreter.
    // I some situations you can call initialize() and destroy()
    // multiple times just fine.
    // This depends on which Python extensions you are using
    // and how are their "process global" variables managed.
    snakeJar.destroy();
    // Currently Noop - since there is no way of manually controlling
    // the library unloading:
    snakeJar.unload();
  }


  static void elaborate() throws Exception {

    class LangIdFunction extends InvokeFunction<HashMap<String, Double>> {
      @SuppressWarnings("unchecked")
      public LangIdFunction() {
        super("func_lang_detect", "lang_id",
          (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
      }
    }

    class LangIdClassFunction extends InvokeClass<HashMap<String, Double>> {
      @SuppressWarnings("unchecked")
      public LangIdClassFunction() {
        // Static function "lang_id" in "LanguageDetect" class
        super("class_lang_detect", "lang_id", "LanguageDetect",
          (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
      }
    }

    SnakeJar snakeJar = SnakeJarFactory
      .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
      .load().initialize();

    Invoker myFuncInvoker = snakeJar.prep(
      List.of(
        new ModuleSource<>("func_lang_detect_model",
          () -> Paths.get("src", "python", "func_lang_detect_model.py")
        ),
        new ModuleSource<>("func_lang_detect",
          () -> Paths.get("src", "python", "func_lang_detect.py")
        )
      )
    );

    Invoker myClassInvoker = snakeJar.prep(
      List.of(
        new ModuleSource<>("class_lang_detect_model",
          () -> Paths.get("src", "python", "class_lang_detect_model.py")
        ),
        new ModuleSource<>("class_lang_detect",
          () -> Paths.get("src", "python", "class_lang_detect.py")
        )
      )
    );

    HashMap<String, Double> funcInvokeResult = myFuncInvoker
      .apply(
        new LangIdFunction(),
        () -> new Object[]{
          "Blah Blah.",
          3
        }
      ).get();

    HashMap<String, Double> classInvokeResult = myClassInvoker
      .apply(
        new LangIdClassFunction(),
        () -> new Object[]{
          "Blah Blah.",
          3
        }
      ).get();

    snakeJar.destroy();
    snakeJar.unload();
  }
}
