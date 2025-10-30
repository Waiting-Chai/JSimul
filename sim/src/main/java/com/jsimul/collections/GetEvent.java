package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Container get event for removing quantity.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class GetEvent implements SimEvent {

    final BaseResource resource;

    final double amount;

    private final Event inner;

    GetEvent(BaseResource resource, double amount) {
        this.resource = resource;
        this.amount = amount;
        this.inner = new Event(resource.env);
        resource.getQueue.add(this);
        this.inner.addCallback(resource::triggerPut);
        resource.triggerGet(null);
    }

    public void cancel() {
        if (!inner.triggered()) resource.getQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}