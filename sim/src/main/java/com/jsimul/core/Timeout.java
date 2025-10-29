package com.jsimul.core;

/**
 * A timeout event that is triggered after a delay.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class Timeout extends Event {
  private final double delay;

  public Timeout(Environment env, double delay, Object value) {
    super(env);
    if (delay < 0) throw new IllegalArgumentException("Negative delay " + delay);
    this.delay = delay;
    this.ok = true;
    this.value = value;
    env.schedule(this, NORMAL, delay);
  }

  public Timeout(Environment env, double delay) {
    this(env, delay, null);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + delay + (value == null ? ")" : ", value=" + value + ")");
  }
}