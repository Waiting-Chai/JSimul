package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Resource release event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Release implements SimEvent {

    final BaseResource resource;

    final Request request;

    private final Event inner;

    Release(BaseResource resource, Request request) {
        this.resource = resource;
        this.request = request;
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
