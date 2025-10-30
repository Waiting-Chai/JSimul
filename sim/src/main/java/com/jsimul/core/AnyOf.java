package com.jsimul.core;

import java.util.Arrays;
import java.util.List;

/**
 * AnyOf condition: succeeds when any event is processed successfully (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class AnyOf implements SimEvent {

    private final Event inner;

    public AnyOf(Environment env, List<?> events) {
        this.inner = new Condition(env, Condition::anyEvents, events).asEvent();
    }

    public AnyOf(Environment env, Object... events) {
        this(env, Arrays.asList(events));
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}