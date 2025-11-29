package com.jsimul.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value wrapper for Condition events providing event->value mapping.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ConditionValue {

    private final Map<Event, Object> values = new LinkedHashMap<>();

    void add(Event e) {
        values.put(e, e.value());
    }

    public Map<Event, Object> toMap() {
        return values;
    }

    public boolean contains(Event e) {
        return values.containsKey(e);
    }

    public Object get(Event e) {
        return values.get(e);
    }

    /**
     * @return iterable over events in insertion order.
     */
    public Iterable<Event> events() {
        return values.keySet();
    }

    /**
     * SimPy parity: equality compares underlying event->value mapping.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof ConditionValue other) {
            return Objects.equals(this.values, other.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "ConditionValue" + values;
    }

}
