package com.jsimul.core;

import java.util.Arrays;
import java.util.List;

/**
 * AllOf condition: succeeds when all events are processed successfully (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class AllOf implements SimEvent {

    private final Event inner;

    public AllOf(Environment env, List<?> events) {
        this.inner = new Condition(env, Condition::allEvents, events).asEvent();
    }

    public AllOf(Environment env, Object... events) {
        this(env, Arrays.asList(events));
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}