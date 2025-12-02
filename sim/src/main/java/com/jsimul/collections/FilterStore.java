package com.jsimul.collections;

import com.jsimul.core.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Store supporting filtered get requests.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStore<T> {

    private final BaseResource core;

    protected final List<T> items = new ArrayList<>();

    public FilterStore(Environment env, int capacity) {
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
                    if (event instanceof FilterStoreGet get) {
                        if (get.filter == null) {
                            get.asEvent().fail(new IllegalArgumentException("filter cannot be null"));
                            return true;
                        }
                        for (int i = 0; i < items.size(); i++) {
                            T item = items.get(i);
                            @SuppressWarnings("unchecked")
                            Predicate<T> filter = (Predicate<T>) get.filter;
                            if (filter.test(item)) {
                                items.remove(i);
                                get.asEvent().succeed(item);
                                break;
                            }
                        }
                        return true;
                    }
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

    public FilterStoreGet get(Predicate<T> filter) {
        return new FilterStoreGet(core, filter); // FilterStoreGet stores Predicate<Object> or T?
    }

}
