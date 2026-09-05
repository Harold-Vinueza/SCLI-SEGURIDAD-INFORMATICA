package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAuditoriaController {

    private final AuditService auditService;

    public AdminAuditoriaController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/dashboard/admin/auditorias")
    public String auditorias(
            @RequestParam(required = false, defaultValue = "logins") String tab,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) Boolean ok,         // solo aplica a logins
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false, defaultValue = "200") Integer limit,
            Model model,
            HttpSession session
    ) {
        // Gate simple (ajústalo a tus reglas)
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us == null || us.getNombreRol() == null ||
                !(us.getNombreRol().equalsIgnoreCase("admin_master")
                        || us.getNombreRol().equalsIgnoreCase("administrador"))) {
            return "redirect:/login?error=auth";
        }

        // Normaliza limit a un rango razonable
        if (limit == null || limit < 1) limit = 1;
        if (limit > 1000) limit = 1000;

        // Filtros a la vista
        model.addAttribute("tab", tab);
        model.addAttribute("fUser", user);
        model.addAttribute("fOk", ok);
        model.addAttribute("fDesde", desde);
        model.addAttribute("fHasta", hasta);
        model.addAttribute("fLimit", limit);

        // Datos por pestaña
        switch (tab == null ? "logins" : tab) {
            case "sesiones":
                model.addAttribute("itemsSesiones",
                        auditService.listarSesiones(user, desde, hasta, limit));
                break;
            case "cambios":
                model.addAttribute("itemsCambios",
                        auditService.listarCambios(user, desde, hasta, limit));
                break;
            case "logins":
            default:
                model.addAttribute("itemsLogins",
                        auditService.listar(user, ok, desde, hasta, limit));
                break;
        }

        return "dashboard/admin/auditorias";
    }
}
