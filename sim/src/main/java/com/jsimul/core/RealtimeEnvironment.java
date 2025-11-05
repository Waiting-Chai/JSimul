package com.jsimul.core;

/**
 * Real-time environment wrapper that keeps simulation time in sync with the
 * wall clock. The inner {@link Environment} retains responsibility for event
 * management while this facade adds scheduling delays before each step.
 *
 * <p>All factory helpers delegate to the wrapped environment so existing
 * components ({@link Process}, {@link Timeout}, etc.) can be reused without
 * modification.
 *
 * @author waiting
 * @date 2025/11/05
 */
public final class RealtimeEnvironment implements BaseEnvironment {

    private final Environment delegate;

    private final double factor;

    private final boolean strict;

    private final double envStart;

    private volatile double realStart;

    /**
     * Create a real-time environment starting at time {@code 0} with a wall
     * clock factor of {@code 1.0} and strict mode enabled.
     */
    public RealtimeEnvironment() {
        this(0.0, 1.0, true);
    }

    /**
     * @param initialTime virtual time origin
     * @param factor      mapping between virtual seconds and real seconds
     * @param strict      when true, throw if execution lags behind real time by more
     *                    than one simulated step
     */
    public RealtimeEnvironment(double initialTime, double factor, boolean strict) {
        this.delegate = new Environment(initialTime);
        this.factor = factor;
        this.strict = strict;
        this.envStart = initialTime;
        this.realStart = monotonicSeconds();
    }

    /**
     * @return scaling factor applied to virtual time deltas
     */
    public double factor() {
        return factor;
    }

    /**
     * @return whether the environment enforces strict timing checks
     */
    public boolean strict() {
        return strict;
    }

    /**
     * Reset the wall clock baseline. Useful if a long pause happened between
     * environment creation and {@link #run(Object)}.
     */
    public void sync() {
        this.realStart = monotonicSeconds();
    }

    /**
     * Forward factory to the inner environment.
     */
    public Process process(Process.ProcessFunction function) {
        return delegate.process(function);
    }

    public Timeout timeout(double delay) {
        return delegate.timeout(delay);
    }

    public Timeout timeout(double delay, Object value) {
        return delegate.timeout(delay, value);
    }

    public Event event() {
        return delegate.event();
    }

    public SimEvent allOf(Object... events) {
        return delegate.allOf(events);
    }

    public SimEvent anyOf(Object... events) {
        return delegate.anyOf(events);
    }

    public void exit(Object value) {
        delegate.exit(value);
    }

    public void exit() {
        delegate.exit();
    }

    @Override
    public double now() {
        return delegate.now();
    }

    @Override
    public Process activeProcess() {
        return delegate.activeProcess();
    }

    @Override
    public void schedule(Event event, int priority, double delay) {
        delegate.schedule(event, priority, delay);
    }

    /**
     * Expose the next scheduled time for testability.
     */
    public double peek() {
        return delegate.peek();
    }

    @Override
    public void step() {
        double evtTime = peek();
        if (evtTime == Environment.Infinity) {
            throw new EmptySchedule();
        }

        double target = realStart + (evtTime - envStart) * factor;
        delayUntil(target);
        delegate.step();
    }

    @Override
    public Object run(Object until) {
        Event sentinel = resolveUntil(until);
        if (sentinel != null && sentinel.isProcessed()) {
            return sentinel.value();
        }
        return executeLoop(sentinel);
    }

    private Object executeLoop(Event sentinel) {
        while (true) {
            try {
                step();
            } catch (StopSimulation stop) {
                return stop.value();
            } catch (EmptySchedule empty) {
                if (sentinel != null) {
                    waitForScheduling(sentinel);
                    continue;
                }
                return null;
            }
        }
    }

    private void waitForScheduling(Event sentinel) {
        while (peek() == Environment.Infinity && !sentinel.isProcessed() && !sentinel.triggered()) {
            Thread.onSpinWait();
        }
    }

    private Event resolveUntil(Object until) {
        if (until == null) {
            return null;
        }
        if (until instanceof Event e) {
            if (e.isProcessed()) {
                return e;
            }
            e.addCallback(StopSimulation::callback);
            return e;
        }
        if (until instanceof SimEvent se) {
            return resolveUntil(se.asEvent());
        }
        if (until instanceof Number number) {
            double target = number.doubleValue();
            if (target <= now()) {
                throw new IllegalArgumentException("until must be > now");
            }
            Event event = new Event(delegate).markOk(null);
            schedule(event, Event.URGENT, target - now());
            event.addCallback(StopSimulation::callback);
            return event;
        }
        throw new IllegalArgumentException("Unsupported until type: " + until);
    }

    private void delayUntil(double target) {
        while (true) {
            double delta = target - monotonicSeconds();
            if (delta <= 0) {
                break;
            }
            sleep(delta);
        }
        if (strict) {
            double lag = monotonicSeconds() - target;
            if (lag > factor) {
                throw new RuntimeException(String.format("Simulation too slow for real time (lag=%.3fs)", lag));
            }
        }
    }

    private void sleep(double seconds) {
        long millis = (long) (seconds * 1000);
        if (millis > 1) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } else {
            Thread.onSpinWait();
        }
    }

    private double monotonicSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
    }
}
