package com.uteq.SCLI.dto;

import lombok.Data;

@Data
public class LabOpcionDTO {
    private Integer id;
    private String  nombre;
    private Integer capacidad;
    private String  estado;
    private String  piso;
    private Boolean disponible; // true = libre en el horario de la solicitud
}
