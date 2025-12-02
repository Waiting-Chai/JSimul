package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Compositional core for shared resources (put/get queues).
 *
 * @author waiting
 * @date 2025/10/29
 */
public final class BaseResource {

    final Environment env;

    final int capacity;

    final List<SimEvent> putQueue = java.util.Collections.synchronizedList(new ArrayList<>());

    final List<SimEvent> getQueue = java.util.Collections.synchronizedList(new ArrayList<>());

    private final BiFunction<SimEvent, BaseResource, Boolean> doPut;

    private final BiFunction<SimEvent, BaseResource, Boolean> doGet;

    public BaseResource(Environment env, int capacity,
                        BiFunction<SimEvent, BaseResource, Boolean> doPut,
                        BiFunction<SimEvent, BaseResource, Boolean> doGet) {
        this.env = env;
        this.capacity = capacity;
        this.doPut = doPut;
        this.doGet = doGet;
    }

    public int capacity() {
        return capacity;
    }

    public SimEvent put() {
        return new Put(this);
    }

    public SimEvent get() {
        return new Get(this);
    }

    private boolean _doPut(SimEvent event) {
        if (doPut == null) throw new UnsupportedOperationException("Put behavior not set");
        return doPut.apply(event, this);
    }

    private boolean _doGet(SimEvent event) {
        if (doGet == null) throw new UnsupportedOperationException("Get behavior not set");
        return doGet.apply(event, this);
    }

    public void triggerPut(Event getEvent) {
        synchronized (putQueue) {
            int idx = 0;
            while (idx < putQueue.size()) {
                SimEvent se = putQueue.get(idx);
                Event e = se.asEvent();
                boolean proceed = _doPut(se);
                // Keep pending events in the queue; remove completed ones
                if (!e.triggered()) {
                    idx++;
                } else if (putQueue.remove(idx) != se) {
                    throw new RuntimeException("Put queue invariant violated");
                }
                if (!proceed) break;
            }
        }
    }

    public void triggerGet(Event putEvent) {
        synchronized (getQueue) {
            int idx = 0;
            while (idx < getQueue.size()) {
                SimEvent se = getQueue.get(idx);
                Event e = se.asEvent();
                boolean proceed = _doGet(se);
                // Keep pending events in the queue; remove completed ones
                if (!e.triggered()) {
                    idx++;
                } else if (getQueue.remove(idx) != se) {
                    throw new RuntimeException("Get queue invariant violated");
                }
                if (!proceed) break;
            }
        }
    }

    public int putQueueSize() {
        return putQueue.size();
    }

    public int getQueueSize() {
        return getQueue.size();
    }

}
