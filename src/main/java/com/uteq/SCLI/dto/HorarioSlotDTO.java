// src/main/java/com/uteq/SCLI/dto/HorarioSlotDTO.java
package com.uteq.SCLI.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class HorarioSlotDTO {
    private Integer id;
    private String  diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String  jornada;
    private Boolean disponible;
}
