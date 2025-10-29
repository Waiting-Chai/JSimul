package com.jsimul.collections;

import com.jsimul.core.Environment;

/**
 * Container for continuous or discrete matter up to capacity, supporting put/get of quantities.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Container {

    private final BaseResource core;

    private double level;

    public Container(Environment env, double capacity, double initial) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        if (initial < 0 || initial > capacity) throw new IllegalArgumentException("invalid initial");
        this.level = initial;
        this.core = new BaseResource(
                env,
                (int) Math.ceil(capacity),
                (event, res) -> {
                    PutEvent pe = (PutEvent) event;
                    if (level + pe.amount <= capacity) {
                        level += pe.amount;
                        pe.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    GetEvent ge = (GetEvent) event;
                    if (level >= ge.amount) {
                        level -= ge.amount;
                        ge.asEvent().succeed(ge.amount);
                    }
                    return true;
                }
        );
    }

    public double level() {
        return level;
    }

    public PutEvent put(double amount) {
        return new PutEvent(core, amount);
    }

    public GetEvent get(double amount) {
        return new GetEvent(core, amount);
    }

}