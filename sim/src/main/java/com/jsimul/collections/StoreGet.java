package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Store get event returning next item.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class StoreGet implements SimEvent {

    final BaseResource resource;

    private final Event inner;

    StoreGet(BaseResource resource) {
        this.resource = resource;
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