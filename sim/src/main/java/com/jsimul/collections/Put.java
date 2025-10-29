package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Put request event for BaseResource.
 *
 * @author waiting
 * @date 2025/10/29
 */
class Put implements SimEvent {

    final BaseResource resource;

    private final Event inner;

    Put(BaseResource resource) {
        this.resource = resource;
        this.inner = new Event(resource.env);
        resource.putQueue.add(this);
        this.inner.addCallback(resource::triggerGet);
        resource.triggerPut(null);
    }

    public void cancel() {
        if (inner.triggered()) resource.putQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}