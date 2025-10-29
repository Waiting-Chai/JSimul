package com.jsimul.collections;

import com.jsimul.core.Environment;

import java.util.PriorityQueue;

/**
 * Store that maintains items in priority order (min-heap) using composition.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class PriorityStore {

    private final BaseResource core;

    private final PriorityQueue<Object> heap = new PriorityQueue<>();

    public PriorityStore(Environment env, int capacity) {
        this.core = new BaseResource(
                env,
                capacity,
                (event, res) -> {
                    StorePut put = (StorePut) event;
                    if (heap.size() < res.capacity) {
                        heap.add(put.item);
                        put.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    StoreGet get = (StoreGet) event;
                    if (!heap.isEmpty()) {
                        Object v = heap.poll();
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

}