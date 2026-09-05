// src/main/java/com/uteq/SCLI/dto/HorarioDocenteItem.java
package com.uteq.SCLI.dto;

import java.time.LocalTime;

public record HorarioDocenteItem(
        String periodo,
        String jornada,
        String diaSemana,       // Lunes, Martes, ...
        LocalTime horaInicio,
        LocalTime horaFin,
        String codLaboratorio,
        String nombreLaboratorio,
        String materia,         // "COD - Nombre" o texto libre
        Long idAsignacion
) {}