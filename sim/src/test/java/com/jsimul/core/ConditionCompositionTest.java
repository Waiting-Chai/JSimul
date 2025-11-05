package com.jsimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Covers composite event combinations (AND/OR) and nested condition value
 * aggregation to mirror SimPy's semantics.
 *
 * @author waiting
 * @date 2025/11/05
 */
public class ConditionCompositionTest {

    @Test
    void eventAndProducesConditionValue() {
        Environment env = new Environment();
        Timeout fast = env.timeout(1.0, "A");
        Timeout slow = env.timeout(2.0, "B");

        SimEvent combined = fast.and(slow);
        env.run(combined);

        ConditionValue cv = (ConditionValue) combined.asEvent().value();
        assertEquals(2, cv.toMap().size());
        assertEquals("A", cv.get(fast.asEvent()));
        assertEquals("B", cv.get(slow.asEvent()));
        assertEquals(2.0, env.now(), 1e-9);
    }

    @Test
    void eventOrCompletesOnFirstSuccess() {
        Environment env = new Environment();
        Timeout fast = env.timeout(0.5, "fast");
        Timeout slow = env.timeout(5.0, "slow");

        SimEvent either = fast.or(slow);
        env.run(either);

        ConditionValue cv = (ConditionValue) either.asEvent().value();
        assertEquals(1, cv.toMap().size());
        assertTrue(cv.contains(fast.asEvent()));
        assertEquals("fast", cv.get(fast.asEvent()));
        assertEquals(0.5, env.now(), 1e-9);
    }

    @Test
    void nestedConditionsPropagateValues() {
        Environment env = new Environment();
        Timeout a = env.timeout(1.0, "A");
        Timeout b = env.timeout(2.0, "B");
        Timeout c = env.timeout(3.0, "C");

        SimEvent nested = env.allOf(a, env.anyOf(b, c));
        env.run(nested);

        ConditionValue cv = (ConditionValue) nested.asEvent().value();
        assertEquals(2, cv.toMap().size());
        assertTrue(cv.contains(a.asEvent()));
        assertTrue(cv.contains(b.asEvent()));
        assertFalse(cv.contains(c.asEvent()));
    }

    @Test
    void mixingDifferentEnvironmentsFails() {
        Environment env1 = new Environment();
        Environment env2 = new Environment();

        Timeout event1 = env1.timeout(1.0);
        Timeout event2 = env2.timeout(1.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> env1.allOf(event1, event2),
                "Cross-environment combinations must be rejected");
    }
}
