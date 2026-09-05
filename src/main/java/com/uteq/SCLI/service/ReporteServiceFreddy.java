package com.uteq.SCLI.service;

import com.uteq.SCLI.repository.ReporteRepositoryFreddy;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Service
public class ReporteServiceFreddy {

    private final ReporteRepositoryFreddy reporteRepositoryFreddy;

    public ReporteServiceFreddy(ReporteRepositoryFreddy reporteRepositoryFreddy) {
        this.reporteRepositoryFreddy = reporteRepositoryFreddy;
    }

    // --- Asistencia por materia ---
    public List<Map<String, Object>> getAsistenciaPorMateria() {
        return reporteRepositoryFreddy.asistenciaPorMateria();
    }

    // --- Uso de laboratorios ---
    public List<Map<String, Object>> getUsoLaboratorios() {
        return reporteRepositoryFreddy.usoLaboratorios();
    }

    // --- Carga de docentes ---
    public List<Map<String, Object>> getCargaDocentes() {
        return reporteRepositoryFreddy.cargaDocentes();
    }

    // --- Top asistencia estudiantes ---
    public List<Map<String, Object>> getTopAsistenciaEstudiantes() {
        return reporteRepositoryFreddy.topAsistenciaEstudiantes();
    }

    public List<Map<String,Object>> getReporteFallos(Integer labId, LocalDate desde, LocalDate hasta, String estado){
        return reporteRepositoryFreddy.reporteFallos(labId, desde, hasta, estado);
    }
    public List<Map<String,Object>> getTendenciaMensualFallos(LocalDate desde, LocalDate hasta, Integer labId, String estado){
        return reporteRepositoryFreddy.tendenciaMensualFallos(desde, hasta, labId, estado);
    }


    // --- Ocupación de laboratorios ---
    public List<Map<String, Object>> getOcupacionLaboratorios() {
        return reporteRepositoryFreddy.ocupacionLaboratorios();
    }
    // --- Resumen del sistema ---
    public List<Map<String, Object>> getResumenSistema() {
        return reporteRepositoryFreddy.resumenSistema();
    }

}