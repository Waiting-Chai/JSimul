package com.jsimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.jsimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Container functionality.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class ContainerTest {
  @Test
  void putGetAmounts() {
    Environment env = new Environment();
    Container c = new Container(env, 100, 10);
    c.put(5);
    env.step();
    assertEquals(15.0, c.level(), 1e-9);
    Container.GetEvent ge = (Container.GetEvent) c.get(8);
    env.step();
    assertEquals(8.0, ge.value());
    assertEquals(7.0, c.level(), 1e-9);
  }
}