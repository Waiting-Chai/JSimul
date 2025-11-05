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

        private void completeFromEvent(Event ev, CompletableFuture<Object> fut) {
            if (ev.ok()) {
                fut.complete(ev.value());
            } else {
                Throwable t = (Throwable) ev.value();
                if (t == null) {
                    fut.completeExceptionally(new RuntimeException("Event failed without cause"));
                } else {
                    fut.completeExceptionally(t);
                }
            }
        }

        /**
         * Await completion of an event; returns its value or throws if failed/interrupted.
         */
        public Object await(Event e) throws Exception {
            // Record the current target event for observability and SimPy parity
            target = e;
            CompletableFuture<Object> fut = new CompletableFuture<>();
            currentWait.set(fut);
            Event.Callback callback = ev -> completeFromEvent(ev, fut);
            e.addCallback(callback);
            // Handle race where event already processed before callback registration
            if (e.isProcessed() && !fut.isDone()) {
                completeFromEvent(e, fut);
            }
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
         * Type-safe overload to await a compositional SimEvent.
         */
        public Object await(SimEvent e) throws Exception {
            return await(e.asEvent());
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
        // Alive means completion event not yet triggered
        return !inner.triggered();
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
                if (!inner.triggered()) {
                    inner.markOk(ret);
                }
                env.schedule(inner, Event.NORMAL, 0);
            } catch (ProcessExit exit) {
                inner.markOk(exit.value());
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
    public Environment env() {
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
