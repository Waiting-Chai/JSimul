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

  @Test
  void processRunsUsingRealtimeStep() {
    RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.0, false);
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
    RealtimeEnvironment env = new RealtimeEnvironment(0.0, 0.0, false);
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
}
