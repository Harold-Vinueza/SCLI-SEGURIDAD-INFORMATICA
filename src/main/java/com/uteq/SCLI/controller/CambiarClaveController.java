// src/main/java/com/uteq/SCLI/controller/CambiarClaveController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.PasswordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CambiarClaveController {

  private final PasswordService svc;
  public CambiarClaveController(PasswordService svc){ this.svc = svc; }

  @GetMapping("/cambiar-clave")
  public String form(HttpSession s, Model model){
    // si ya no debe cambiar, lánzalo a su dashboard real
    Object must = s.getAttribute("MUST_CHANGE_PASSWORD");
    if (must instanceof Boolean b && !b) {
      return "redirect:" + resolveDashboardByRole(s);
    }
    return "auth/cambiar-clave";
  }

  @PostMapping("/cambiar-clave")
  public String cambiar(@RequestParam String actual,
                        @RequestParam String nueva,
                        @RequestParam String repetir,
                        HttpSession s,
                        RedirectAttributes ra){
    if (!nueva.equals(repetir)) {
      ra.addFlashAttribute("error","Las contraseñas nuevas no coinciden.");
      return "redirect:/cambiar-clave";
    }

    Integer idUsuario = getIdUsuarioFromSession(s);
    if (idUsuario == null){
      ra.addFlashAttribute("error","Sesión no válida.");
      return "redirect:/login";
    }

    try {
      svc.cambiar(idUsuario, actual, nueva);
      s.setAttribute("MUST_CHANGE_PASSWORD", false);
      ra.addFlashAttribute("ok","Contraseña actualizada correctamente.");
      return "redirect:" + resolveDashboardByRole(s); // << redirección correcta
    } catch (Exception ex){
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/cambiar-clave";
    }
  }

  // ---- helpers ----
  private static Integer getIdUsuarioFromSession(HttpSession s){
    Object v;
    if ((v = s.getAttribute("ID_USUARIO")) != null && v instanceof Integer) return (Integer) v; // por si lo tienes
    if ((v = s.getAttribute("id_usuario")) != null && v instanceof Integer) return (Integer) v;
    if ((v = s.getAttribute("idUsuario"))  != null && v instanceof Integer) return (Integer) v;
    UserSession us = (UserSession) s.getAttribute("userSession");
    return (us != null) ? us.getIdUsuario() : null;
  }

  private static String resolveDashboardByRole(HttpSession s){
    // intenta desde userSession; si no, desde "rol" en sesión
    UserSession us = (UserSession) s.getAttribute("userSession");
    String rol = (us != null ? us.getNombreRol() : null);
    if (rol == null) {
      Object r = s.getAttribute("rol");
      rol = (r != null) ? r.toString() : "";
    }
    rol = rol == null ? "" : rol.trim().toLowerCase();

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
        return "/login";
    }
  }
}
