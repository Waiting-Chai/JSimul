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
public class Process extends Event {
  /** Functional interface for process logic. */
  @FunctionalInterface
  public interface ProcessFunction {
    Object run(ProcessContext ctx) throws Exception;
  }

  /** Context passed into user logic to await events and access environment. */
  public final class ProcessContext {
    public Environment env() { return env; }

    /** Await completion of an event; returns its value or throws if failed/interrupted. */
    public Object await(Event e) throws Exception {
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
        Object v = fut.join();
        return v;
      } catch (RuntimeException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof Interrupt) throw (Interrupt) cause;
        if (cause instanceof Exception) throw (Exception) cause;
        throw ex;
      } finally {
        currentWait.compareAndSet(fut, null);
      }
    }
  }

  private final ProcessFunction function;
  private final ProcessContext ctx = new ProcessContext();
  private final AtomicReference<CompletableFuture<Object>> currentWait = new AtomicReference<>();
  private volatile Thread thread;
  private volatile Event target;
  private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

  public Process(Environment env, ProcessFunction function) {
    super(env);
    this.function = function;
    // schedule initialization urgently to start before interrupts
    env.schedule(new Initialize(env, this), URGENT, 0);
  }

  public Event target() { return target; }

  public boolean isAlive() { return value == PENDING; }

  /** Interrupt this process with optional cause. */
  public void interrupt(Object cause) {
    env.schedule(new Interruption(this, cause), URGENT, 0);
  }

  void _start() {
    env.setActiveProcess(this);
    thread = Thread.ofVirtual().name("simpy-process").unstarted(() -> {
      try {
        Object ret = function.run(ctx);
        this.ok = true;
        this.value = ret;
      } catch (Throwable t) {
        this.ok = false;
        this.value = t;
      } finally {
        env.schedule(this, NORMAL, 0);
      }
    });
    thread.start();
    env.setActiveProcess(null);
  }

  void _resume(Event e) {
    // For compatibility with Initialize/Interruption callbacks
    if (e instanceof Initialize) {
      _start();
    } else if (e instanceof Interruption) {
      // remove from target callbacks (if any) by completing current wait exceptionally
      CompletableFuture<Object> wait = currentWait.get();
      if (wait != null) {
        wait.completeExceptionally(new Interrupt(((Interruption) e).cause));
      }
    }
  }

  /** Internal init event to start process. */
  static final class Initialize extends Event {
    private final Process proc;

    Initialize(Environment env, Process proc) {
      super(env);
      this.proc = proc;
      this.callbacks.add(proc::_resume);
      this.ok = true;
      this.value = null;
      env.schedule(this, URGENT, 0);
    }
  }

  /** Internal interruption event. */
  static final class Interruption extends Event {
    private final Object cause;
    private final Process proc;

    Interruption(Process proc, Object cause) {
      super(proc.env);
      this.proc = proc;
      this.cause = cause;
      this.callbacks.add(proc::_resume);
      this.ok = false;
      this.defused = true; // prevent environment crash
      this.value = new Interrupt(cause);
      env.schedule(this, URGENT, 0);
    }
  }
}