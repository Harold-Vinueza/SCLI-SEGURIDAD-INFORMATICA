// src/main/java/com/uteq/SCLI/dto/coordinador/SolicitudCreadaDTO.java
package com.uteq.SCLI.dto.coordinador;

import java.util.List;

public class SolicitudCreadaDTO {
  public Integer idSolicitud;
  public String  estado;           // "Pendiente"
  public List<ItemResultado> items;
  public static class ItemResultado {
    public Integer idHorario;
    public String  materiaTexto;   // lo que quedó guardado en detalle
    public boolean conflictivo;    // si el slot estaba ocupado
    public String  motivo;         // texto explicativo
  }
}
