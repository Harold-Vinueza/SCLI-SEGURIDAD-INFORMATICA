package com.uteq.SCLI.dto;

import lombok.Data;

/** DTO exclusivo para /api/docentes/horarios (Strings para evitar 500 al serializar) */
@Data
public class DocHorarioDTO {
  private Integer id;
  private String  diaSemana;
  private String  horaInicio; // "HH:mm"
  private String  horaFin;    // "HH:mm"
  private String  jornada;
  private Boolean disponible;
}
