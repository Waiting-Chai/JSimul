package com.jsimul.collections;

import com.jsimul.core.Environment;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Store that maintains items in priority order (min-heap) using composition.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class PriorityStore<T> {

    private final BaseResource core;

    private final PriorityQueue<T> heap;

    public PriorityStore(Environment env, int capacity) {
        this(env, capacity, null);
    }

    public PriorityStore(Environment env, int capacity, Comparator<? super T> comparator) {
        this.heap = comparator == null ? new PriorityQueue<>() : new PriorityQueue<>(comparator);
        this.core = new BaseResource(
                env,
                capacity,
                (event, res) -> {
                    StorePut put = (StorePut) event;
                    if (heap.size() < res.capacity) {
                        @SuppressWarnings("unchecked")
                        T item = (T) put.item;
                        heap.add(item);
                        put.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    StoreGet get = (StoreGet) event;
                    if (!heap.isEmpty()) {
                        T v = heap.poll();
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

}