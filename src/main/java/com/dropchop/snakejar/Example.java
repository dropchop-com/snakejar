package com.dropchop.snakejar;

import com.dropchop.snakejar.impl.SnakeJarFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 2. 11. 21.
 */
public class Example {

  private static final Logger LOG = LoggerFactory.getLogger(Example.class);

  public static final List<Source<?>> LANG_ID_FUNC_SOURCES = List.of(
    new ModuleSource<>("func_lang_detect_model", () -> Paths.get("src", "main", "python", "func_lang_detect_model.py")),
    new ModuleSource<>("func_lang_detect", () -> Paths.get("src", "main", "python", "func_lang_detect.py"))
  );

  public static class InvokeLangIdFunc extends InvokeFunction<HashMap<String, Double>> {
    @SuppressWarnings("unchecked")
    public InvokeLangIdFunc() {
      super("func_lang_detect", "lang_id", (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
    }
  }

  public static final Invocation<HashMap<String, Double>> LANG_ID_FUNC = new InvokeLangIdFunc();


  public static final List<Source<?>> LANG_ID_CLASS_SOURCES = List.of(
    new ModuleSource<>("class_lang_detect_model", () -> Paths.get("src", "main", "python", "class_lang_detect_model.py")),
    new ModuleSource<>("class_lang_detect", () -> Paths.get("src", "main", "python", "class_lang_detect.py"))
  );

  public static class InvokeLangIdClass extends InvokeClass<HashMap<String, Double>> {
    @SuppressWarnings("unchecked")
    public InvokeLangIdClass() {
      super("class_lang_detect", "lang_id", "LanguageDetect", (Class<HashMap<String, Double>>)(Class<?>)HashMap.class);
    }
  }

  public static final Invocation<HashMap<String, Double>> LANG_ID_CLASS = new InvokeLangIdClass();


  public static final List<Source<?>> PARAMS_FUNC_SOURCES = List.of(
    new ModuleSource<>("func_test_params", () -> Paths.get("src", "main", "python", "func_test_params.py"))
  );

  public static class InvokeHashMapFunc extends InvokeFunction<List<String>> {
    @SuppressWarnings("unchecked")
    public InvokeHashMapFunc() {
      super("func_test_params", "params_hashmap", (Class<List<String>>)(Class<?>)ArrayList.class);
    }
  }

  public static final Invocation<List<String>> PARAMS_HASHMAP_FUNC = new InvokeHashMapFunc();

  public static class InvokeListFunc extends InvokeFunction<List<String>> {
    @SuppressWarnings("unchecked")
    public InvokeListFunc() {
      super("func_test_params", "params_list", (Class<List<String>>)(Class<?>)ArrayList.class);
    }
  }

  public static final Invocation<List<String>> PARAMS_LIST_FUNC = new InvokeListFunc();


  public static class InvokeListHashMapFunc extends InvokeFunction<List<Map<String, Object>>> {
    @SuppressWarnings("unchecked")
    public InvokeListHashMapFunc() {
      super("func_test_params", "params_list_hashmap", (Class<List<Map<String, Object>>>)(Class<?>)ArrayList.class);
    }
  }

  public static final Invocation<List<Map<String, Object>>> PARAMS_LIST_HASHMAP_FUNC = new InvokeListHashMapFunc();

  public static class InvokeListHashMapFuncNoCopy extends InvokeFunction<List<Map<String, Object>>> {
    @SuppressWarnings("unchecked")
    public InvokeListHashMapFuncNoCopy() {
      super("func_test_params", "params_list_hashmap_nocopy", (Class<List<Map<String, Object>>>)(Class<?>)ArrayList.class);
    }
  }

  public static final Invocation<List<Map<String, Object>>> PARAMS_LIST_HASHMAP_FUNC_NO_COPY = new InvokeListHashMapFunc();


  public static void exec(List<Future<HashMap<String, Double>>> futures, int numCalls) {
    long time = System.currentTimeMillis();
    for (Future<HashMap<String, Double>> future : futures) {
      try {
        System.out.println(future.get() + " ");
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      } catch (InterruptedException ex) {
        LOG.error("Interrupted!", ex);
      }
    }
    LOG.info("Done [{}] calls in [{}]ms", numCalls, System.currentTimeMillis() - time);
  }

  public static void execSimpleParams(Invoker paramsInvoker, Invocation<List<String>> invocation, Object params, int numCalls) {
    long time = System.currentTimeMillis();
    Collection<Future<List<String>>> futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(
        paramsInvoker.apply(invocation, () ->
          new Object[]{"sl", params}
        ));
    }
    for (Future<List<String>> future : futures) {
      try {
        System.out.println(future.get() + " ");
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      } catch (InterruptedException ex) {
        LOG.error("Interrupted!", ex);
      }
    }
    LOG.info("Done [{}] calls in [{}]ms", numCalls, System.currentTimeMillis() - time);
  }

  public static void execParamsHashMap(Invoker paramsInvoker, int numCalls) {
    Map<String, Object> map = Map.of("title", "test title2");
    execSimpleParams(paramsInvoker, PARAMS_HASHMAP_FUNC, map, numCalls);
  }

  public static void execParamsList(Invoker paramsInvoker, int numCalls) {
    List<String> strings = List.of("test title1", "test title2");
    execSimpleParams(paramsInvoker, PARAMS_LIST_FUNC, strings, numCalls);
  }

  public static void execParams(Invoker paramsInvoker, Invocation<List<Map<String, Object>>> invocation, Object params, int numCalls) {
    long time = System.currentTimeMillis();
    Collection<Future<List<Map<String, Object>>>> futures = new ArrayList<>();
    for (int i = 0; i < numCalls; i++) {
      futures.add(
        paramsInvoker.apply(invocation, () ->
          new Object[]{"sl", params}
        ));
    }
    for (Future<List<Map<String, Object>>> future : futures) {
      try {
        System.out.println(future.get() + " ");
      } catch (ExecutionException ex) {
        LOG.error("Unable to execute!", ex);
      } catch (InterruptedException ex) {
        LOG.error("Interrupted!", ex);
      }
    }
    LOG.info("Done [{}] calls in [{}]ms", numCalls, System.currentTimeMillis() - time);
  }

  public static void execParamsListHashMap(Invoker paramsInvoker, int numCalls) {
    List<Map<String, Object>> docs = List.of(
      Map.of("title", "test title1", "body", "test body1", "embed", List.of(1.2, 1.3, 1.4, 1.5, 1.6)),
      Map.of("title", "test title2", "body", "test body2", "embed", List.of(1.2, 1.3, 1.4, 1.5, 1.6))
    );
    execParams(paramsInvoker, PARAMS_LIST_HASHMAP_FUNC, docs, numCalls);
  }

  public static void execParamsListHashMapNoCopy(Invoker paramsInvoker, int numCalls) {
    List<Map<String, Object>> docs = List.of(
      Map.of("title", "test title1", "body", "test body1", "embed", List.of(1.2, 1.3, 1.4, 1.5, 1.6)),
      Map.of("title", "test title2", "body", "test body2", "embed", List.of(1.2, 1.3, 1.4, 1.5, 1.6))
    );
    execParams(paramsInvoker, PARAMS_LIST_HASHMAP_FUNC_NO_COPY, docs, numCalls);
  }

  public static void main(String[] args) throws Exception {
    SnakeJar snakeJar = SnakeJarFactory.get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
    snakeJar.load();
    snakeJar.initialize(
      new Invoker.Params("test_thread_pool", 1, 1)
    );

    int numCalls = 20000;

    Invoker funcInvoker = snakeJar.prep(
      "test_thread_pool", LANG_ID_FUNC_SOURCES
    );
    Invoker paramsInvoker = snakeJar.prep(PARAMS_FUNC_SOURCES);
    Invoker classInvoker = snakeJar.prep(LANG_ID_CLASS_SOURCES);

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

    execParamsHashMap(paramsInvoker, numCalls);
    execParamsList(paramsInvoker, numCalls);
    execParamsListHashMap(paramsInvoker, numCalls);
    execParamsListHashMapNoCopy(paramsInvoker, numCalls);

    snakeJar.destroy();
    snakeJar.unload();
  }
}
