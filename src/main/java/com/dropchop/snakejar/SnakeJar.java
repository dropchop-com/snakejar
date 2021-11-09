package com.dropchop.snakejar;

import com.dropchop.snakejar.Invoker.Params;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 29. 10. 21.
 */
public interface SnakeJar {

  enum State {
    UNLOADED(10),
    LOADED(20),
    READY(30),
    DESTROYED(40);

    private final int state;

    State(int state) {
      this.state = state;
    }

    public boolean isLower(State other) {
      return this.state < other.state;
    }

    public boolean atLeast(State other) {
      return this.state >= other.state;
    }
  }

  SnakeJar load();
  SnakeJar initialize();
  SnakeJar destroy();
  SnakeJar unload();

  Invoker prep(String poolName, Params params, List<Source<?>> sources) throws ExecutionException, InterruptedException;

  Invoker prep(String poolName, List<Source<?>> sources) throws ExecutionException, InterruptedException;

  Invoker prep(String poolName, Params params, Source<?> source) throws ExecutionException, InterruptedException;

  Invoker prep(String poolName, Source<?> source) throws ExecutionException, InterruptedException;

  Invoker prep(Params params, List<Source<?>> sources) throws ExecutionException, InterruptedException;

  Invoker prep(Params params, Source<?> source) throws ExecutionException, InterruptedException;

  Invoker prep(List<Source<?>> sources) throws ExecutionException, InterruptedException;

  Invoker prep(Source<?> source) throws ExecutionException, InterruptedException;
}
