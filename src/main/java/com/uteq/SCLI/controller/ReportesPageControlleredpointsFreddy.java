// ReportesPageController.java
package com.uteq.SCLI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportesPageControlleredpointsFreddy {

    // Dashboard de reportes (nuevo)
    @GetMapping("/dashboard/admin/reportes/overview")
    public String reportesOverview() {
        return "dashboard/admin/reportes/overview"; // templates/dashboard/admin/reportes/overview.html
    }

    // Luego replicas esto para los demás:
    // /dashboard/admin/reportes/asistencia-materia  -> asistencia-materia.html
    // /dashboard/admin/reportes/uso-laboratorios    -> uso-laboratorios.html
    // /dashboard/admin/reportes/carga-docentes      -> carga-docentes.html
    // /dashboard/admin/reportes/top-estudiantes     -> top-estudiantes.html
    // /dashboard/admin/reportes/reporte-fallos      -> reporte-fallos.html
    // /dashboard/admin/reportes/tendencia-fallos    -> tendencia-fallos.html
}