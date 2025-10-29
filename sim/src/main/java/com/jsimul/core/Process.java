package com.jsimul.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process wrapping user logic that awaits events. Modeled after SimPy's Process.
 *
 * <p>This implementation uses virtual threads (Java 21) to run process logic and provides a
 * blocking await(Event) API via {@link ProcessContext}.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Process implements SimEvent {

    /**
     * Functional interface for process logic.
     */
    @FunctionalInterface
    public interface ProcessFunction {
        Object run(ProcessContext ctx) throws Exception;
    }

    /**
     * Context passed into user logic to await events and access environment.
     */
    public final class ProcessContext {

        public Environment env() {
            return env;
        }

        /**
         * Await completion of an event; returns its value or throws if failed/interrupted.
         */
        public Object await(Event e) throws Exception {
            // Record the current target event for observability and SimPy parity
            target = e;
            CompletableFuture<Object> fut = new CompletableFuture<>();
            currentWait.set(fut);
            e.addCallback(ev -> {
                if (ev.ok()) {
                    fut.complete(ev.value());
                } else {
                    // propagate failure
                    Throwable t = (Throwable) ev.value();
                    fut.completeExceptionally(t);
                }
            });
            try {
                return fut.join();
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof Interrupt) throw (Interrupt) cause;
                if (cause instanceof Exception) throw (Exception) cause;
                throw ex;
            } finally {
                currentWait.compareAndSet(fut, null);
                // Clear the target once the wait completes (success or failure)
                target = null;
            }
        }

        /**
         * Await overload that accepts either Event or SimEvent.
         * If a SimEvent is provided, its underlying Event is awaited.
         */
        public Object await(Object e) throws Exception {
            if (e instanceof Event) return await((Event) e);
            if (e instanceof SimEvent) return await(((SimEvent) e).asEvent());
            throw new IllegalArgumentException("Unsupported await type: " + e);
        }

    }

    private final Environment env;

    private final Event inner;

    private final ProcessFunction function;

    private final ProcessContext ctx = new ProcessContext();

    private final AtomicReference<CompletableFuture<Object>> currentWait = new AtomicReference<>();

    private volatile Event target;

    private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

    public Process(Environment env, ProcessFunction function) {
        this.env = env;
        this.inner = new Event(env);
        this.function = function;
        // schedule initialization urgently to start before interrupts
        env.schedule(Initialize.make(env, this), Event.URGENT, 0);
    }

    public Event target() {
        return target;
    }

    public boolean isAlive() {
        return inner.triggered();
    }

    /**
     * Interrupt this process with optional cause.
     */
    public void interrupt(Object cause) {
        env.schedule(Interruption.make(this, cause), Event.URGENT, 0);
    }

    void _start() {
        EXEC.submit(() -> {
            env.setActiveProcess(this);
            try {
                Object ret = function.run(ctx);
                inner.markOk(ret);
                env.schedule(inner, Event.NORMAL, 0);
            } catch (Throwable t) {
                inner.fail(t);
            } finally {
                env.setActiveProcess(null);
            }
        });
    }

    void _resume(Event e) {
        // Initialize: ok==true -> start process; Interruption: ok==false with Interrupt value
        if (e.ok()) {
            _start();
        } else {
            CompletableFuture<Object> wait = currentWait.get();
            if (wait != null && e.value() instanceof Throwable) {
                wait.completeExceptionally((Throwable) e.value());
            }
        }
    }

    /**
     * Expose Environment for internal helper events.
     */
    Environment env() {
        return env;
    }

    /**
     * Underlying completion event for this process.
     */
    @Override
    public Event asEvent() {
        return inner;
    }

}