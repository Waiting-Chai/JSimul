package com.jsimul.collections;

import com.jsimul.core.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * FIFO store for arbitrary items with capacity.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Store {

    private final BaseResource core;

    protected final List<Object> items = new ArrayList<>();

    public Store(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.core = new BaseResource(
                env,
                capacity,
                (event, res) -> {
                    StorePut put = (StorePut) event;
                    if (items.size() < capacity) {
                        items.add(put.item);
                        put.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    StoreGet get = (StoreGet) event;
                    if (!items.isEmpty()) {
                        Object v = items.removeFirst();
                        get.asEvent().succeed(v);
                    }
                    return true;
                }
        );
    }

    public StorePut put(Object item) {
        return new StorePut(core, item);
    }

    public StoreGet get() {
        return new StoreGet(core);
    }

    BaseResource core() {
        return core;
    }

}
