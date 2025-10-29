package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.Environment;

/**
 * Container for continuous or discrete matter up to capacity, supporting put/get of quantities.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class Container extends BaseResource {
  private double level;

  public Container(Environment env, double capacity, double initial) {
    super(env, (int) Math.ceil(capacity));
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    if (initial < 0 || initial > capacity) throw new IllegalArgumentException("invalid initial");
    this.level = initial;
  }

  public double level() { return level; }

  public Event put(double amount) { return new PutEvent(this, amount); }

  public Event get(double amount) { return new GetEvent(this, amount); }

  @Override
  protected boolean doPut(Event event) {
    PutEvent pe = (PutEvent) event;
    if (level + pe.amount <= capacity) {
      level += pe.amount;
      pe.succeed(null);
    }
    return true;
  }

  @Override
  protected boolean doGet(Event event) {
    GetEvent ge = (GetEvent) event;
    if (level >= ge.amount) {
      level -= ge.amount;
      ge.succeed(ge.amount);
    }
    return true;
  }

  public static class PutEvent extends BaseResource.Put {
    final double amount;
    public PutEvent(Container c, double amount) {
      super(c);
      this.amount = amount;
    }
  }

  public static class GetEvent extends BaseResource.Get {
    final double amount;
    public GetEvent(Container c, double amount) {
      super(c);
      this.amount = amount;
    }
  }
}