package com.uteq.SCLI.controller;

import com.uteq.SCLI.exception.EmailWarningException;
import com.uteq.SCLI.service.AdminUsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.MediaType;
import java.util.Map;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuariosController {

  private final AdminUsuarioService adminUsuarioService;

  public AdminUsuariosController(AdminUsuarioService adminUsuarioService) {
    this.adminUsuarioService = adminUsuarioService;
  }

  @GetMapping
  public String alias() {
    return "redirect:/dashboard/admin/admin-usuarios";
  }

   @GetMapping(value = "/sugerir", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Map<String, String> sugerir(@RequestParam("nombrePersona") String nombrePersona) {
    String username = adminUsuarioService.sugerirUsernameDesdeNombre(nombrePersona);
    String password = adminUsuarioService.generarPasswordAleatoria(12);
    return Map.of("username", username, "password", password);
  }

  
  @PostMapping("/crear")
public String crearUsuario(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam Integer idPersona,
                           @RequestParam Integer idRol,
                           RedirectAttributes ra) {
  try {
    adminUsuarioService.crearUsuario(username, password, idPersona, idRol);
    ra.addFlashAttribute("ok", "Usuario creado correctamente.");
  } catch (EmailWarningException warn) {
    ra.addFlashAttribute("ok", "Usuario creado correctamente.");
    ra.addFlashAttribute("warn", "⚠ No se pudo enviar el correo de credenciales. Revise la configuración SMTP.");
  } catch (Exception ex) {
    ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage() : "No se pudo crear el usuario.");
  }
  return "redirect:/dashboard/admin/admin-usuarios";
}

  @PostMapping("/rol")
  public String cambiarRol(@RequestParam Integer idUsuario,
                           @RequestParam Integer idRol,
                           RedirectAttributes ra) {
    try {
      adminUsuarioService.cambiarRol(idUsuario, idRol);
      ra.addFlashAttribute("ok", "Rol actualizado.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage() : "No se pudo cambiar el rol.");
    }
    return "redirect:/dashboard/admin/admin-usuarios";
  }

  @PostMapping("/reset")
  public String resetClave(@RequestParam Integer idUsuario,
                           @RequestParam String password,
                           RedirectAttributes ra) {
    try {
      adminUsuarioService.resetearClave(idUsuario, password);
      ra.addFlashAttribute("ok", "Contraseña restablecida.");
    } catch (Exception ex) {
      ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage() : "No se pudo restablecer la contraseña.");
    }
    return "redirect:/dashboard/admin/admin-usuarios";
  }
}
