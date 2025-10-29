package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * FIFO store for arbitrary items with capacity.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class Store extends BaseResource {
  protected final List<Object> items = new ArrayList<>();

  public Store(Environment env, int capacity) {
    super(env, capacity);
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
  }

  public Event put(Object item) { return new StorePut(this, item); }

  public Event get() { return new StoreGet(this); }

  @Override
  protected boolean doPut(Event event) {
    StorePut put = (StorePut) event;
    if (items.size() < capacity) {
      items.add(put.item);
      put.succeed(null);
    }
    return true;
  }

  @Override
  protected boolean doGet(Event event) {
    StoreGet get = (StoreGet) event;
    if (!items.isEmpty()) {
      Object v = items.remove(0);
      get.succeed(v);
    }
    return true;
  }

  public static class StorePut extends BaseResource.Put {
    final Object item;
    public StorePut(Store store, Object item) {
      super(store);
      this.item = item;
    }
  }

  public static class StoreGet extends BaseResource.Get {
    public StoreGet(Store store) { super(store); }
  }
}