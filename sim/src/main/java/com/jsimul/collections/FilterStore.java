package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Store supporting filtered get requests.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStore {

    private final BaseResource core;

    protected final List<Object> items = new ArrayList<>();

    public FilterStore(Environment env, int capacity) {
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
                    if (event instanceof FilterStoreGet get) {
                        if (get.filter == null) {
                            get.asEvent().fail(new IllegalArgumentException("filter cannot be null"));
                            return true;
                        }
                        for (int i = 0; i < items.size(); i++) {
                            Object item = items.get(i);
                            if (get.filter.test(item)) {
                                items.remove(i);
                                get.asEvent().succeed(item);
                                break;
                            }
                        }
                        return true;
                    }
                    StoreGet get = (StoreGet) event;
                    if (!items.isEmpty()) {
                        Object v = items.remove(0);
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

    public FilterStoreGet get(Predicate<Object> filter) {
        return new FilterStoreGet(core, filter);
    }

}
