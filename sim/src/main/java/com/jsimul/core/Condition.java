package com.jsimul.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Condition event triggered when evaluate(events,count) returns true.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class Condition extends Event {
  public static boolean allEvents(List<Event> events, int count) { return events.size() == count; }

  public static boolean anyEvents(List<Event> events, int count) {
    return count > 0 || events.isEmpty();
  }

  private final BiPredicate<List<Event>, Integer> evaluate;
  private final List<Event> events;
  private int count;

  public Condition(Environment env, BiPredicate<List<Event>, Integer> evaluate, List<Event> events) {
    super(env);
    this.evaluate = evaluate;
    this.events = new ArrayList<>(events);

    if (this.events.isEmpty()) {
      succeed(new ConditionValue());
      return;
    }

    for (Event e : this.events) {
      if (e.env() != env) {
        throw new IllegalArgumentException("Cannot mix events from different environments");
      }
    }

    for (Event e : this.events) {
      if (e.isProcessed()) {
        check(e);
      } else {
        e.addCallback(this::check);
      }
    }

    this.addCallback(this::buildValue);
  }

  private void populateValue(ConditionValue cv) {
    for (Event e : events) {
      if (e instanceof Condition) {
        ((Condition) e).populateValue(cv);
      } else if (e.isProcessed()) {
        cv.add(e);
      }
    }
  }

  private void buildValue(Event event) {
    if (event.ok()) {
      ConditionValue cv = new ConditionValue();
      populateValue(cv);
      this.value = cv;
    }
  }

  private void check(Event e) {
    if (this.value != PENDING) return;
    this.count += 1;
    if (!e.ok()) {
      e.setDefused(true);
      fail((Throwable) e.value());
      return;
    }
    if (evaluate.test(events, count)) {
      succeed(null);
    }
  }
}