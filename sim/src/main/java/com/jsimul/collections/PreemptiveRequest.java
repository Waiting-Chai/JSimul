package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Request event for {@link PreemptiveResource}.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PreemptiveRequest implements SimEvent, Comparable<PreemptiveRequest> {

    final PreemptiveResource resource;
    final int priority;
    final long order;
    private final Event inner;
    private volatile boolean preempted;

    PreemptiveRequest(PreemptiveResource resource, int priority, long order) {
        this.resource = resource;
        this.priority = priority;
        this.order = order;
        this.inner = new Event(resource.env());
        resource.onRequest(this);
    }

    public void cancel() {
        if (!inner.triggered()) {
            resource.cancelRequest(this);
        }
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    public boolean isPreempted() {
        return preempted;
    }

    void markPreempted() {
        this.preempted = true;
    }

    @Override
    public int compareTo(PreemptiveRequest other) {
        int p = Integer.compare(this.priority, other.priority);
        if (p != 0) return p;
        return Long.compare(this.order, other.order);
    }
}
