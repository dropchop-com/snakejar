package com.dropchop.snakejar;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 18. 10. 21.
 */
public abstract class InvokeFunction<V> implements Invocation<V> {

  private final String   pyModule;
  private final String   pyFunction;
  private final Class<V> resultClass;

  public InvokeFunction(String pyModule, String pyFunction, Class<V> resultClass) {
    this.pyModule = pyModule;
    this.pyFunction = pyFunction;
    this.resultClass = resultClass;
  }

  @Override
  public String getPyModule() {
    return pyModule;
  }

  @Override
  public String getPyFunction() {
    return pyFunction;
  }

  @Override
  public Class<V> getResultClass() {
    return resultClass;
  }
}
