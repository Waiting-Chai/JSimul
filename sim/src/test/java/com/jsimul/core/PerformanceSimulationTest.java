package com.jsimul.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Performance-oriented tests to guard against major throughput regressions
 * while staying deterministic.
 */
public class PerformanceSimulationTest {

    @Test
    void timeoutBurstCompletesWithinBudget() {
        Environment env = new Environment();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (int i = 0; i < 20000; i++) {
                env.timeout(1);
            }

            env.run();

            assertEquals(1.0, env.now());
            assertEquals(0, env.scheduledCount());
        });
    }

    @Test
    void denseCallbackChainExecutesQuickly() {
        Environment env = new Environment();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Event root = env.event();
            AtomicInteger counter = new AtomicInteger();

            for (int i = 0; i < 5000; i++) {
                root.addCallback(evt -> counter.incrementAndGet());
            }

            root.succeed("ok");
            env.run();

            assertEquals(5000, counter.get());
            assertEquals("ok", root.value());
        });
    }
}

