package com.dropchop.snakejar.impl;

import com.dropchop.snakejar.Interpreter;
import com.dropchop.snakejar.Invoker;

/**
 * @author Nikola Ivačič <nikola.ivacic@dropchop.org> on 29. 10. 21.
 */
public class SnakeJarSubProcess extends SnakeJarBase {

  SnakeJarSubProcess() {
  }

  @Override
  public Interpreter getInterpreter() {
    return null;
  }


  @Override
  protected boolean supportsMultithreading() {
    return true;
  }

  @Override
  public void destroyInterpreter(Interpreter interpreter) {

  }

  @Override
  protected String getDefaultThreadPoolName() {
    return null;
  }

  @Override
  protected Invoker.Params getDefaultInvokerParams() {
    return null;
  }

  @Override
  protected void _load() {

  }

  @Override
  protected void _initialize() {

  }

  @Override
  protected void _destroy() {

  }

  @Override
  protected void _unload() {

  }
}
