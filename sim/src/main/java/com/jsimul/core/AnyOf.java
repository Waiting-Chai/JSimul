package com.jsimul.core;

import java.util.Arrays;
import java.util.List;

/**
 * AnyOf condition: succeeds when any event is processed successfully.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class AnyOf extends Condition {
  public AnyOf(Environment env, List<Event> events) {
    super(env, Condition::anyEvents, events);
  }

  public AnyOf(Environment env, Event... events) {
    this(env, Arrays.asList(events));
  }
}