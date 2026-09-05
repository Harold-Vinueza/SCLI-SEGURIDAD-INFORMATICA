package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.ProfileDTO;
import com.uteq.SCLI.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/dashboard/coordinador")
public class CoordinadorProfileController {

  private static final Logger log = LoggerFactory.getLogger(CoordinadorProfileController.class);

  private final ProfileService svc;
  private final JdbcTemplate jdbc;

  public CoordinadorProfileController(ProfileService svc, JdbcTemplate jdbc) {
    this.svc = svc;
    this.jdbc = jdbc;
  }

  /* ==== helpers sesión (igual que antes) ==== */
  private Integer safeIdPersona(HttpSession s){
    Object v = (s!=null)? s.getAttribute("id_persona") : null;
    if (v == null) v = (s!=null)? s.getAttribute("idPersona") : null;
    return (v instanceof Integer)? (Integer)v : null;
  }
  private Integer safeIdUsuario(HttpSession s){
    Object v = (s!=null)? s.getAttribute("id_usuario") : null;
    if (v == null) v = (s!=null)? s.getAttribute("idUsuario") : null;
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
    catch(Exception e){ return null; }
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

  /* ======== Rutas SIN el "/" base ======== */

  @GetMapping("/perfil")
  public String viewPerfil(Model model, HttpSession session, HttpServletRequest req){
    ensureIds(session, req);
    Integer idPer = safeIdPersona(session);
    if (idPer == null) {
      model.addAttribute("error", "La sesión no contiene id_persona. Vuelve a iniciar sesión.");
      return "redirect:/login";
    }
    ProfileDTO dto = svc.get(idPer);
    model.addAttribute("perfil", dto);
    return "dashboard/coordinador/coordinador-perfil";
  }

  @PostMapping("/perfil")
  public String savePerfil(@ModelAttribute("perfil") ProfileDTO dto,
                           @RequestParam(value="foto", required=false) MultipartFile foto,
                           Model model, HttpSession session, HttpServletRequest req){
    ensureIds(session, req);
    Integer idPer = safeIdPersona(session);
    if (idPer == null) {
      model.addAttribute("error", "La sesión expiró. Vuelve a iniciar sesión.");
      return "redirect:/login";
    }
    try{
      dto.setIdPersona(idPer);
      svc.save(dto, foto);
      model.addAttribute("ok", "Perfil actualizado.");
    }catch(Exception e){
      log.error("Error guardando perfil coordinador", e);
      model.addAttribute("error", "No se pudo guardar el perfil.");
    }
    model.addAttribute("perfil", svc.get(idPer));
    return "dashboard/coordinador/coordinador-perfil";
  }

  @GetMapping("/cambio-clave")
  public String viewClave(){
    return "dashboard/coordinador/coordinador-cambio-clave";
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
      return "dashboard/coordinador/coordinador-cambio-clave";
    }
    if (!nueva.equals(repetir)) {
      model.addAttribute("error", "Las contraseñas no coinciden.");
      return "dashboard/coordinador/coordinador-cambio-clave";
    }
    if (actual.equals(nueva)) {
      model.addAttribute("error", "La nueva clave no debe ser igual a la anterior.");
      return "dashboard/coordinador/coordinador-cambio-clave";
    }
    if (!nueva.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
      model.addAttribute("error", "La nueva clave no cumple la política.");
      return "dashboard/coordinador/coordinador-cambio-clave";
    }
    var res = svc.cambiarClave(idUsu, actual, nueva);
    if (res.ok()) model.addAttribute("ok", res.msg());
    else          model.addAttribute("error", res.msg());
    return "dashboard/coordinador/coordinador-cambio-clave";
  }
}
