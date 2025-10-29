package com.jsimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.jsimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Store functionality.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class StoreTest {
  @Test
  void fifoPutGet() {
    Environment env = new Environment();
    Store s = new Store(env, 10);
    s.put("a");
    s.put("b");
    env.step();
    env.step();
    Store.StoreGet g1 = (Store.StoreGet) s.get();
    env.step();
    assertEquals("a", g1.value());
    Store.StoreGet g2 = (Store.StoreGet) s.get();
    env.step();
    assertEquals("b", g2.value());
  }

  @Test
  void filterStoreGet() {
    Environment env = new Environment();
    FilterStore fs = new FilterStore(env, 10);
    fs.put("x");
    fs.put(42);
    env.step();
    env.step();
    FilterStore.FilterStoreGet g = (FilterStore.FilterStoreGet) fs.get(o -> o instanceof Integer);
    env.step();
    assertEquals(42, g.value());
  }
}