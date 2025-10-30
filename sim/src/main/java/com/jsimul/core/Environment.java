package com.jsimul.core;

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

    private double now;

    private final PriorityQueue<Scheduled> queue;

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
        queue.add(new Scheduled(now + delay, priority, eid.incrementAndGet(), event));
    }

    public double peek() {
        Scheduled head = queue.peek();
        return head == null ? Infinity : head.time();
    }

    @Override
    public void step() {
        Scheduled s = queue.poll();
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
                // If we have an until sentinel, and it has NOT yet triggered, wait for new events.
                if ( untilEvent != null ) {
                    while ( peek() == Infinity && !untilEvent.isProcessed() && !untilEvent.triggered() ) {
                        Thread.onSpinWait();
                    }
                    // Either new events are scheduled or untilEvent got triggered; try stepping again.
                    continue;
                }
                // No sentinel: terminate when the schedule is empty.
                return null;
            }
        }
    }

}