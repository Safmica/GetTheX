package com.safmica.model;

public class PlayerEvent {
  private String eventType; // "CONNECTED" or "DISCONNECTED"
  private String username;
  private long timestamp;

  public PlayerEvent(String eventType, String username) {
    this.eventType = eventType;
    this.username = username;
    this.timestamp = System.currentTimeMillis();
  }

  public String getEventType() {
    return eventType;
  }

  public String getUsername() {
    return username;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
