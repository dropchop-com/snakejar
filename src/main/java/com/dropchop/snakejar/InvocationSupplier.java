package com.dropchop.snakejar;

import java.util.function.Supplier;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 9. 11. 21.
 */
public interface InvocationSupplier<V> extends Supplier<Invocation<V>> {
}
