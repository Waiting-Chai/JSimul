package com.jsimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Verifies behaviour specific to {@link RealtimeEnvironment}.
 *
 * @author waiting
 * @date 2025/11/05
 */
public class RealtimeEnvironmentTest {

    private static class FakeClockSleeper implements RealtimeEnvironment.Clock, RealtimeEnvironment.Sleeper {
        double seconds = 0.0;
        long sleepCalls = 0;

        @Override
        public double nowSeconds() {
            return seconds;
        }

        @Override
        public void sleepNanos(long nanos) {
            sleepCalls++;
            seconds += nanos / 1_000_000_000.0;
        }
    }

    @Test
    void processRunsUsingRealtimeStep() {
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.0001, false);
        Process proc =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(0.5, "rt"));
                            return "rt";
                        });

        Object result = env.run(proc);
        assertEquals("rt", result);
        assertEquals(0.5, env.now(), 1e-9);
    }

    @Test
    void timeoutAndNumericUntilInteractProperly() {
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.0001, false);
        env.timeout(1.0);
        Object result = env.run(1.0);
        assertNull(result);
        assertEquals(1.0, env.now(), 1e-9);
    }

    @Test
    void syncResetsRealBaseline() {
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.01, false);
        env.sync();
        // If sync introduced illegal state, the run below would throw. Using a short factor avoids
        // waiting but still exercises the scheduling path after sync.
        Process proc = env.process(ctx -> ctx.await(env.timeout(0.2, null)));
        assertDoesNotThrow(() -> env.run(proc));
    }

    @Test
    void realtimeUsesSleeperInsteadOfBusySpin() {
        FakeClockSleeper controller = new FakeClockSleeper();
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 1.0, false, controller, controller);
        Process proc =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(1.0, "done"));
                            return "done";
                        });

        Object result = env.run(proc);
        assertEquals("done", result);
        assertEquals(1.0, env.now(), 1e-9);
        assertTrue(controller.sleepCalls > 0, "Sleeper should have been invoked");
    }

    @Test
    void strictModeDetectsLag() {
        FakeClockSleeper controller = new FakeClockSleeper() {
            @Override
            public void sleepNanos(long nanos) {
                super.sleepNanos(nanos);
                // Inject additional lag by advancing beyond requested sleep
                this.seconds += 0.2;
            }
        };
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.05, true, controller, controller);
        env.timeout(0.1);

        RuntimeException ex = assertThrows(RuntimeException.class, env::step);
        assertTrue(ex.getMessage().contains("too slow"));
    }

    @Test
    void longIdleFollowedByEventRespectsSync() {
        FakeClockSleeper controller = new FakeClockSleeper();
        RealtimeEnvironment env = new RealtimeEnvironment(5.0, 1.0, false, controller, controller);

        // simulate long idle then sync before running
        controller.seconds = 100.0;
        env.sync();
        controller.seconds = 100.5;

        Process proc =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(1.0, "go"));
                            return "go";
                        });

        Object result = env.run(proc);
        assertEquals("go", result);
        assertEquals(6.0, env.now(), 1e-9);
    }

    @Test
    void verySmallFactorStillSleeps() {
        FakeClockSleeper controller = new FakeClockSleeper();
        RealtimeEnvironment env = new RealtimeEnvironment(0.0, 1e-6, false, controller, controller);
        Process proc = env.process(ctx -> ctx.await(env.timeout(1.0, "tiny")));

        Object result = env.run(proc);
        assertEquals("tiny", result);
        assertTrue(controller.sleepCalls > 0);
    }
}
