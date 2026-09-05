// src/main/java/com/uteq/SCLI/dto/coordinador/NuevaSolicitudDTO.java
package com.uteq.SCLI.dto.coordinador;

import java.util.List;

public class NuevaSolicitudDTO {
  public Integer idCarrera;        // requerido
  public String  jornada;          // "Matutina" | "Vespertina" (UI)
  public String  observaciones;    // opcional
  public List<CeldaDTO> celdas;    // lista de celdas a reservar
  public static class CeldaDTO {
    public Integer idHorario;      // requerido (slot de tabla Horario)
    public Integer idMateria;      // requerido
    public Integer idDocente; 
     public Integer idLaboratorio;      // opcional
  }
}
