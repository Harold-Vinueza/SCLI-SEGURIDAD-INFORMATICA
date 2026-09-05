package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.exception.CredencialesInvalidasException;
import com.uteq.SCLI.exception.CuentaBloqueadaException;
import com.uteq.SCLI.service.AuthService;
import com.uteq.SCLI.service.PasswordService;
import com.uteq.SCLI.session.SessionTracker;
import com.uteq.SCLI.service.AuditJdbcPort;   
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthService authService;
    

     @Autowired
    private SessionTracker sessionTracker;

     @Autowired private AuditJdbcPort auditPort;
      @Autowired private PasswordService passwordService;

      @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us != null && us.getNombreRol() != null) {
            seedBasicSession(session, us);
            

            // NEW: si aún debe cambiar clave, manténlo en el flujo de cambio
            Object must = session.getAttribute("MUST_CHANGE_PASSWORD");
            if (must instanceof Boolean b && b) {
                return "redirect:/cambiar-clave";
            }

            switch (us.getNombreRol()) {
                case "admin_master":
                case "admin":
                case "administrador":
                case "admin_piso":
                    return "redirect:/dashboard/admin";
                case "docente":
                    return "redirect:/dashboard/docente";
                case "estudiante":
                    return "redirect:/dashboard/estudiante";
                case "coordinador":
                    return "redirect:/dashboard/coordinador";
            }
        }
        return "login/login";
    }

     @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            try {
                // ⬇️  NEW: cierra sesión en BD si la app tiene sessionId
                UserSession us = (UserSession) session.getAttribute("userSession");
                if (us != null && us.getSessionId() != null) {
                    auditPort.endSession(us.getSessionId(), "logout");
                }
            } catch (Exception e) {
                log.warn("No se pudo cerrar la sesión en auditoría: {}", e.getMessage());
            }

            try { sessionTracker.unregister(session.getId()); } catch (Exception ignored) {}
            session.invalidate();
        }
        return "redirect:/login?logout";
    }

    @GetMapping("/whoami")
    public @ResponseBody UserSession whoami(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

     @PostMapping("/login")
    public String procesarLogin(@RequestParam String nombreUsuario,
                                @RequestParam String clave,
                                HttpServletRequest request,
                                HttpSession session) {
        try {
            String ip = resolveClientIp(request);
            String ua = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "";
            log.info("Intento de login user={} ip={}", nombreUsuario, ip);

            // Autenticar
            UserSession sessionInfo = authService.autenticar(
                    nombreUsuario.trim(),
                    clave.trim(),
                    ip,
                    ua
            );

            // Guardar en sesión
            session.setAttribute("userSession", sessionInfo);
            seedBasicSession(session, sessionInfo);

            // Asegurar ID en sesión con alias de compatibilidad
            Integer idUsuario = sessionInfo.getIdUsuario();
            if (idUsuario != null) {
                session.setAttribute("ID_USUARIO", idUsuario); // compat con otros controladores
                session.setAttribute("id_usuario", idUsuario);  // compat legado
                session.setAttribute("idUsuario", idUsuario);   // compat legado
            }

            // Registrar sesión activa y adjuntar HttpSession (para expulsiones remotas, etc.)
            try {
                sessionTracker.register(
                        session.getId(),
                        sessionInfo.getIdUsuario(),
                        sessionInfo.getUsername(),
                        sessionInfo.getNombreRol(),
                        ip, ua
                );
            } catch (Exception e) {
                log.warn("No se pudo registrar sesión activa: {}", e.getMessage());
            }
            sessionTracker.attach(session);

            // (Opcional) fallback para obtener sessionId desde la auditoría si no vino del AuthService
            if (sessionInfo.getSessionId() == null || sessionInfo.getSessionId().isBlank()) {
                try {
                    var r = auditPort.loginAudit(nombreUsuario.trim(), clave.trim(), ip, ua);
                    if (r.ok()) {
                        sessionInfo.setSessionId(r.sessionId());
                    } else {
                        log.warn("fn_login_audit reportó ok=false para {}", nombreUsuario);
                    }
                } catch (Exception e) {
                    log.warn("Fallback fn_login_audit falló: {}", e.getMessage());
                }
            }

            // Forzar cambio de contraseña si corresponde
            boolean mustChange = (idUsuario != null) && passwordService.mustChange(idUsuario);
            session.setAttribute("MUST_CHANGE_PASSWORD", mustChange);
            if (mustChange) {
                return "redirect:/cambiar-clave";
            }

            // Redirigir según rol
            String rol = sessionInfo.getNombreRol();
            return "redirect:" + resolveDashboardByRole(rol);

        /* Antes
        } catch (CredencialesInvalidasException ex) {
            log.warn("Login inválido para user={}", nombreUsuario);
            return "redirect:/login?error=credenciales";
        }*/

        //despues
                } catch (CuentaBloqueadaException ex) {
            log.warn("Login bloqueado para user={}, minutos restantes={}", nombreUsuario, ex.getMinutosRestantes());
            return "redirect:/login?error=bloqueado&minutos=" + ex.getMinutosRestantes();
        } catch (CredencialesInvalidasException ex) {
            log.warn("Login inválido para user={}", nombreUsuario);
            return "redirect:/login?error=credenciales";
        }
    }

     private static String resolveClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        String xr = req.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr.trim();
        return req.getRemoteAddr();
    }

    /**
     * Coloca en sesión los atributos que esperan DocenteProfileController y las vistas.
     * Usa SOLO getters existentes en tu UserSession: idUsuario, idPersona, username, nombreRol.
     */
    private static void seedBasicSession(HttpSession session, UserSession us) {
        // IDs (+ alias por compatibilidad)
        if (session.getAttribute("id_usuario") == null && us.getIdUsuario() != null)
            session.setAttribute("id_usuario", us.getIdUsuario());
        if (session.getAttribute("idUsuario") == null && us.getIdUsuario() != null)
            session.setAttribute("idUsuario", us.getIdUsuario());

        if (session.getAttribute("id_persona") == null && us.getIdPersona() != null)
            session.setAttribute("id_persona", us.getIdPersona());
        if (session.getAttribute("idPersona") == null && us.getIdPersona() != null)
            session.setAttribute("idPersona", us.getIdPersona());

        // username “técnico” (para fallbacks)
        if (session.getAttribute("username") == null && us.getUsername() != null)
            session.setAttribute("username", us.getUsername());

        // nombre que muestra la UI (si no tienes nombres/apellidos, usamos username)
        if (session.getAttribute("nombreUsuario") == null) {
            String display = (us.getUsername() != null && !us.getUsername().isBlank())
                    ? us.getUsername()
                    : "Usuario";
            session.setAttribute("nombreUsuario", display);
        }

        // rol
        if (session.getAttribute("rol") == null && us.getNombreRol() != null)
            session.setAttribute("rol", us.getNombreRol());
    }

    

     private static String resolveDashboardByRole(String rolRaw) {
        String rol = rolRaw == null ? "" : rolRaw.trim().toLowerCase();
        switch (rol) {
            case "admin_master":
            case "admin":
            case "administrador":
            case "admin_piso":
                return "/dashboard/admin";
            case "docente":
                return "/dashboard/docente";
            case "estudiante":
                return "/dashboard/estudiante";
            case "coordinador":
                return "/dashboard/coordinador";
            default:
                return "/login?error=rol";
        }
    }

    // === Endpoint client-info: guarda JSON y campos derivados ===
@PostMapping(path = "/client-info", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public String registrarClientInfo(@RequestBody java.util.Map<String, Object> body,
                                  HttpSession session,
                                  HttpServletRequest request) {
    try {
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us == null) return "{\"ok\":false,\"msg\":\"no-session\"}";

        String ipPublic = body.get("ipPublic") != null ? String.valueOf(body.get("ipPublic")) : null;
        String ipLocal  = body.get("ipLocal")  != null ? String.valueOf(body.get("ipLocal"))  : null;
        String ua       = body.get("ua")       != null ? String.valueOf(body.get("ua"))       : "";

        // Preferir lo detectado en cliente
        String browser     = body.get("browser")     != null ? String.valueOf(body.get("browser"))     : null;
        String browserVer  = body.get("browserVer")  != null ? String.valueOf(body.get("browserVer"))  : null;
        String platform    = body.get("platform")    != null ? String.valueOf(body.get("platform"))    : null;
        String platformVer = body.get("platformVer") != null ? String.valueOf(body.get("platformVer")) : null;
        String arch        = body.get("arch")        != null ? String.valueOf(body.get("arch"))        : null;
        String deviceModel = body.get("deviceModel") != null ? String.valueOf(body.get("deviceModel")) : null;
        String deviceType  = body.get("deviceType")  != null ? String.valueOf(body.get("deviceType"))  : null;

        // Hints crudos → JSONB
        Object hintsObj = body.get("uaHints");
        String hintsJson = "";
        if (hintsObj != null) {
            hintsJson = new ObjectMapper().writeValueAsString(hintsObj);
        }

        // Fallbacks desde uaHints si falta algo
        if (hintsObj != null) {
            try {
                java.util.Map<?,?> hm = (java.util.Map<?,?>) hintsObj;
                if (platform    == null) platform    = str(hm.get("platform"));
                if (platformVer == null) platformVer = str(hm.get("platformVersion"));
                if (arch        == null) arch        = str(hm.get("architecture"));
                if (deviceModel == null) deviceModel = str(hm.get("model"));

                if (browser == null || browserVer == null) {
                    Object fvl = hm.get("fullVersionList");
                    if (fvl instanceof java.util.List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof java.util.Map<?,?> m) {
                            if (browser    == null) browser    = str(m.get("brand"));
                            if (browserVer == null) browserVer = str(m.get("version"));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // ✅ Fallback por UA clásico (Firefox / Safari, etc.)
        if (browser == null || browser.isBlank()) {
            BrowserInfo bi = parseUaClassic(ua);
            browser    = bi.browser;
            browserVer = bi.version;
        }

        // Normaliza a nombres canónicos
        browser = normalizeBrowser(browser);

        // Tipo de dispositivo si aún no está
        if (deviceType == null) {
            if (platform != null && platform.toLowerCase().contains("android")) deviceType = "mobile";
            else if (platform != null && platform.toLowerCase().contains("ios")) deviceType = "mobile";
            else deviceType = "desktop";
        }

        // UA enriquecido
        String uaFull = hintsJson.isBlank() ? ua : (ua + " | hints=" + hintsJson);

        // Guarda en sesión (para vistas/trigger)
        if (ipPublic != null && !ipPublic.isBlank()) session.setAttribute("clientIpPublic", ipPublic);
        if (ipLocal  != null && !ipLocal.isBlank())  session.setAttribute("clientIpLocal",  ipLocal);
        session.setAttribute("uaFull", uaFull);
        session.setAttribute("uaHintsJson", hintsJson);
        session.setAttribute("uaBrowser", browser);
        session.setAttribute("uaBrowserVer", browserVer);
        session.setAttribute("uaPlatform", platform);
        session.setAttribute("uaPlatformVer", platformVer);
        session.setAttribute("uaArch", arch);
        session.setAttribute("uaDeviceModel", deviceModel);
        session.setAttribute("uaDeviceType", deviceType);

        // Si no vino ipPublic, dedúcela del request
        if (ipPublic == null || ipPublic.isBlank()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) ipPublic = xff.split(",")[0].trim();
            else {
                String xr = request.getHeader("X-Real-IP");
                ipPublic = (xr != null && !xr.isBlank()) ? xr.trim() : request.getRemoteAddr();
            }
        }

        // Dispara heartbeat con todo (BD)
        auditPort.heartbeatDetailed(
                us, ipPublic, ipLocal, uaFull,
                browser, browserVer, platform, platformVer, arch, deviceModel, deviceType,
                hintsJson
        );

        // ⬅⬅⬅ Sincronizar el TRACKER con los datos enriquecidos
        try {
            String ipForTracker = (ipPublic != null && !ipPublic.isBlank())
                    ? ipPublic
                    : resolveClientIp(request);
            sessionTracker.updateIp(session.getId(), ipForTracker);
            sessionTracker.updateUa(session.getId(), browser, browserVer, uaFull);
        } catch (Exception ignore) {}

        return "{\"ok\":true}";
    } catch (Exception e) {
        return "{\"ok\":false,\"error\":\"" + e.getMessage().replace("\"","'") + "\"}";
    }
}


  // ===== Helpers =====
private static String str(Object o){ return o == null ? null : String.valueOf(o); }

private static String normalizeBrowser(String brand){
    if (brand == null) return null;
    String b = brand.toLowerCase();
    if (b.contains("edge") || b.contains("edg")) return "Microsoft Edge";
    if (b.contains("firefox"))  return "Firefox";
    if (b.contains("chromium")) return "Chromium";
    if (b.contains("chrome"))   return "Chrome";
    if (b.contains("safari"))   return "Safari";
    if (b.contains("opera") || b.contains("opr")) return "Opera";
    if (b.contains("brave"))    return "Brave";
    return brand;
}

private static class BrowserInfo {
    String browser; String version;
    BrowserInfo(String b, String v){ this.browser=b; this.version=v; }
}

private static BrowserInfo parseUaClassic(String ua){
    if (ua == null) ua = "";

    java.util.regex.Matcher m;

    // Edge (Chromium)
    m = java.util.regex.Pattern.compile("EdgA?/([\\d.]+)").matcher(ua);
    if (m.find()) return new BrowserInfo("Microsoft Edge", m.group(1));

    // Opera
    m = java.util.regex.Pattern.compile("OPR/([\\d.]+)").matcher(ua);
    if (m.find()) return new BrowserInfo("Opera", m.group(1));

    // Chrome (evita confundir con Chromium)
    m = java.util.regex.Pattern.compile("Chrome/([\\d.]+)").matcher(ua);
    if (m.find() && !ua.contains("Chromium")) return new BrowserInfo("Chrome", m.group(1));

    // Chromium
    m = java.util.regex.Pattern.compile("Chromium/([\\d.]+)").matcher(ua);
    if (m.find()) return new BrowserInfo("Chromium", m.group(1));

    // Firefox
    m = java.util.regex.Pattern.compile("Firefox/([\\d.]+)").matcher(ua);
    if (m.find()) return new BrowserInfo("Firefox", m.group(1));

    // Safari (Version/x.y)
    if (ua.contains("Safari") && !ua.contains("Chrome")) {
        m = java.util.regex.Pattern.compile("Version/([\\d.]+)").matcher(ua);
        return new BrowserInfo("Safari", m.find()? m.group(1) : null);
    }

    return new BrowserInfo(null, null);
}  



}



/*package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.exception.CredencialesInvalidasException;
import com.uteq.SCLI.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        // Si ya hay sesión, manda directo a su dashboard
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us != null && us.getNombreRol() != null) {
            switch (us.getNombreRol()) {
                case "admin_master":
                case "admin":
                case "administrador":
                    return "redirect:/dashboard/admin";
                case "admin_piso":
                    return "redirect:/dashboard/admin";
                case "docente":
                    return "redirect:/dashboard/docente";
                case "estudiante":
                    return "redirect:/dashboard/estudiante";
            }
        }
        return "login/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
    if (session != null) session.invalidate();
    return "redirect:/login?logout";
}

    @GetMapping("/whoami")
    public @ResponseBody UserSession whoami(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    @PostMapping("/login")
public String procesarLogin(@RequestParam String nombreUsuario,
                            @RequestParam String clave,
                            HttpSession session) {
    try {
        UserSession sessionInfo = authService.autenticar(nombreUsuario.trim(), clave.trim());

        session.setAttribute("userSession", sessionInfo);

        // 👇 Añade estas dos líneas:
      session.setAttribute("nombreUsuario", sessionInfo.getUsername());
        session.setAttribute("rol", sessionInfo.getNombreRol());

        String rol = sessionInfo.getNombreRol();
        switch (rol == null ? "" : rol.trim().toLowerCase()) {
            case "admin_master":
            case "admin":
            case "administrador":
                return "redirect:/dashboard/admin";
            case "admin_piso":
                return "redirect:/dashboard/admin";
            case "docente":
                return "redirect:/dashboard/docente";
            case "estudiante":
                return "redirect:/dashboard/estudiante";
            default:
                return "redirect:/login?error=rol";
        }
    } catch (CredencialesInvalidasException ex) {
        return "redirect:/login?error=credenciales";
    }
}
}*/
