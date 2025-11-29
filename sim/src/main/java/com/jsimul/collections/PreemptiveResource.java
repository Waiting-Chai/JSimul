package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resource with priority-based preemption. Lower priority value == higher priority.
 *
 * <p>When capacity is full and a higher-priority request arrives, the running user with the
 * lowest priority is preempted and its request fails with a {@link Preempted} exception carrying
 * the new request as the cause.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PreemptiveResource {

    private final Environment env;
    private final int capacity;
    private final List<PreemptiveRequest> users = new ArrayList<>();
    private final PriorityQueue<PreemptiveRequest> waiters;
    private final AtomicLong order = new AtomicLong();
    private final AtomicLong grants = new AtomicLong();
    private final AtomicLong preemptions = new AtomicLong();

    public PreemptiveResource(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.env = env;
        this.capacity = capacity;
        this.waiters = new PriorityQueue<>();
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
        return grants.get();
    }

    public long preemptionCount() {
        return preemptions.get();
    }

    public PreemptiveRequest request(int priority) {
        return request(priority, true);
    }

    public PreemptiveRequest request(int priority, boolean preempt) {
        return new PreemptiveRequest(this, priority, preempt, order.getAndIncrement());
    }

    public PreemptiveRequest request(int priority, boolean preempt, double timeout) {
        PreemptiveRequest req = request(priority, preempt);
        if (timeout > 0) {
            env.timeout(timeout).addCallback(ev -> {
                if (!req.asEvent().triggered()) {
                    cancelRequest(req);
                    req.asEvent().fail(new RuntimeException("PreemptiveRequest timeout"));
                    env.schedule(req.asEvent(), Event.NORMAL, 0);
                }
            });
        }
        return req;
    }

    public PreemptiveRelease release(PreemptiveRequest req) {
        return new PreemptiveRelease(this, req);
    }

    void onRequest(PreemptiveRequest req) {
        if (users.size() < capacity) {
            grant(req);
            return;
        }
        // Capacity full: check for preemption if allowed
        PreemptiveRequest victim = findWorstUser();
        if (req.isPreempt() && victim != null && req.compareTo(victim) < 0) {
            preempt(victim, req);
            grant(req);
        } else {
            waiters.add(req);
        }
    }

    void cancelRequest(PreemptiveRequest req) {
        waiters.remove(req);
    }

    void onRelease(PreemptiveRelease rel) {
        PreemptiveRequest req = rel.request;
        if (!users.remove(req)) {
            rel.asEvent().fail(new IllegalArgumentException("Request not holding resource"));
            env.schedule(rel.asEvent(), Event.NORMAL, 0);
            return;
        }
        rel.asEvent().succeed(null);
        grantFromQueue();
    }

    private void grantFromQueue() {
        while (users.size() < capacity && !waiters.isEmpty()) {
            PreemptiveRequest next = waiters.poll();
            if (next == null || next.asEvent().triggered()) continue;
            grant(next);
        }
    }

    private void grant(PreemptiveRequest req) {
        users.add(req);
        grants.incrementAndGet();
        req.asEvent().succeed(null);
    }

    private PreemptiveRequest findWorstUser() {
        return users.stream()
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private void preempt(PreemptiveRequest victim, PreemptiveRequest intruder) {
        if (!users.remove(victim)) {
            return;
        }
        victim.markPreempted();
        preemptions.incrementAndGet();
        if (!victim.asEvent().triggered()) {
            victim.asEvent().fail(new Preempted(intruder));
        }
    }
}
