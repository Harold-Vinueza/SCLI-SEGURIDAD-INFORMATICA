package com.uteq.SCLI.dto;

import java.time.LocalDate;
import java.util.List;

public record PaseListaVM(
    Integer idRegistro,
    Integer idMateria,
    String  nombreMateria,
    LocalDate fechaClase,
    String  temaClase,
    String  nombreDocente,
    String  nombreLaboratorio,
    List<StudentAttendanceRow> filas
) {}
