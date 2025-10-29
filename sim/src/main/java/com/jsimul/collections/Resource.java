package com.jsimul.collections;

import com.jsimul.core.Environment;
import com.jsimul.core.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple capacity-based Resource supporting request/release semantics.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class Resource extends BaseResource {
  public Resource(Environment env, int capacity) {
    super(env, capacity);
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
  }

  public final List<Event> users = new ArrayList<>();

  public int count() { return users.size(); }

  public Event request() { return new Request(this); }

  public Event release(Request req) { return new Release(this, req); }

  @Override
  protected boolean doPut(Event event) {
    Request req = (Request) event;
    if (users.size() < capacity) {
      users.add(req);
      req.succeed(null);
    }
    return true;
  }

  @Override
  protected boolean doGet(Event event) {
    Release rel = (Release) event;
    users.remove(rel.request);
    rel.succeed(null);
    return true;
  }

  /** Request event. */
  public static class Request extends BaseResource.Put {
    public Request(Resource resource) { super(resource); }
  }

  /** Release event. */
  public static class Release extends BaseResource.Get {
    final Request request;
    public Release(Resource resource, Request request) {
      super(resource);
      this.request = request;
    }
  }
}