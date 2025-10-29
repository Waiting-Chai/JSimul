package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for shared resources (put/get queues).
 * 
 * @author waiting
 * @date 2025/10/29
 */
public abstract class BaseResource {
  protected final Environment env;
  protected final int capacity;
  protected final List<Event> putQueue = new ArrayList<>();
  protected final List<Event> getQueue = new ArrayList<>();

  public BaseResource(Environment env, int capacity) {
    this.env = env;
    this.capacity = capacity;
  }

  public int capacity() { return capacity; }

  public Event put() { return new Put(this); }

  public Event get() { return new Get(this); }

  protected abstract boolean doPut(Event event);

  protected abstract boolean doGet(Event event);

  public void triggerPut(Event getEvent) {
    int idx = 0;
    while (idx < putQueue.size()) {
      Event e = putQueue.get(idx);
      boolean proceed = doPut(e);
      if (e.triggered()) {
        idx++;
      } else if (putQueue.remove(idx) != e) {
        throw new RuntimeException("Put queue invariant violated");
      }
      if (!proceed) break;
    }
  }

  public void triggerGet(Event putEvent) {
    int idx = 0;
    while (idx < getQueue.size()) {
      Event e = getQueue.get(idx);
      boolean proceed = doGet(e);
      if (e.triggered()) {
        idx++;
      } else if (getQueue.remove(idx) != e) {
        throw new RuntimeException("Get queue invariant violated");
      }
      if (!proceed) break;
    }
  }

  /** Put request event. */
  public static class Put extends Event {
    final BaseResource resource;
    public Put(BaseResource resource) {
      super(resource.env);
      this.resource = resource;
      resource.putQueue.add(this);
      this.addCallback(resource::triggerGet);
      resource.triggerPut(null);
    }
    public void cancel() {
      if (this.triggered()) resource.putQueue.remove(this);
    }
  }

  /** Get request event. */
  public static class Get extends Event {
    final BaseResource resource;
    public Get(BaseResource resource) {
      super(resource.env);
      this.resource = resource;
      resource.getQueue.add(this);
      this.addCallback(resource::triggerPut);
      resource.triggerGet(null);
    }
    public void cancel() {
      if (this.triggered()) resource.getQueue.remove(this);
    }
  }
}