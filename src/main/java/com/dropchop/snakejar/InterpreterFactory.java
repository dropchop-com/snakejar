package com.dropchop.snakejar;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 31. 10. 21.
 */
public interface InterpreterFactory {
  Interpreter getInterpreter();
  void destroyInterpreter(Interpreter interpreter);
}
