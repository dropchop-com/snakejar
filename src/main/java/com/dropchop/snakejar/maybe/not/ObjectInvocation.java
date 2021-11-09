package com.dropchop.snakejar.maybe.not;

import com.dropchop.snakejar.ClassInvocation;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 18. 10. 21.
 */
public interface ObjectInvocation<V> extends ClassInvocation<V> {

  String getPyObjectName();
}
