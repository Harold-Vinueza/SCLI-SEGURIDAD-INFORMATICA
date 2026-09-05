package com.uteq.SCLI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonaDto {
    private Integer id;
    private String nombres;
    private String apellidos;
}
