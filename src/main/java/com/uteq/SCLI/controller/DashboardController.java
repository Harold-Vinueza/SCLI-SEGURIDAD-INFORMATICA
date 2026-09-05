package com.uteq.SCLI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.uteq.SCLI.dto.UserSession;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

       @GetMapping({"", "/"})
    public String router(HttpSession session) {
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us == null || us.getNombreRol() == null) {
            return "redirect:/login";
        }
        String rol = us.getNombreRol().trim().toLowerCase();
        switch (rol) {
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
            default:
                return "redirect:/login?error=rol";
        }
    }

    @GetMapping("/admin")
    public String admin() {
        // templates/dashboard/admin.html
        return "dashboard/admin";
    }

    @GetMapping("/docente")
    public String docente() {
        // templates/dashboard/docente.html
        return "dashboard/docente";
    }

    @GetMapping("/estudiante")
    public String estudiante() {
        // templates/dashboard/estudiante.html
        return "dashboard/estudiante";
    }

    @GetMapping("/docente/reservas")
    public String docenteReservas() {
        // busca templates/dashboard/docente/reservas.html
        return "dashboard/docente/reservas";
    }

    @GetMapping("/admin/solicitudes")
    public String adminSolicitudes() {
        // busca templates/dashboard/admin/solicitudes.html
        return "dashboard/admin/solicitudes";
    }
   
}
