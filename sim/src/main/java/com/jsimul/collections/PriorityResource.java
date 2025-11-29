package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resource that grants requests by priority (lower first), FIFO within same priority.
 *
 * <p>Preemption is not implemented here; higher-priority requests wait until
 * capacity is available.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PriorityResource {

    private final Environment env;
    private final int capacity;
    private final List<PriorityRequest> users = new ArrayList<>();
    private final PriorityQueue<PriorityRequest> waiters = new PriorityQueue<>();
    private final AtomicLong order = new AtomicLong();
    private final AtomicLong granted = new AtomicLong();

    public PriorityResource(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.env = env;
        this.capacity = capacity;
    }

    Environment env() {
        return env;
    }

    public int capacity() {
        return capacity;
    }

    public int count() {
        return users.size();
    }

    public int waitingCount() {
        return waiters.size();
    }

    public long grantedCount() {
        return granted.get();
    }

    public PriorityRequest request(int priority) {
        return new PriorityRequest(this, priority, order.getAndIncrement());
    }

    public PriorityRelease release(PriorityRequest req) {
        return new PriorityRelease(this, req);
    }

    void onRequest(PriorityRequest req) {
        if (users.size() < capacity) {
            grant(req);
        } else {
            waiters.add(req);
        }
    }

    void cancelRequest(PriorityRequest req) {
        waiters.remove(req);
    }

    void onRelease(PriorityRelease release) {
        PriorityRequest req = release.request;
        if (!users.remove(req)) {
            release.asEvent().fail(new IllegalArgumentException("Request not using this resource"));
            return;
        }
        release.asEvent().succeed(null);
        grantAvailable();
    }

    private void grantAvailable() {
        while (users.size() < capacity && !waiters.isEmpty()) {
            PriorityRequest next = waiters.poll();
            if (next == null) break;
            if (next.asEvent().triggered()) {
                continue;
            }
            grant(next);
        }
    }

    private void grant(PriorityRequest req) {
        users.add(req);
        granted.incrementAndGet();
        req.asEvent().succeed(null);
    }
}
