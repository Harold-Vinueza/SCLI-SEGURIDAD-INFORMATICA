// src/main/java/com/uteq/SCLI/session/LastSeenFilter.java
package com.uteq.SCLI.session;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LastSeenFilter implements Filter {

  private final SessionTracker tracker;
  public LastSeenFilter(SessionTracker tracker){ this.tracker = tracker; }

  @Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
  HttpServletRequest req = (HttpServletRequest) request;
  HttpSession s = req.getSession(false);

  if (s != null) {
    // opcional: por si no se adjuntó en login (no hace daño si ya estaba)
    tracker.attach(s);
    tracker.touch(s.getId());
  }

  chain.doFilter(request, response);
}
}
