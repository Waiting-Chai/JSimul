package com.jsimul.examples;

import com.jsimul.core.AllOf;
import com.jsimul.core.AnyOf;
import com.jsimul.core.Environment;
import com.jsimul.core.Event;
import com.jsimul.core.Timeout;

import java.util.logging.Logger;

/**
 * Demonstrates Condition composition (anyOf / allOf) with logging.
 */
public final class ConditionExample {

    private static final Logger LOG = Logger.getLogger(ConditionExample.class.getName());

    public static void main(String[] args) {
        Environment env = new Environment();

        Event slow = new Timeout(env, 5.0, "slow").asEvent();
        Event fast = new Timeout(env, 2.0, "fast").asEvent();

        AnyOf any = new AnyOf(env, slow, fast);
        AllOf all = new AllOf(env, slow, fast);

        any.addCallback(ev -> LOG.info("AnyOf triggered with: " + ev.value()));
        all.addCallback(ev -> LOG.info("AllOf triggered with: " + ev.value()));

        LOG.info("Running until AllOf completes...");
        env.run(all.asEvent());
        LOG.info("Simulation finished at t=" + env.now());
    }
}
