package com.jsimul.core;

/**
 * Exception used internally to stop Environment.run at an until event.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class StopSimulation extends RuntimeException {
  private final Object value;

  public StopSimulation(Object value) {
    this.value = value;
  }

  public Object value() { return value; }

  public static void callback(Event event) {
    if (event.ok()) {
      throw new StopSimulation(event.value());
    } else {
      Throwable t = (Throwable) event.value();
      RuntimeException rt = (t instanceof RuntimeException) ? (RuntimeException) t : new RuntimeException(t);
      rt.initCause(t);
      throw rt;
    }
  }
}