package com.dropchop.snakejar;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 18. 10. 21.
 */
public interface Invocation<V> {

  String getPyModule();
  String getPyFunction();
  Class<V> getResultClass();
}
