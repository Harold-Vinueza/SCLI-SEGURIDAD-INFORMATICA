// com.uteq.SCLI.dto.ProfileDTO.java
package com.uteq.SCLI.dto;
import lombok.Data;

@Data
public class ProfileDTO {
  private Integer idPersona;
  private String nombres;
  private String apellidos;
  private String correo;
  private String telefono;
  private String tituloAcademico;
  private String departamento;
  private String fotoUrl;
}
