package com.jsimul.examples;

import com.jsimul.collections.Request;
import com.jsimul.collections.Resource;
import com.jsimul.core.Environment;
import com.jsimul.core.Process;
import com.jsimul.core.Timeout;

import java.util.logging.Logger;

/**
 * Demonstrates Resource request/release with a single machine and queued jobs.
 */
/**
 * Demonstrates Resource request/release with a single machine and queued jobs.
 *
 * @author waiting
 * @date 2025/12/02
 */
public final class ResourceUsageExample {

    private static final Logger LOG = Logger.getLogger(ResourceUsageExample.class.getName());

    public static void main(String[] args) {
        Environment env = new Environment();
        Resource machine = new Resource(env, 1); // single machine

        for (int i = 0; i < 3; i++) {
            env.process(job(env, machine, i));
        }

        env.run();
        LOG.info("All jobs finished at t=" + env.now());
    }

    private static Process.ProcessFunction job(Environment env, Resource machine, int id) {
        return ctx -> {
            LOG.info("job-" + id + " requesting machine @ t=" + env.now());
            Request req = machine.request();
            ctx.await(req.asEvent());

            double processTime = 3.0;
            LOG.info("job-" + id + " acquired machine, processing " + processTime + "s");
            ctx.await(new Timeout(env, processTime, null));

            machine.release(req).asEvent();
            LOG.info("job-" + id + " released machine @ t=" + env.now());
            return "done";
        };
    }
}
