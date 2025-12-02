package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.SimEvent;

import java.util.function.BiFunction;

/**
 * A simple resource using default Put/Get events.
 *
 * @author waiting
 * @date 2025/12/02
 */
public class SimpleResource extends BaseResource<Put, Get> {

    public SimpleResource(Environment env, int capacity,
                          BiFunction<Put, BaseResource<Put, Get>, Boolean> doPut,
                          BiFunction<Get, BaseResource<Put, Get>, Boolean> doGet) {
        super(env, capacity, doPut, doGet);
    }

    public SimEvent put() {
        return new Put(this);
    }

    public SimEvent get() {
        return new Get(this);
    }
}
