package com.dropchop.snakejar;

import java.util.function.Supplier;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 18. 10. 21.
 */
public class ModuleSource<S> implements Source<S> {

  private final String moduleName;
  private final Supplier<S> source;

  public ModuleSource(String moduleName, Supplier<S> source) {
    this.moduleName = moduleName;
    this.source = source;
  }

  @Override
  public String getModuleName() {
    return moduleName;
  }

  @Override
  public S get() {
    return source.get();
  }
}
