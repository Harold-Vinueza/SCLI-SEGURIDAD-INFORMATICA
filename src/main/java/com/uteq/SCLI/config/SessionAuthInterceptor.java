package com.uteq.SCLI.config;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.AuditJdbcPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    private final AuditJdbcPort auditPort;

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