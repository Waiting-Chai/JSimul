package resources;

import com.jsimul.collections.PriorityRequest;
import com.jsimul.collections.PriorityResource;
import com.jsimul.core.Environment;
import com.jsimul.core.Process;

/**
 * Demonstrates how priority alters queue ordering for a resource checkout
 * scenario.
 */
public class PriorityCheckout {

    public static void main(String[] args) {
        Environment env = new Environment();
        PriorityResource cashier = new PriorityResource(env, 1);

        env.process(customer(env, cashier, "regular", 5, 10));
        env.process(customer(env, cashier, "vip", 2, 4));
        env.process(customer(env, cashier, "walk-in", 3, 6));

        env.run();
    }

    private static Process.ProcessFunction customer(Environment env, PriorityResource cashier, String name,
                                                    int priority, double serviceTime) {
        return process -> {
            PriorityRequest request = cashier.request(priority);
            yield request;

            System.out.printf("t=%.0f -> %s got service\n", env.now(), name);
            yield env.timeout(serviceTime);

            cashier.release(request);
            System.out.printf("t=%.0f -> %s finished\n", env.now(), name);
        };
    }
}

