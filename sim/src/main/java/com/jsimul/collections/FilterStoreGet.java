package com.jsimul.collections;

import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

import java.util.function.Predicate;

/**
 * Filtered get event for FilterStore.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStoreGet implements SimEvent {

    final BaseResource<?, FilterStoreGet> resource;

    final Predicate<?> filter;

    private final Event inner;

    FilterStoreGet(BaseResource<?, FilterStoreGet> resource, Predicate<?> filter) {
        this.resource = resource;
        this.filter = filter;
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