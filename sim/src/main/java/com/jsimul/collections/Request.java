package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

/**
 * Resource request event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Request implements SimEvent {

    final BaseResource resource;

    private final Event inner;

    Request(BaseResource resource) {
        this.resource = resource;
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
