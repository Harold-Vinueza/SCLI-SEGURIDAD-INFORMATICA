// src/main/java/com/uteq/SCLI/session/SessionTracker.java
package com.uteq.SCLI.session;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.AuditJdbcPort;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTracker implements HttpSessionListener {

  private final Map<String, ActiveSession> sessions = new ConcurrentHashMap<>();
  private final Map<String, HttpSession>   httpSessions = new ConcurrentHashMap<>();
  // HttpSessionID -> UUID real en audit.sesion
  private final Map<String, String>        httpToAudit = new ConcurrentHashMap<>();

  private final AuditJdbcPort auditPort;

  public SessionTracker(AuditJdbcPort auditPort) {
    this.auditPort = auditPort;
  }

  // ===== Registro/attach/touch =====
  public void register(String sessionId, Integer userId, String username, String rol,
                       String ip, String userAgent) {
    sessions.put(sessionId, new ActiveSession(sessionId, userId, username, rol, ip, userAgent));
  }

  public void attach(HttpSession httpSession) {
    if (httpSession != null) {
      httpSessions.put(httpSession.getId(), httpSession);
      try {
        Object usObj = httpSession.getAttribute("userSession");
        if (usObj instanceof UserSession us && us.getSessionId() != null) {
          httpToAudit.put(httpSession.getId(), us.getSessionId()); // Http -> UUID audit
        }
      } catch (Throwable ignored) {}
    }
  }

  public void touch(String sessionId) {
    ActiveSession s = sessions.get(sessionId);
    if (s != null) {
      try { s.touch(); }
      catch (Throwable ignored) {
        sessions.put(sessionId, rebuild(s, getUserId(s), getUsername(s), getRol(s), getIp(s), getUserAgent(s)));
      }
    }
  }

  // ===== Actualizaciones en memoria =====
  public void updateIp(String sessionId, String ip) {
    if (sessionId == null || ip == null || ip.isBlank()) return;
    ActiveSession s = sessions.get(sessionId);
    if (s == null) return;
    ActiveSession updated = rebuild(s, getUserId(s), getUsername(s), getRol(s), ip, getUserAgent(s));
    sessions.put(sessionId, updated);
    safeTouch(updated);
  }

  public void updateUa(String sessionId, String browser, String browserVer, String userAgent) {
    if (sessionId == null) return;
    ActiveSession s = sessions.get(sessionId);
    if (s == null) return;
    String ua = (userAgent != null && !userAgent.isBlank())
            ? userAgent
            : buildUaFrom(browser, browserVer, getUserAgent(s));
    ActiveSession updated = rebuild(s, getUserId(s), getUsername(s), getRol(s), getIp(s), ua);
    sessions.put(sessionId, updated);
    safeTouch(updated);
  }

  public void unregister(String sessionId) {
    sessions.remove(sessionId);
    httpSessions.remove(sessionId);
    httpToAudit.remove(sessionId);
  }

  public Collection<ActiveSession> listAll() { return sessions.values(); }

  // ===== Util público para el controller =====
  public String resolveAuditUuid(String httpSessionId) {
    return httpToAudit.get(httpSessionId);
  }

  // ===== EXPULSAR blindado =====
  /**
   * Expulsa una sesión ajena sin tocar la del admin.
   *
   * @param targetHttpSessionId HttpSessionID de la fila a expulsar
   * @param currentAdminHttpId  HttpSessionID del admin (para NO cerrarla)
   * @param knownAuditUuid      UUID de auditoría ya resuelto en el controller (si viene null, se intenta resolver)
   */
  public void kick(String targetHttpSessionId, String currentAdminHttpId, String knownAuditUuid) {
    try {
      // 0) Proteger la sesión del admin
      if (targetHttpSessionId != null && targetHttpSessionId.equals(currentAdminHttpId)) {
        selfKick(targetHttpSessionId);
        return;
      }

      // 1) Resolver HttpSession objetivo
      HttpSession hs = httpSessions.remove(targetHttpSessionId);

      // 2) Resolver UUID de auditoría
      String auditUuid = (knownAuditUuid != null && !knownAuditUuid.isBlank())
              ? knownAuditUuid
              : httpToAudit.remove(targetHttpSessionId);

      if (auditUuid == null && hs != null) {
        Object usObj = hs.getAttribute("userSession");
        if (usObj instanceof UserSession us && us.getSessionId() != null) {
          auditUuid = us.getSessionId();
        }
      }

      // 3) Marcar FIN solo para esa sesión de auditoría
      if (auditUuid != null) {
        try { auditPort.endSession(auditUuid, "kick"); } catch (Exception ignored) {}
      }

      // 4) Invalidar SOLO la HttpSession destino
      if (hs != null) {
        try { hs.invalidate(); } catch (IllegalStateException ignored) {}
      }
    } finally {
      sessions.remove(targetHttpSessionId);
    }
  }

  private void selfKick(String httpSessionId) {
    try {
      HttpSession hs = httpSessions.remove(httpSessionId);
      String auditUuid = httpToAudit.remove(httpSessionId);
      if (auditUuid != null) {
        try { auditPort.endSession(auditUuid, "kick-self"); } catch (Exception ignored) {}
      }
      if (hs != null) {
        try { hs.invalidate(); } catch (IllegalStateException ignored) {}
      }
    } finally {
      sessions.remove(httpSessionId);
    }
  }

  // ===== Listener expiración =====
  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    String id = se.getSession().getId();
    try {
      String auditUuid = httpToAudit.remove(id);
      if (auditUuid == null) {
        Object usObj = se.getSession().getAttribute("userSession");
        if (usObj instanceof UserSession us && us.getSessionId() != null) {
          auditUuid = us.getSessionId();
        }
      }
      if (auditUuid != null) {
        try { auditPort.endSession(auditUuid, "expired"); } catch (Exception ignored) {}
      }
    } catch (Exception ignored) {}

    sessions.remove(id);
    httpSessions.remove(id);
  }

  // ===== Helpers internos =====
  private ActiveSession rebuild(ActiveSession old,
                                Integer userId, String username, String rol,
                                String ip, String userAgent) {
    return new ActiveSession(old.getSessionId(), userId, username, rol, ip, userAgent);
  }

  private static String buildUaFrom(String browser, String ver, String fallback) {
    String b = (browser == null ? "" : browser.trim());
    String v = (ver == null ? "" : ver.trim());
    if (!b.isEmpty() && !v.isEmpty()) return b + "/" + v;
    if (!b.isEmpty()) return b;
    return (fallback == null || fallback.isBlank()) ? "-" : fallback;
  }

  private void safeTouch(ActiveSession s) { try { s.touch(); } catch (Throwable ignored) {} }

  private static Integer getUserId(ActiveSession s) {
    try { return (Integer) call(s, "getUserId"); } catch (Throwable ignored) { return null; }
  }
  private static String getUsername(ActiveSession s) {
    try { return (String) call(s, "getUsername"); } catch (Throwable ignored) { return null; }
  }
  private static String getRol(ActiveSession s) {
    try { return (String) call(s, "getRol"); } catch (Throwable ignored) { return null; }
  }
  private static String getIp(ActiveSession s) {
    try { return (String) call(s, "getIp"); } catch (Throwable ignored) { return null; }
  }
  private static String getUserAgent(ActiveSession s) {
    try { return (String) call(s, "getUserAgent"); } catch (Throwable ignored) { return null; }
  }
  private static Object call(Object target, String method) throws Exception {
    java.lang.reflect.Method m = target.getClass().getMethod(method);
    return m.invoke(target);
  }
}