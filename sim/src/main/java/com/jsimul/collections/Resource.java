package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;
import com.jsimul.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple capacity-based Resource supporting request/release semantics.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Resource {

    private final BaseResource core;

    public final List<SimEvent> users = new ArrayList<>();

    public Resource(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.core = new BaseResource(
                env,
                capacity,
                (event, res) -> {
                    Request req = (Request) event;
                    if (users.size() < capacity) {
                        users.add(req);
                        req.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    Release rel = (Release) event;
                    users.remove(rel.request);
                    rel.asEvent().succeed(null);
                    return true;
                }
        );
    }

    public int count() {
        return users.size();
    }

    public Request request() {
        return new Request(core);
    }

    public Release release(Request req) {
        if (req == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (!users.contains(req)) {
            throw new IllegalArgumentException("request does not hold the resource");
        }
        return new Release(core, req);
    }

}
