package conditions;

import com.jsimul.core.AllOf;
import com.jsimul.core.AnyOf;
import com.jsimul.core.Environment;
import com.jsimul.core.Process;
import com.jsimul.core.SimEvent;
import com.jsimul.core.Timeout;

/**
 * Illustrates condition composition: wait for any supplier, then for all
 * finishing tasks before shipping.
 */
public class ConditionalAssembly {

    public static void main(String[] args) {
        Environment env = new Environment();

        SimEvent shipment = env.process(shipping(env));
        AnyOf supplierArrival = env.anyOf(env.timeout(3, "supplier-A"), env.timeout(5, "supplier-B"));

        supplierArrival.addCallback(event -> {
            System.out.printf("t=%.0f -> first delivery: %s%n", env.now(), event.value());
            env.process(assembly(env));
        });

        shipment.addCallback(event -> System.out.printf("t=%.0f -> shipment ready%n", env.now()));
        env.run();
    }

    private static Process.ProcessFunction assembly(Environment env) {
        return process -> {
            Timeout frame = env.timeout(4, "frame");
            Timeout electronics = env.timeout(2, "electronics");
            AllOf finished = env.allOf(frame, electronics);
            yield finished;
            System.out.printf("t=%.0f -> assembly complete%n", env.now());
        };
    }

    private static Process.ProcessFunction shipping(Environment env) {
        return process -> {
            yield env.timeout(10);
            System.out.printf("t=%.0f -> shipping triggered%n", env.now());
        };
    }
}

