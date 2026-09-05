// src/main/java/com/uteq/SCLI/config/PasswordEnforcerInterceptor.java
package com.uteq.SCLI.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class PasswordEnforcerInterceptor implements HandlerInterceptor {

  private static final Set<String> WHITELIST = Set.of(
      "/login", "/logout", "/cambiar-clave", "/css/", "/js/", "/img/", "/assets/"
  );

  private boolean isWhitelisted(String path) {
    return WHITELIST.stream().anyMatch(path::startsWith);
  }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
    String path = req.getRequestURI();
    if (isWhitelisted(path)) return true;

    HttpSession s = req.getSession(false);
    if (s == null) return true; // que tu AuthInterceptor se encargue

    Object must = s.getAttribute("MUST_CHANGE_PASSWORD");
    if (must instanceof Boolean b && b) {
      res.sendRedirect(req.getContextPath() + "/cambiar-clave");
      return false;
    }
    return true;
  }
}
