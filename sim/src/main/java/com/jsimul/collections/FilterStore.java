package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.function.Predicate;

/**
 * Store supporting filtered get requests.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStore extends Store {
  public FilterStore(Environment env, int capacity) {
    super(env, capacity);
  }

  public Event get(Predicate<Object> filter) { return new FilterStoreGet(this, filter); }

  @Override
  protected boolean doGet(Event event) {
    if (event instanceof FilterStoreGet) {
      FilterStoreGet get = (FilterStoreGet) event;
      for (int i = 0; i < items.size(); i++) {
        Object item = items.get(i);
        if (get.filter.test(item)) {
          items.remove(i);
          get.succeed(item);
          break;
        }
      }
      return true;
    }
    return super.doGet(event);
  }

  public static class FilterStoreGet extends BaseResource.Get {
    final Predicate<Object> filter;
    public FilterStoreGet(FilterStore store, Predicate<Object> filter) {
      super(store);
      this.filter = filter;
    }
  }
}