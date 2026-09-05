package com.uteq.SCLI.controller;

import com.uteq.SCLI.service.ReporteServiceFreddy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes-freddy")
public class ReporteControllerFreddy {

    private final ReporteServiceFreddy reporteServiceFreddy;

    public ReporteControllerFreddy(ReporteServiceFreddy reporteServiceFreddy) {
        this.reporteServiceFreddy = reporteServiceFreddy;
    }

    // --- Asistencia por materia ---
    @GetMapping("/asistencia-materia")
    public List<Map<String, Object>> asistenciaPorMateria() {
        return reporteServiceFreddy.getAsistenciaPorMateria();
    }

    // --- Uso de laboratorios ---
    @GetMapping("/uso-laboratorios")
    public List<Map<String, Object>> usoLaboratorios() {
        return reporteServiceFreddy.getUsoLaboratorios();
    }

    // --- Carga de docentes ---
    @GetMapping("/carga-docentes")
    public List<Map<String, Object>> cargaDocentes() {
        return reporteServiceFreddy.getCargaDocentes();
    }

    // --- Top asistencia estudiantes ---
    @GetMapping("/top-estudiantes")
    public List<Map<String, Object>> topAsistenciaEstudiantes() {
        return reporteServiceFreddy.getTopAsistenciaEstudiantes();
    }

    // --- Reporte de fallos (detalle con filtros) ---
    @GetMapping("/fallos")
    public List<Map<String,Object>> reporteFallos(
            @RequestParam(required = false) Integer labId,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate desde,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate hasta,
            @RequestParam(required = false) String estado
    ){
        return reporteServiceFreddy.getReporteFallos(labId, desde, hasta, estado);
    }

    // --- Tendencia mensual de fallos (con filtros) ---
    @GetMapping("/fallos-mensuales")
    public List<Map<String,Object>> tendenciaMensualFallos(
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate desde,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate hasta,
            @RequestParam(required = false) Integer labId,
            @RequestParam(required = false) String estado
    ){
        return reporteServiceFreddy.getTendenciaMensualFallos(desde, hasta, labId, estado);
    }

    // --- Ocupación de laboratorios ---
    @GetMapping("/ocupacion-laboratorios")
    public List<Map<String, Object>> ocupacionLaboratorios() {
        return reporteServiceFreddy.getOcupacionLaboratorios();
    }
    // --- Resumen del sistema ---
    @GetMapping("/resumen-sistema")
    public List<Map<String, Object>> resumenSistema() {
        return reporteServiceFreddy.getResumenSistema();
    }
}