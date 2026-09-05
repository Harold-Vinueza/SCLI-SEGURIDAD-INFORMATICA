package com.uteq.SCLI.dto;

public record StudentAttendanceRow(
    Integer idEstudiante,
    String  apellidos,
    String  nombres,
    Boolean presente
) {}