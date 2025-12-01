package basic;

import com.jsimul.core.Environment;
import com.jsimul.core.Timeout;

/**
 * Minimal end-to-end simulation example: schedule two timeouts and run until all
 * events finish.
 */
public class HelloEnvironment {

    public static void main(String[] args) {
        Environment env = new Environment();

        Timeout first = env.timeout(2, "first");
        Timeout second = env.timeout(1, "second");

        first.addCallback(event -> System.out.printf("t=%.0f -> %s%n", env.now(), event.value()));
        second.addCallback(event -> System.out.printf("t=%.0f -> %s%n", env.now(), event.value()));

        env.run();
    }
}

