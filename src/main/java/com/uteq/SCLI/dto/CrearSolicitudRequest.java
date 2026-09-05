package com.uteq.SCLI.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearSolicitudRequest {
    private Integer idHorario;
    private String  motivo;
    private String  tipoSolicitud; // "Nueva" | "Cambio" | "Temporal"
    private Integer idAdminPiso;   // admin de piso a quien se enruta (si aplica)
    private Integer idMateria;     // opcional: tu tabla guarda 'materia' texto; el service lo resuelve

    // >>> Fecha elegida en el modal (enviar como "YYYY-MM-DD")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaUso;
}