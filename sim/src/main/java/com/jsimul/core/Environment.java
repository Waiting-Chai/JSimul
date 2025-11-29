package com.jsimul.core;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Execution environment for an event-based simulation, modeled after SimPy's Environment.
 *
 * <p>Time advances by stepping through scheduled events. Events are scheduled with a time and
 * priority, and processed in order. Failed events will crash the environment unless defused.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Environment implements BaseEnvironment {

    public static final double Infinity = Double.POSITIVE_INFINITY;

    private volatile double now;

    private final PriorityQueue<Scheduled> queue;
    
    private final Object queueLock = new Object();

    private final AtomicLong eid;

    private Process activeProcess;

    public Environment() {
        this(0.0);
    }

    public Environment(double initialTime) {
        this.now = initialTime;
        this.queue = new PriorityQueue<>();
        this.eid = new AtomicLong();
    }

    @Override
    public double now() {
        return now;
    }

    @Override
    public Process activeProcess() {
        return activeProcess;
    }

    void setActiveProcess(Process p) {
        this.activeProcess = p;
    }

    @Override
    public void schedule(Event event, int priority, double delay) {
        double time = now + delay;
        synchronized (queueLock) {
            queue.add(new Scheduled(time, priority, eid.incrementAndGet(), event));
            queueLock.notifyAll();
        }
    }

    /**
     * Create and start a {@link Process} using the supplied function. Matches
     * the ergonomics of SimPy's {@code env.process(generator)} helper so that
     * callers do not need to interact with {@link Process} constructors
     * directly.
     *
     * @param function user logic to execute inside the process
     * @return newly created process instance
     */
    public Process process(Process.ProcessFunction function) {
        return new Process(this, function);
    }

    /**
     * Create a {@link Timeout} that fires after the given delay.
     */
    public Timeout timeout(double delay) {
        return new Timeout(this, delay);
    }

    /**
     * Create a {@link Timeout} with a payload that will be produced when the
     * timeout fires.
     */
    public Timeout timeout(double delay, Object value) {
        return new Timeout(this, delay, value);
    }

    /**
     * Create a bare {@link Event} bound to this environment, mirroring
     * SimPy's {@code env.event()} helper.
     */
    public Event event() {
        return new Event(this);
    }

    /**
     * Create an {@link AllOf} compositional event for the provided operands.
     */
    public SimEvent allOf(Object... events) {
        return new AllOf(this, normalizeArgs(events));
    }

    /**
     * Create an {@link AnyOf} compositional event for the provided operands.
     */
    public SimEvent anyOf(Object... events) {
        return new AnyOf(this, normalizeArgs(events));
    }

    /**
     * Terminate the currently active process successfully with the given
     * return value. This emulates SimPy's {@code env.exit(value)} convenience
     * for users who prefer early exits instead of {@code return}.
     *
     * @param value return value to propagate from the active process
     */
    public void exit(Object value) {
        if (activeProcess == null) {
            throw new IllegalStateException("No active process to exit");
        }
        throw new ProcessExit(value);
    }

    /**
     * Convenience overload that terminates the active process without a
     * result value.
     */
    public void exit() {
        exit(null);
    }

    public double peek() {
        synchronized (queueLock) {
            Scheduled head = queue.peek();
            return head == null ? Infinity : head.time();
        }
    }

    @Override
    public void step() {
        Scheduled s;
        synchronized (queueLock) {
            s = queue.poll();
        }
        if (s == null) throw new EmptySchedule();
        this.now = s.time();

        Event event = s.event();
        var callbacks = event.detachCallbacks();
        for (Event.Callback cb : callbacks) {
            cb.call(event);
        }

        if (!event.ok() && !event.isDefused()) {
            throw event.failureAsRuntime();
        }
    }

    @Override
    public Object run(Object until) {
        return switch (until) {
            case null -> run();
            case Event event -> run(event);
            case SimEvent simEvent -> run(simEvent);
            default -> run(((Number) until).doubleValue());
        };
    }

    /**
     * Run until no events are left.
     */
    public Object run() {
        return runInternal(null);
    }

    /**
     * Run until the given Event is processed.
     */
    public Object run(Event untilEvent) {
        if (untilEvent == null) return run();
        if (untilEvent.isProcessed()) return untilEvent.value();
        untilEvent.addCallback(StopSimulation::callback);
        return runInternal(untilEvent);
    }

    /**
     * Run until the given compositional event is processed.
     */
    public Object run(SimEvent untilEvent) {
        return run(untilEvent == null ? null : untilEvent.asEvent());
    }

    /**
     * Run until the given absolute time is reached.
     */
    public Object run(double untilTime) {
        if (untilTime <= now) throw new IllegalArgumentException("until must be > now");
        Event untilEvent = new Event(this).markOk(null);
        schedule(untilEvent, Event.URGENT, untilTime - now);
        untilEvent.addCallback(StopSimulation::callback);
        return runInternal(untilEvent);
    }

    /**
     * Core run loop shared by all run overloads.
     */
    private Object runInternal(Event untilEvent) {
        while ( true ) {
            try {
                step();
            } catch ( StopSimulation e ) {
                return e.value();
            } catch ( EmptySchedule e ) {
                if ( untilEvent != null && !untilEvent.triggered() && !untilEvent.isProcessed() ) {
                    // Allow a short grace period for asynchronous producers (e.g., processes starting on virtual threads)
                    if ( awaitNewEvents(untilEvent, 50) ) {
                        continue;
                    }
                    throw new RuntimeException("No scheduled events left before until condition is met");
                }
                return null; // terminate when the schedule is empty
            }
        }
    }

    private List<Object> normalizeArgs(Object[] events) {
        return Arrays.asList(events == null ? new Object[0] : events);
    }

    /**
     * Wait briefly for new events to appear or the untilEvent to complete.
     * Returns true if either condition occurs within the timeout.
     */
    private boolean awaitNewEvents(Event untilEvent, long timeoutMillis) {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while ( System.nanoTime() < deadline ) {
            if ( untilEvent.triggered() || untilEvent.isProcessed() ) {
                return true;
            }
            synchronized ( queueLock ) {
                if ( !queue.isEmpty() ) {
                    return true;
                }
                long remainingNanos = deadline - System.nanoTime();
                if ( remainingNanos <= 0 ) {
                    break;
                }
                try {
                    queueLock.wait(Math.max(1L, remainingNanos / 1_000_000L));
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return untilEvent.triggered() || untilEvent.isProcessed() || !queue.isEmpty();
    }

}
