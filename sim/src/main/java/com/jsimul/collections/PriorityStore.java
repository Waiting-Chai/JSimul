package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.PriorityQueue;

/**
 * Store that maintains items in priority order (min-heap).
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class PriorityStore extends Store {
  private final PriorityQueue<Object> heap = new PriorityQueue<>();

  public PriorityStore(Environment env, int capacity) { super(env, capacity); }

  @Override
  protected boolean doPut(Event event) {
    StorePut put = (StorePut) event;
    if (heap.size() < capacity) {
      heap.add(put.item);
      put.succeed(null);
    }
    return true;
  }

  @Override
  protected boolean doGet(Event event) {
    StoreGet get = (StoreGet) event;
    if (!heap.isEmpty()) {
      Object v = heap.poll();
      get.succeed(v);
    }
    return true;
  }
}