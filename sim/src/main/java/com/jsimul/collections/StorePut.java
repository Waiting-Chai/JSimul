package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Store put event carrying an item.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class StorePut implements SimEvent {

    final BaseResource resource;

    final Object item;

    private final Event inner;

    StorePut(BaseResource resource, Object item) {
        this.resource = resource;
        this.item = item;
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