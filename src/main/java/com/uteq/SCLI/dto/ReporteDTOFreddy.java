package com.uteq.SCLI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDTOFreddy {
    private String nombreReporte;   // Ej: "Asistencia por materia", "Uso de laboratorios"
    private String descripcion;     // Explicación breve del reporte
    private Object datos;           // Aquí va la data (puede ser lista, mapa o lo que retorne la consulta)
}