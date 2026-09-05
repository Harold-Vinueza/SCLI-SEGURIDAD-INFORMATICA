package com.uteq.SCLI.dto;

public record AsignacionRequest(
  Integer idLaboratorio,
  Integer idPeriodo,
  Integer idHorario,
  Integer idDocente,   // opcional
  Integer idMateria    // OBLIGATORIO
) {}