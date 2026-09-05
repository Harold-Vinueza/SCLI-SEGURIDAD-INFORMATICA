package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.StudentProfileDTO;
import com.uteq.SCLI.service.StudentProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import com.uteq.SCLI.service.ReservaEspecialService;


@Controller
@RequestMapping("/dashboard/estudiante")
public class EstudianteProfileController {

    private static final Logger log = LoggerFactory.getLogger(EstudianteProfileController.class);

    private final StudentProfileService svc;
    private final JdbcTemplate jdbc;

    private final ReservaEspecialService reservaEspecialService;

    public EstudianteProfileController(StudentProfileService svc,
                                   JdbcTemplate jdbc,
                                   ReservaEspecialService reservaEspecialService) { // << NUEVO
    this.svc = svc;
    this.jdbc = jdbc;
    this.reservaEspecialService = reservaEspecialService; // << NUEVO
}

    /* ===================== Helpers de sesión ===================== */

    private Integer safeIdPersona(HttpSession s){
        Object v = (s!=null)? s.getAttribute("id_persona") : null;
        if (v == null && s!=null) v = s.getAttribute("idPersona"); // alias posible
        return (v instanceof Integer)? (Integer)v : null;
    }

    private Integer safeIdUsuario(HttpSession s){
        Object v = (s!=null)? s.getAttribute("id_usuario") : null;
        if (v == null && s!=null) v = s.getAttribute("idUsuario");
        return (v instanceof Integer)? (Integer)v : null;
    }

    private String currentUsername(HttpServletRequest req){
        try { return req.getUserPrincipal()!=null ? req.getUserPrincipal().getName() : null; }
        catch(Exception e){ return null; }
    }

    private String guessUsername(HttpSession s, HttpServletRequest req){
        Object u = (s!=null)? s.getAttribute("username") : null;
        if (u == null && s!=null) u = s.getAttribute("usuario");
        if (u == null && s!=null) u = s.getAttribute("login");
        if (u == null && s!=null) u = s.getAttribute("user");
        if (u == null) u = currentUsername(req);
        return u!=null ? String.valueOf(u) : null;
    }

    private Integer findIdUsuarioByUsername(String username){
        if (username == null || username.isBlank()) return null;
        final String sql = "SELECT id_usuario FROM app.app_usuario WHERE lower(username)=lower(?)";
        try { return jdbc.query(sql, rs -> rs.next()? (Integer) rs.getObject(1) : null, username); }
        catch(Exception e){ log.debug("findIdUsuarioByUsername: {}", e.getMessage()); return null; }
    }

    private Integer findIdPersonaByUsername(String username){
        if (username == null || username.isBlank()) return null;
        final String sql = "SELECT id_persona FROM app.app_usuario WHERE lower(username)=lower(?)";
        try { return jdbc.query(sql, rs -> rs.next()? (Integer) rs.getObject(1) : null, username); }
        catch(Exception e){ return null; }
    }

    private Integer findIdUsuarioByPersona(Integer idPersona){
        if (idPersona == null) return null;
        final String sql = "SELECT id_usuario FROM app.app_usuario WHERE id_persona = ?";
        try { return jdbc.query(sql, rs -> rs.next()? (Integer) rs.getObject(1) : null, idPersona); }
        catch(Exception e){ return null; }
    }

    private void ensureIds(HttpSession session, HttpServletRequest req){
        Integer idPer = safeIdPersona(session);
        Integer idUsu = safeIdUsuario(session);

        if (idPer != null && idUsu != null) return;

        if (idUsu == null && idPer != null){
            idUsu = findIdUsuarioByPersona(idPer);
            if (idUsu != null) session.setAttribute("id_usuario", idUsu);
        }

        if (idUsu == null || idPer == null){
            String user = guessUsername(session, req);
            if (user != null){
                if (idUsu == null){
                    idUsu = findIdUsuarioByUsername(user);
                    if (idUsu != null) session.setAttribute("id_usuario", idUsu);
                }
                if (idPer == null){
                    Integer tmp = findIdPersonaByUsername(user);
                    if (tmp != null) session.setAttribute("id_persona", tmp);
                }
            }
        }
    }

    /* ============================ Rutas ============================ */

    @GetMapping("/perfil")
    public String viewPerfil(Model model, HttpSession session, HttpServletRequest req){
        ensureIds(session, req);
        Integer idPer = safeIdPersona(session);
        if (idPer == null) {
            model.addAttribute("error", "La sesión no contiene id_persona. Vuelve a iniciar sesión.");
            return "redirect:/login";
        }
        StudentProfileDTO dto = svc.get(idPer);
        model.addAttribute("perfil", dto);
        return "dashboard/estudiante/estudiante-perfil";
    }

    @PostMapping("/perfil")
    public String savePerfil(@ModelAttribute("perfil") StudentProfileDTO dto,
                             @RequestParam(value="foto", required=false) MultipartFile foto,
                             Model model, HttpSession session, HttpServletRequest req){
        ensureIds(session, req);
        Integer idPer = safeIdPersona(session);
        if (idPer == null) {
            model.addAttribute("error", "La sesión expiró. Vuelve a iniciar sesión.");
            return "redirect:/login";
        }

        dto.setIdPersona(idPer);

        try {
            // El service ya maneja internamente la mayoría de errores no críticos
            svc.save(dto, foto);
            model.addAttribute("ok", "Perfil actualizado.");
        }
        // LÍMITES DE SUBIDA
        catch (MaxUploadSizeExceededException ex) {
            model.addAttribute("error", "La imagen es demasiado grande. Reduce el tamaño o comprímela.");
        }
        // FORMATO INVÁLIDO (del UploadService)
        catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        // RESTRICCIONES DE BD (únicos, FK, etc.)
        catch (DataIntegrityViolationException ex) {
            String msg = (ex.getMessage() != null) ? ex.getMessage().toLowerCase() : "";
            if (msg.contains("estudiante_matricula_key")) {
                model.addAttribute("error", "La matrícula ya existe. Usa otra diferente.");
            } else if (msg.contains("persona_correo_key")) {
                model.addAttribute("error", "Ese correo ya está en uso.");
            } else {
                model.addAttribute("error", "No se pudo guardar por restricción de datos.");
            }
            log.warn("DataIntegrityViolation al guardar perfil estudiante: {}", ex.getMessage());
        }
        // CUALQUIER OTRA EXCEPCIÓN CRÍTICA
        catch (Exception ex) {
            log.error("Error inesperado al guardar perfil estudiante", ex);
            model.addAttribute("error", "Error inesperado al guardar el perfil.");
        }

        // Refrescar el DTO para mostrar lo que quedó persistido
        model.addAttribute("perfil", svc.get(idPer));
        return "dashboard/estudiante/estudiante-perfil";
    }

    @GetMapping("/cambio-clave")
    public String viewClave(){
        return "dashboard/estudiante/estudiante-cambio-clave";
    }

    @PostMapping("/cambio-clave")
    public String changeClave(@RequestParam String actual,
                              @RequestParam String nueva,
                              @RequestParam String repetir,
                              Model model, HttpSession session, HttpServletRequest req){
        ensureIds(session, req);
        Integer idUsu = safeIdUsuario(session);
        if (idUsu == null) {
            model.addAttribute("error", "La sesión no contiene id_usuario. Vuelve a iniciar sesión.");
            return "dashboard/estudiante/estudiante-cambio-clave";
        }

        if (!nueva.equals(repetir)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "dashboard/estudiante/estudiante-cambio-clave";
        }
        if (actual.equals(nueva)) {
            model.addAttribute("error", "La nueva clave no debe ser igual a la anterior.");
            return "dashboard/estudiante/estudiante-cambio-clave";
        }
        if (!nueva.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            model.addAttribute("error", "La nueva clave no cumple la política.");
            return "dashboard/estudiante/estudiante-cambio-clave";
        }

        var res = svc.cambiarClave(idUsu, actual, nueva);
        if (res.ok()) model.addAttribute("ok", res.msg());
        else          model.addAttribute("error", res.msg());

        return "dashboard/estudiante/estudiante-cambio-clave";
    }


    // Panel estudiante (ruta base): carga últimos 5 avisos publicados


// Página del menú: “Anuncios” -> lista completa
@GetMapping("/anuncios")
public String anuncios(Model model, HttpSession session, HttpServletRequest req){
    // ensureIds(session, req);
    model.addAttribute("avisosAll", reservaEspecialService.publicadas());
    return "dashboard/estudiante-anuncios"; // ver archivo del paso 3
}
}