package com.jsimul.core;

/**
 * A timeout event that is triggered after a delay (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Timeout implements SimEvent {

    private final double delay;

    private final Event inner;

    private final Object v;

    public Timeout(Environment env, double delay, Object value) {
        if (delay < 0) throw new IllegalArgumentException("Negative delay " + delay);
        this.delay = delay;
        this.v = value;
        this.inner = new Event(env);
        this.inner.markOk(value);
        env.schedule(this.inner, Event.NORMAL, delay);
    }

    public Timeout(Environment env, double delay) {
        this(env, delay, null);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + delay + (v == null ? ")" : ", value=" + v + ")");
    }

}