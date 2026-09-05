package com.uteq.SCLI.controller;

import com.uteq.SCLI.service.AdminUsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/admin")
public class AdminDashboardController {

  private final AdminUsuarioService adminUsuarioService;

  public AdminDashboardController(AdminUsuarioService adminUsuarioService) {
    this.adminUsuarioService = adminUsuarioService;
  }



  @GetMapping("/horarios")
  public String horariosView() {
    return "dashboard/admin/horarios";
  }

  // /dashboard/admin/admin-usuarios  -> templates/dashboard/admin/admin-usuarios.html
  @GetMapping("/admin-usuarios")
  public String usuariosView(Model model) {
    model.addAttribute("usuarios", adminUsuarioService.listarUsuariosConRolDbRole());
    return "dashboard/admin/admin-usuarios";
  }

  // === NUEVOS MAPEOS DE VISTAS ===
  // Materias: templates/dashboard/admin/materias.html
  @GetMapping("/materias")
  public String materiasView() {
    return "dashboard/admin/materias";
  }

 

}
