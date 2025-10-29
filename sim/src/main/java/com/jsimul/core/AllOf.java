package com.jsimul.core;

import java.util.Arrays;
import java.util.List;

/**
 * AllOf condition: succeeds when all events are processed successfully.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class AllOf extends Condition {
  public AllOf(Environment env, List<Event> events) {
    super(env, Condition::allEvents, events);
  }

  public AllOf(Environment env, Event... events) {
    this(env, Arrays.asList(events));
  }
}