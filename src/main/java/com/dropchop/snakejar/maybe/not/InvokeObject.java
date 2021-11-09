package com.dropchop.snakejar.maybe.not;

import com.dropchop.snakejar.InvokeClass;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 18. 10. 21.
 */
public abstract class InvokeObject<V> extends InvokeClass<V> implements ObjectInvocation<V> {

  private final String pyObject;

  public InvokeObject(String pyModule, String pyFunction, String pyClass, String pyObject, Class<V> resultClass) {
    super(pyModule, pyFunction, pyClass, resultClass);
    this.pyObject = pyObject;
  }

  @Override
  public String getPyObjectName() {
    return pyObject;
  }
}
