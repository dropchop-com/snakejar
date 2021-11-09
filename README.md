SnakeJar
=========

[![][Build Status img]][Build Status]
[![][Shippable Status img]][Shippable Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

A Java library with embedded CPython which supports native image compile and usage 
in Java Application Servers, targeting Python Machine Learning frameworks and libraries.

Hence we stuffed some snakes in the jar.

### Rationale

Why yet another Java Python bridge?
Well, we tested quite a few options and sooner or latter reaching a dead end. 
Most of the issues that stopped us were:
- Classloading in Java Application Servers / Servlet and REST frameworks,
- Multi thread access support,
- Liveliness of the project implementing a bridge,
- Native image compile and run
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

### Limitations

Of course, we also introduced numerous limitations, so we only support: 
- single usage paradigm (contrary to Jep with much richer usage patterns),
- Python 3.9 (if you use compiled code),
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
(since our main use is in Linux Containers).

### Usage
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

Here is a usage snippet:

```java
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
```

### Development

You need Linux with Python 3.9 installed. 

Gradle wrapper script can be used for build, test and run.  
Wrapper script has been modified so that it first creates Python virtual environment in *venv* folder.

So you should set it up for your terminal for later on:
```
source venv/bin/activate
```
Since we also support native image build, you must set up GraalVM as your JDK.

```
export JAVA_HOME=/usr/lib/jvm/java-11-graalvm
```
If you want to use other Java w/o native image build support, comment out  
`applicationDefaultJvmArgs.add("-agentlib:native-image-agent=config-output-dir=./agent")`
in *build.gradle* file.  
(We use this settings to generate jni and reflection configuration files) 