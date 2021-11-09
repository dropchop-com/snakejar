package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.*;
import com.dropchop.snakejar.maybe.not.ObjectInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public class Invoker implements com.dropchop.snakejar.Invoker {

  private static final Logger LOG = LoggerFactory.getLogger(Invoker.class);

  private final ExecutorService service;
  private final InterpreterFactory interpreterFactory;

  public Invoker(InterpreterFactory interpreterFactory, ExecutorService service) {
    this.interpreterFactory = interpreterFactory;
    this.service = service;
  }

  protected InterpreterFactory getInterpreterFactory() {
    return interpreterFactory;
  }

  protected ExecutorService getService() {
    return service;
  }

  @Override
  public <X, V> Future<V> apply(InvocationSupplier<V> invocationSupplier, Arg<X> argSupplier, KwArg kwSupplier) {
    return this.getService().submit(
      () -> {
        InterpreterFactory interpreterFactory = getInterpreterFactory();
        LOG.trace("Get interpreter from [{}]...", interpreterFactory);
        com.dropchop.snakejar.Interpreter interpreter = interpreterFactory.getInterpreter();
        LOG.trace("Got interpreter from [{}].", interpreterFactory);
        Invocation<V> invocation = invocationSupplier.get();
        X args = argSupplier.get();
        Object[] argsObjects;
        if (args != null) {
          if (!args.getClass().isArray()) {
            argsObjects = new Object[]{args};
          } else {
            argsObjects = (Object[])args;
          }
        } else {
          argsObjects = new Object[]{null};
        }

        V result;
        try {
          if (invocation instanceof ObjectInvocation) {
            throw new UnsupportedOperationException("This is not yet implemented. And it might never be! Dont use ObjectInvocation class!");
            /*result = interpreter.invoke(
              invocation.getPyModule(),
              ((ObjectInvocation<V>) invocation).getPyClass(),
              invocation.getPyFunction(),
              ((ObjectInvocation<V>) invocation).getPyObjectName(),
              invocation.getResultClass(),
              kwSuplier != null ? kwSuplier.get() : new HashMap<>(),
              argsObjects);*/
          } else if (invocation instanceof ClassInvocation) {
            result = interpreter.invoke(
              invocation.getPyModule(),
              ((ClassInvocation<V>) invocation).getPyClass(),
              invocation.getPyFunction(),
              invocation.getResultClass(),
              kwSupplier != null ? kwSupplier.get() : null,
              argsObjects);
          } else {
            result = interpreter.invoke(
              invocation.getPyModule(),
              invocation.getPyFunction(),
              invocation.getResultClass(),
              kwSupplier != null ? kwSupplier.get() : null,
              argsObjects);
          }

          return result;
        } catch (Exception e) {
          throw new Exception("Unable to invoke interpreter!", e);
        }
      }
    );
  }

  public <X, V> Future<V> apply(Invocation<V> invocation, Arg<X> argSupplier, KwArg kwSupplier) {
    return apply(() -> invocation, argSupplier, kwSupplier);
  }

  @Override
  public <X, V> Future<V> apply(Invocation<V> invocation, Arg<X> argSuplier) {
    return this.apply(invocation, argSuplier, null);
  }

  @Override
  public <X, V> Future<V> apply(InvocationSupplier<V> invocationSupplier, Arg<X> argSupplier){
    return apply(invocationSupplier, argSupplier, null);
  }
}
