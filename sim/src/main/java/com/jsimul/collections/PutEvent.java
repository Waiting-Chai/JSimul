package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Container put event for adding quantity.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class PutEvent implements SimEvent {

    final BaseResource resource;

    final double amount;

    private final Event inner;

    PutEvent(BaseResource resource, double amount) {
        this.resource = resource;
        this.amount = amount;
        this.inner = new Event(resource.env);
        resource.putQueue.add(this);
        this.inner.addCallback(resource::triggerGet);
        resource.triggerPut(null);
    }

    public void cancel() {
        if (!inner.triggered()) resource.putQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}