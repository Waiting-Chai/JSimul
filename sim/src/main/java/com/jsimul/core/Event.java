package com.jsimul.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base Event type mirroring SimPy's Event.
 *
 * <p>Events are bound to an Environment, can be triggered successfully or with failure, and
 * invoke registered callbacks when processed by the environment.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Event {
    public static final Object PENDING = new Object();

    public static final int URGENT = 0;

    public static final int NORMAL = 1;

    /**
     * Simple callback interface.
     */
    public interface Callback {
        void call(Event event);
    }

    protected final Environment env;

    protected List<Callback> callbacks = new ArrayList<>();

    protected Object value = PENDING;

    protected boolean ok;

    protected boolean defused;

    public Event(Environment env) {
        this.env = env;
    }

    public Environment env() {
        return env;
    }

    public String toString() {
        return getClass().getSimpleName() + "()";
    }

    public boolean triggered() {
        // Triggered means the event has been completed (value set),
        // aligning with common past-tense semantics.
        return value != PENDING;
    }

    public boolean isProcessed() {
        return callbacks == null;
    }

    public boolean ok() {
        return ok;
    }

    public boolean isDefused() {
        return defused;
    }

    public void setDefused(boolean v) {
        this.defused = v;
    }

    public Object value() {
        if (value == PENDING) throw new IllegalStateException("Event value not yet available");
        return value;
    }

    public void addCallback(Callback cb) {
        if (callbacks != null) callbacks.add(cb);
    }

    /**
     * Detach current callbacks for processing; set callbacks to null.
     */
    List<Callback> detachCallbacks() {
        List<Callback> cbs = callbacks;
        callbacks = null;
        return cbs == null ? Collections.emptyList() : cbs;
    }

    /**
     * Trigger with another event's state and value.
     */
    public Event trigger(Event other) {
        this.ok = other.ok;
        this.value = other.value;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    /**
     * Succeed with value.
     */
    public Event succeed(Object v) {
        if (value != PENDING) throw new RuntimeException(this + " has already been triggered");
        this.ok = true;
        this.value = v;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    /**
     * Mark OK with value without scheduling (internal use).
     */
    Event markOk(Object v) {
        this.ok = true;
        this.value = v;
        return this;
    }

    /**
     * Fail with exception.
     */
    public Event fail(Throwable ex) {
        if (value != PENDING) throw new RuntimeException(this + " has already been triggered");
        if (ex == null) throw new IllegalArgumentException("not an exception");
        this.ok = false;
        this.value = ex;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    RuntimeException failureAsRuntime() {
        Throwable t = (Throwable) value;
        RuntimeException rt =
                (t instanceof RuntimeException) ? (RuntimeException) t : new RuntimeException(t.getMessage(), t);
        rt.initCause(t);
        return rt;
    }
}