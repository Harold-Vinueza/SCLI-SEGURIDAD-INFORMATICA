// src/main/java/com/uteq/SCLI/session/ActiveSession.java
package com.uteq.SCLI.session;

import java.io.Serializable;
import java.time.Instant;

public class ActiveSession implements Serializable {
  private final String sessionId;
  private final Integer userId;         // ajusta al tipo de tu PK
  private final String username;        // tu campo nombreUsuario
  private final String rol;             // si lo tienes
  private final String ip;
  private final String userAgent;
  private final Instant loginAt;
  private volatile Instant lastSeen;

  public ActiveSession(String sessionId, Integer userId, String username, String rol, String ip, String userAgent) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.username = username;
    this.rol = rol;
    this.ip = ip;
    this.userAgent = userAgent;
    this.loginAt = Instant.now();
    this.lastSeen = this.loginAt;
  }

  public String getSessionId() { return sessionId; }
  public Integer getUserId() { return userId; }
  public String getUsername() { return username; }
  public String getRol() { return rol; }
  public String getIp() { return ip; }
  public String getUserAgent() { return userAgent; }
  public Instant getLoginAt() { return loginAt; }
  public Instant getLastSeen() { return lastSeen; }
  public void touch() { this.lastSeen = Instant.now(); }
}
