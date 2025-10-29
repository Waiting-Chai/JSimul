package com.jsimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.jsimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Resource functionality.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class ResourceTest {
  @Test
  void requestRelease() {
    Environment env = new Environment();
    Resource r = new Resource(env, 1);
    Request req = r.request();
    env.step();
    assertEquals(1, r.count());
    Release rel = r.release(req);
    env.step();
    assertEquals(0, r.count());
  }
}