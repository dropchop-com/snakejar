SnakeJar
=========

A Java library with embedded CPython which supports native image compile and usage 
in Java Application Servers, targeting Python Machine Learning frameworks and libraries.

We stuffed some snakes in the jar, hence the name.

## Rationale

Why yet another Java Python bridge?
Well, we tested quite a few options, sooner or latter reaching a dead end. 
Most of the issues that stopped us were:
- Classloading in Java Application Servers / Servlet and REST frameworks,
- Multi thread access support,
- Liveliness of the project implementing a bridge,
- Native image compile and run,
- Combinations of the above,
- ...

So we selected the most mature, live and stable project and reshaped it to fit our needs.  
Therefore, SnakeJar uses [Jep](https://github.com/ninia/jep) C code, but is much simpler. 
Thank you Jep [developers](https://github.com/ninia/jep/graphs/contributors)! Awesome work!

Unfortunately Jep imposes restrictions which prevents us from using 
it directly in some obscure environment combinations:
- Library loading is uncontrollable by user (integrator) - it is done in Jep constructor.
- SharedInterpreter (Jep class) can only be used from the same thread that created it.  
  So it should be created and closed on each HTTP Request? Loosing all compiled code?

We only use a subset of Jep [C code](https://github.com/ninia/jep/tree/master/src/main/c/Jep) 
namely for marshalling function arguments and unmarshalling results from Java to Python and back.

## Limitations

Of course, we also introduced numerous limitations, so we only support: 
- single usage paradigm (contrary to Jep with much richer usage patterns),
- Python 3.8, 3.9 or 3.10 (if you use compiled code),
- Linux
  
Internal Python related notes:
- We don't use SubInterpreters for various reasons,
- We use higher level PyGILState_Ensure/Release locking and therefore no thread state managment,
- We don't support callbacks to Java,
- We don't support jep.Py* Java types,
- We dont' use Python pip to build the library

Due to our lack of time (i.e. money for beer) we are currently unable to support 
other OSes and versions.

In the future, we might support additional stuff, but don't count on it
(since our main use is in Linux Containers with Quarkus).

## Usage
We support single usage pattern:
- *Prepare* (single time)
  - Load (our library, detect and load Python Library, detect Python path)
  - Initialize (Python and our classes)
  - Prepare (thread pools)
  - Compile (provided Python code)
- *Execute* (many times)
- *Cleanup* (single time)
  - Stop thread pools
  - Release and reset our internal state
  - Finalize Python
  - Unload (noop - since there is no way of manually controlling library unloading)
    
The whole cycle (*Prepare*, *Execute*, *Cleanup*) can be repeated many times, but due
to improper cleanup of some Python extensions (process global variables), 
the cycle might not work the second time.


Here are some usage snippets:

### Minimal usage snippet

Import from maven:
```xml
<dependency>
  <groupId>com.dropchop</groupId>
  <artifactId>snakejar</artifactId>
  <version>1.0.7</version>
</dependency>
```

Sample Python script *./path/to/actual/add.py*
```python
def add(a: int, b: int) -> int:
    return a + b

```

Sample Java program:
```java
// Prepare phase
SnakeJar snakeJar = SnakeJarFactory
  .get("com.dropchop.snakejar.impl.SnakeJarEmbedded")
  .load()
  .initialize();

Invoker invoker = snakeJar.prep(
  new ModuleSource<>("my_add_module",
    () -> Paths.get("path", "to", "actual", "add.py")
  )
);

// Execute phase
for (int i = 0; i < 100; i++) {
  Integer result = invoker.apply(
    new InvokeFunction<>("my_add_module", "add", Integer.class) {
    },
    () -> new Object[]{15, 27}
  ).get();
}

// Cleanup phase
snakeJar.destroy();
snakeJar.unload();
```

### Step by step guide

Prepare

1. Load

```java
// First we get desired type of SnakeJar
SnakeJar snakeJar = SnakeJarFactory
  // Future implementations might support "sub-process" also,
  // but for now we use the only existing "SnakeJarEmbedded"
  .get("com.dropchop.snakejar.impl.SnakeJarEmbedded");
// We load the JNI library
// In native code we try to find and load Python's library
// based on invoking Python executable.
snakeJar.load();
// Initialize Python interpreter and SnakeJar internal state.
// Again we detect Python's path in JNI lib by invoking actual python,
// so any Python environment settings should work.
snakeJar.initialize();
```

2. Prepare and compile
```java
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
  // Num core threads and max threads in thread pool.
  // (More than one in embedded CPython makes little sense
  // since performance might only degrade)
  new Invoker.Params(1, 1),
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
```

3. Execute
```java
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
```

4. Cleanup
```java
// We stop all thread pools and wait for termination.
// We wait for all threads in all thread pools to terminate
// We release the Python interpreter.
// I some situations you can call initialize() and destroy()
// multiple times just fine.
// This depends on which Python extensions you aer using
// and how are their "process global" variables managed.
snakeJar.destroy();
// Currently Noop - since there is no way of manually controlling
// the library unloading:
snakeJar.unload();
```
### Here is a more complete snippet:

#### Java
```java
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
```
#### Python

Script `src/python/func_lang_detect_model.py`
```python
import os
import fasttext

fasttext_model = None

def get_model():
    global fasttext_model
    if not fasttext_model:
        path = os.path.normpath(
            os.path.join(os.path.dirname(__file__), 
                         'lid.176.ftz.wiki.fasttext'))
        fasttext_model = fasttext.load_model(path)
    return fasttext_model

```

Script `src/python/func_lang_detect.py`
```python
import func_lang_detect_model

def lang_id(text: str, ret_num: int = 1):
    fasttext_model = func_lang_detect_model.get_model()
    classification, confidence = fasttext_model.predict(
        text.replace("\n", " "), k=ret_num)
    result = {}
    for idx, val in enumerate(classification):
        new_label = classification[idx]
        result[new_label] = confidence[idx]
    return result
```
And very similar class samples:

Script `src/python/class_lang_detect_model.py`
```python
import os
import fasttext

class LanguageDetectModel:

    fasttext_model = None

    @staticmethod
    def get_model():
        if not LanguageDetectModel.fasttext_model:
            path = os.path.normpath(os.path.join(
                os.path.dirname(__file__), 'lid.176.ftz.wiki.fasttext'))
            LanguageDetectModel.fasttext_model = fasttext.load_model(path)
        return LanguageDetectModel.fasttext_model

```

Script `src/python/class_lang_detect.py`
```python
from class_lang_detect_model import LanguageDetectModel

class LanguageDetect:
    
    @staticmethod
    def lang_id(text: str, num_ret: int = 1):
        fasttext_model = LanguageDetectModel.get_model()
        classification, confidence = fasttext_model.predict(
            text.replace("\n", " "), k=num_ret)
        result = {}
        for idx, val in enumerate(classification):
            new_label = classification[idx]
            result[new_label] = confidence[idx]
        return result
```

## Development

You need Linux with Python 3.10, 3.9 or 3.8 installed. 

Gradle wrapper script can be used for build, test and run.  
Wrapper script has been modified so that it first creates Python virtual environment in *venv* folder.

So you should set it up for your terminal for later on:
```
source venv/bin/activate
```
Since we also support native image build, you must set up GraalVM as your JDK.

```
export JAVA_HOME=/usr/lib/jvm/java-17-graalvm
```
If you want to use other Java w/o native image build support, comment out  
`applicationDefaultJvmArgs.add("-agentlib:native-image-agent=config-output-dir=./agent")`
in *build.gradle* file.  
(We use this settings to generate jni and reflection configuration files) 
