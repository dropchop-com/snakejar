package com.dropchop.snakejar;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 4. 11. 21.
 */
public interface ClassInvocation<V> extends Invocation<V> {

  String getPyClass();
}
