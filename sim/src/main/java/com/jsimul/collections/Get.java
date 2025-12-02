package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Get request event for BaseResource.
 *
 * @author waiting
 * @date 2025/10/29
 */
class Get implements SimEvent {

    final BaseResource<Put, Get> resource;

    private final Event inner;

    Get(BaseResource<Put, Get> resource) {
        this.resource = resource;
        this.inner = new Event(resource.env);
        resource.getQueue.add(this);
        this.inner.addCallback(resource::triggerPut);
        resource.triggerGet(null);
    }

    public void cancel() {
        if (!this.inner.triggered()) resource.getQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}