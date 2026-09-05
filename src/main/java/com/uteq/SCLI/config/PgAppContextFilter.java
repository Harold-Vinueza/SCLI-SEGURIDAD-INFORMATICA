package com.uteq.SCLI.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.repository.DocenteRepository;

@Component
@RequiredArgsConstructor
public class PgAppContextFilter implements Filter {

  private final JdbcTemplate jdbc;
  private final UserSession userSession;
  private final DocenteRepository docenteRepo;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
          throws java.io.IOException, ServletException {

    try {
      // === (A) Setear GUCs de IP/UA para que los triggers tengan datos correctos ===
      String ip = null, ua = "";
      try {
        HttpServletRequest http = (HttpServletRequest) req;

        // 0) UA base del request
        ua = http.getHeader("User-Agent") != null ? http.getHeader("User-Agent") : "";

        // 1) PREFERIR lo que ya envió el cliente y está en sesión (lo puso /client-info)
        HttpSession httpSession = http.getSession(false);
        if (httpSession != null) {
          Object ipPub  = httpSession.getAttribute("clientIpPublic");
          Object uaFull = httpSession.getAttribute("uaFull");
          if (ipPub != null && !String.valueOf(ipPub).isBlank())  ip = String.valueOf(ipPub);
          if (uaFull != null && !String.valueOf(uaFull).isBlank()) ua = String.valueOf(uaFull);
        }

        // 2) Si no hay IP en sesión, usa cabeceras de proxy/CDN o la remota
        if (ip == null || ip.isBlank()) {
          String cf = http.getHeader("CF-Connecting-IP");
          if (cf != null && !cf.isBlank()) {
            ip = first(cf);
          } else {
            String xff = http.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
              ip = first(xff);
            } else {
              String xr = http.getHeader("X-Real-IP");
              ip = (xr != null && !xr.isBlank()) ? xr.trim() : http.getRemoteAddr();
            }
          }
        }
      } catch (Exception ignored) {}

      if (ip != null) {
        try { jdbc.update("select set_config('app.current_ip', ?, true)", ip); } catch (Exception ignored) {}
      }
      try { jdbc.update("select set_config('app.current_user_agent', ?, true)", ua); } catch (Exception ignored) {}

      // === (B) Tu lógica existente para current_docente_id (se mantiene) ===
      try {
        Integer idPersona = userSession.getIdPersona();
        if (idPersona != null && "docente".equalsIgnoreCase(userSession.getNombreRol())) {
          Integer idDocente = docenteRepo.findIdByPersona(idPersona);
          if (idDocente != null) {
            jdbc.execute("select set_config('app.current_docente_id', '" + idDocente + "', true)");
          } else {
            // limpia si no hay mapeo
            jdbc.execute("select set_config('app.current_docente_id', '', true)");
          }
        } else {
          // no-docente: limpia
          jdbc.execute("select set_config('app.current_docente_id', '', true)");
        }
      } catch (Exception ignored) {}

    } catch (Exception e) {
      // no rompas la request por esto
    }

    chain.doFilter(req, res);
  }

  private String first(String list){
    int i = list.indexOf(',');
    return (i > 0) ? list.substring(0, i).trim() : list.trim();
  }
}