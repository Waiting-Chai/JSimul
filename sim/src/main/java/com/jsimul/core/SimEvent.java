package com.jsimul.core;

/**
 * Compositional event interface that exposes an underlying Event instance.
 * <p>
 * This allows high-level types (e.g., Timeout, Condition, AllOf, AnyOf) to avoid inheritance
 * while remaining compatible with APIs that operate on Event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public interface SimEvent {

    Event asEvent();

    /**
     * Add a callback to be invoked when this event is processed.
     * This is a convenience method that delegates to the underlying Event.
     */
    default void addCallback(Event.Callback callback) {
        asEvent().addCallback(callback);
    }

}