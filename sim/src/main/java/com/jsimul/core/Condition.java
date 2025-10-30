package com.jsimul.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Condition event triggered when evaluate(events,count) returns true (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Condition implements SimEvent {

    public static boolean allEvents(List<Event> events, int count) {
        return events.size() == count;
    }

    public static boolean anyEvents(List<Event> events, int count) {
        return count > 0 || events.isEmpty();
    }

    private final Event inner;

    private final BiPredicate<List<Event>, Integer> evaluate;

    private final List<Event> events;

    private int count;

    public Condition(Environment env, BiPredicate<List<Event>, Integer> evaluate, List<?> events) {
        this.inner = new Event(env);
        this.evaluate = evaluate;
        this.events = normalize(events);

        if (this.events.isEmpty()) {
            inner.succeed(new ConditionValue());
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

        this.inner.addCallback(this::buildValue);
    }

    private static List<Event> normalize(List<?> input) {
        List<Event> out = new ArrayList<>();
        for (Object o : input) {
            if (o instanceof Event) out.add((Event) o);
            else if (o instanceof SimEvent) out.add(((SimEvent) o).asEvent());
            else throw new IllegalArgumentException("Unsupported event type: " + o);
        }
        return out;
    }

    private void populateValue(ConditionValue cv) {
        for (Event e : events) {
            if (e.isProcessed()) cv.add(e);
        }
    }

    private void buildValue(Event event) {
        if (event.ok()) {
            ConditionValue cv = new ConditionValue();
            populateValue(cv);
            inner.markOk(cv);
        }
    }

    private void check(Event e) {
        // If already triggered, no further checks are needed
        if (inner.triggered()) return;
        this.count += 1;
        if (!e.ok()) {
            e.setDefused(true);
            inner.fail((Throwable) e.value());
            return;
        }
        if (evaluate.test(events, count)) {
            inner.succeed(null);
        }
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}