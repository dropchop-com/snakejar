package com.dropchop.snakejar;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 4. 11. 21.
 */
public class InvokeClass<V> extends InvokeFunction<V> implements ClassInvocation<V> {

  private final String pyClass;

  public InvokeClass(String pyModule, String pyFunction, String pyClass, Class<V> resultClass) {
    super(pyModule, pyFunction, resultClass);
    this.pyClass = pyClass;
  }

  public String getPyClass() {
    return pyClass;
  }
}
