// src/main/java/com/uteq/SCLI/dto/HorarioOpcionDTO.java
package com.uteq.SCLI.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

@Data
public class HorarioOpcionDTO {
  private Integer id;
  private String  diaSemana;

  // Renderiza en JSON como "HH:mm" (sin segundos)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  private LocalTime horaInicio;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  private LocalTime horaFin;

  private String  jornada;
  private Boolean disponible; // true = libre (no existe asignación aprobada en ese horario)
}
