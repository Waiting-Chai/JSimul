package com.jsimul.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Value wrapper for Condition events providing event->value mapping.
 * 
 * @author waiting
 * @date 2025/10/29
 */
public class ConditionValue {
  private final Map<Event, Object> values = new LinkedHashMap<>();

  void add(Event e) { values.put(e, e.value()); }

  public Map<Event, Object> toMap() { return values; }

  public boolean contains(Event e) { return values.containsKey(e); }

  public Object get(Event e) { return values.get(e); }
}