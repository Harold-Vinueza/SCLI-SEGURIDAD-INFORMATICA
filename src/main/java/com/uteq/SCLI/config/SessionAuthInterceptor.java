package com.uteq.SCLI.config;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.AuditJdbcPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.uteq.SCLI.exception.AccesoNoAutorizadoException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    private final AuditJdbcPort auditPort;
    
    // Roles permitidos por prefijo de URL. Se evalúa en orden; el primer prefijo que
    // coincida con la ruta define qué roles pueden pasar.
    private static final List<Map.Entry<String, Set<String>>> REGLAS_RBAC = List.of(
        Map.entry("/dashboard/admin", Set.of("admin_master", "admin_piso", "admin", "administrador")),
        Map.entry("/admin/",          Set.of("admin_master", "admin_piso", "admin", "administrador")),
        Map.entry("/api/admin/",      Set.of("admin_master", "admin_piso", "admin", "administrador")),
        Map.entry("/dashboard/coordinador", Set.of("coordinador")),
        Map.entry("/api/coordinador",       Set.of("coordinador")),
        Map.entry("/dashboard/docente", Set.of("docente")),
        Map.entry("/docente/",          Set.of("docente")),
        Map.entry("/api/docentes",      Set.of("docente")),
        Map.entry("/dashboard/estudiante", Set.of("estudiante")),
        Map.entry("/estudiante",          Set.of("estudiante"))
    );

    public SessionAuthInterceptor(AuditJdbcPort auditPort) {
        this.auditPort = auditPort;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI();

        if (path.startsWith("/login")
                || path.equals("/logout")
                || path.startsWith("/error")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/")
                || path.startsWith("/images/")
                || path.startsWith("/assets/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.equals("/LABU.png")) {
            return true;
        }

        HttpSession session = req.getSession(false);
        UserSession us = (session == null) ? null : (UserSession) session.getAttribute("userSession");

                if (us == null) {
            res.sendRedirect("/login?expired");
            return false;
        }

                String rolActual = us.getNombreRol() == null ? "" : us.getNombreRol().trim().toLowerCase();
        for (Map.Entry<String, Set<String>> regla : REGLAS_RBAC) {
            if (path.startsWith(regla.getKey()) && !regla.getValue().contains(rolActual)) {
                String motivo = "Rol '" + rolActual + "' sin permiso para prefijo '" + regla.getKey() + "'";
                auditPort.registrarAccesoDenegado(
                    us.getIdUsuario(), us.getUsername(), us.getNombreRol(), path, req.getRemoteAddr(), motivo);
                throw new AccesoNoAutorizadoException(
                    "Tu rol (" + rolActual + ") no tiene permiso para acceder a esta sección.");
            }
        }

        try {
            // Leer lo que el front pudo haber guardado en sesión:
            String ipPublic   = s(session.getAttribute("clientIpPublic"));
            String ipLocal    = s(session.getAttribute("clientIpLocal"));
            String uaFull     = s(session.getAttribute("uaFull"));
            String hintsJson  = s(session.getAttribute("uaHintsJson"));
            String browser    = s(session.getAttribute("uaBrowser"));
            String browserVer = s(session.getAttribute("uaBrowserVer"));
            String platform   = s(session.getAttribute("uaPlatform"));
            String platformVer= s(session.getAttribute("uaPlatformVer"));
            String arch       = s(session.getAttribute("uaArch"));
            String deviceModel= s(session.getAttribute("uaDeviceModel"));
            String deviceType = s(session.getAttribute("uaDeviceType"));

            // Fallbacks si aún no llegó client-info:
            if (uaFull == null || uaFull.isBlank()) uaFull = req.getHeader("User-Agent");
            if (ipPublic == null || ipPublic.isBlank()) {
                String xff = req.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) ipPublic = xff.split(",")[0].trim();
                else {
                    String xr = req.getHeader("X-Real-IP");
                    ipPublic = (xr != null && !xr.isBlank()) ? xr.trim() : req.getRemoteAddr();
                }
            }

            // Heartbeat completo
            auditPort.heartbeatDetailed(
                    us, ipPublic, ipLocal, uaFull,
                    browser, browserVer, platform, platformVer, arch, deviceModel, deviceType,
                    hintsJson
            );
        } catch (Exception ignored) {}

        return true;
    }

    private static String s(Object o){ return o == null ? null : String.valueOf(o); }
}