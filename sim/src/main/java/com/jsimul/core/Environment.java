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
    return head == null ? Infinity : head.time;
  }

  @Override
  public void step() {
    Scheduled s = queue.poll();
    if (s == null) throw new EmptySchedule();
    this.now = s.time;

    Event event = s.event;
    var callbacks = event.detachCallbacks();
    for (Event.Callback cb : callbacks) {
      cb.call(event);
    }

    if (!event.ok() && !event.isDefused()) {
      RuntimeException exc = event.failureAsRuntime();
      throw exc;
    }
  }

  @Override
  public Object run(Object until) {
    Event untilEvent = null;
    if (until == null) {
      // run until no events left
    } else if (until instanceof Event) {
      untilEvent = (Event) until;
      if (untilEvent.isProcessed()) return untilEvent.value();
      untilEvent.addCallback(StopSimulation::callback);
    } else {
      double at = ((Number) until).doubleValue();
      if (at <= now) throw new IllegalArgumentException("until must be > now");
      untilEvent = new Event(this).markOk(null);
      schedule(untilEvent, Event.URGENT, at - now);
      untilEvent.addCallback(StopSimulation::callback);
    }

    try {
      while (true) step();
    } catch (StopSimulation e) {
      return e.value();
    } catch (EmptySchedule e) {
      if (untilEvent != null && untilEvent.triggered()) {
        throw new RuntimeException("No scheduled events left but \"until\" not triggered: " + untilEvent);
      }
      return null;
    }
  }

  /** Internal scheduled entry. */
  static final class Scheduled implements Comparable<Scheduled> {
    final double time;
    final int priority;
    final long id;
    final Event event;

    Scheduled(double time, int priority, long id, Event event) {
      this.time = time;
      this.priority = priority;
      this.id = id;
      this.event = event;
    }

    @Override
    public int compareTo(Scheduled o) {
      int c = Double.compare(this.time, o.time);
      if (c != 0) return c;
      c = Integer.compare(this.priority, o.priority);
      if (c != 0) return c;
      return Long.compare(this.id, o.id);
    }
  }
}