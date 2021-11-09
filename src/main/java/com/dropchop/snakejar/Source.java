package com.dropchop.snakejar;

import java.util.function.Supplier;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 14. 10. 21.
 */
public interface Source<S> extends Supplier<S> {
  String getModuleName();
}
