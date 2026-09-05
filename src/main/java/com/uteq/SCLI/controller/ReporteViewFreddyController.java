package com.uteq.SCLI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReporteViewFreddyController {

    // Página “Reportes Freddy” dentro del dashboard
    @GetMapping("/dashboard/admin/reportes")
    public String reportesFreddy() {
        // Carga el HTML que creamos abajo
        return "dashboard/admin/reportes-freddy";
    }
}