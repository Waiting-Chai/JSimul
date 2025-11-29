package com.jsimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for Environment inspection helpers.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class EnvironmentInspectionTest {

    @Test
    void scheduledCountReflectsQueuedEvents() {
        Environment env = new Environment();
        env.timeout(1.0);
        env.timeout(2.0);

        assertEquals(2, env.scheduledCount());

        env.step();
        assertEquals(1, env.scheduledCount());
    }

    @Test
    void peekReturnsInfinityWhenEmpty() {
        Environment env = new Environment();
        assertEquals(Environment.Infinity, env.peek());
    }
}
