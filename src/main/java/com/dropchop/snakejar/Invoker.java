package com.dropchop.snakejar;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 15. 10. 21.
 */
public interface Invoker {

  class Params {
    final int numCoreThreads;
    final int maxThreads;
    final long keepAliveTimeout;
    final TimeUnit unit;

    public Params(int numCoreThreads, int maxThreads, long keepAliveTimeout, TimeUnit unit) {
      this.numCoreThreads = numCoreThreads;
      this.maxThreads = maxThreads;
      this.keepAliveTimeout = keepAliveTimeout;
      this.unit = unit;
    }

    public Params(int numCoreThreads, int maxThreads) {
      this(numCoreThreads, maxThreads, 300L, TimeUnit.SECONDS);
    }

    public Params() {
      this(5, 10, 300L, TimeUnit.SECONDS);
    }

    public int getNumCoreThreads() {
      return numCoreThreads;
    }

    public int getMaxThreads() {
      return maxThreads;
    }

    public long getKeepAliveTimeout() {
      return keepAliveTimeout;
    }

    public TimeUnit getUnit() {
      return unit;
    }
  }

  <X, V> Future<V> apply(Invocation<V> invocation, Arg<X> argSupplier);
  <X, V> Future<V> apply(Invocation<V> invocation, Arg<X> argSupplier, KwArg kwSupplier);

  <X, V> Future<V> apply(InvocationSupplier<V> invocationSupplier, Arg<X> argSupplier);
  <X, V> Future<V> apply(InvocationSupplier<V> invocationSupplier, Arg<X> argSupplier, KwArg kwSupplier);
}
