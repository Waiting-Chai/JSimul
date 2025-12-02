package com.jsimul.collections;

import com.jsimul.core.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * FIFO store for arbitrary items with capacity.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class Store<T> {

    private final BaseResource core;

    protected final List<T> items = new ArrayList<>();

    public Store(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.core = new BaseResource(
                env,
                capacity,
                (event, res) -> {
                    StorePut put = (StorePut) event;
                    if (items.size() < capacity) {
                        @SuppressWarnings("unchecked")
                        T item = (T) put.item;
                        items.add(item);
                        put.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    StoreGet get = (StoreGet) event;
                    if (!items.isEmpty()) {
                        T v = items.removeFirst();
                        get.asEvent().succeed(v);
                    }
                    return true;
                }
        );
    }

    public StorePut put(T item) {
        return new StorePut(core, item);
    }

    public StoreGet get() {
        return new StoreGet(core);
    }

    BaseResource core() {
        return core;
    }

}
