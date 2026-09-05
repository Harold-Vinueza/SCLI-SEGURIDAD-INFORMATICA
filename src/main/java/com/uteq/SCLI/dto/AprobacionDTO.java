package com.uteq.SCLI.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AprobacionDTO {
    private String observaciones;          // opcional
    private List<AprobacionItem> labs;  
      private Map<String, Integer> labsMateria;   // uno por cada fila del detalle
}
