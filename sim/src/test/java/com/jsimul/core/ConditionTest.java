package com.jsimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for Condition functionality.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class ConditionTest {
  @Test
  void allOfTriggersWhenAllProcessed() {
    Environment env = new Environment();
    Timeout a = new Timeout(env, 2, "A");
    Timeout b = new Timeout(env, 1, "B");
    Event all = new AllOf(env, a, b);
    env.run(all);
    assertTrue(all.ok());
    ConditionValue cv = (ConditionValue) all.value();
    assertEquals(2, cv.toMap().size());
    assertTrue(cv.contains(a));
    assertTrue(cv.contains(b));
  }

  @Test
  void anyOfTriggersWhenAnyProcessed() {
    Environment env = new Environment();
    Timeout a = new Timeout(env, 5, "A");
    Timeout b = new Timeout(env, 1, "B");
    Event any = new AnyOf(env, a, b);
    env.run(any);
    assertTrue(any.ok());
    ConditionValue cv = (ConditionValue) any.value();
    assertEquals(1, cv.toMap().size());
    assertTrue(cv.contains(b));
  }
}