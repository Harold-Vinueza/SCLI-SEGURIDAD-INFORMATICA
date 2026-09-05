package com.uteq.SCLI.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;


import jakarta.persistence.*;

@Entity
@Table(name = "asignacion_laboratorio") // << snake_case
public class AsignacionLaboratorio {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_asignacion")
  private Integer idAsignacion;

  public Integer getIdAsignacion() { return idAsignacion; }
  public void setIdAsignacion(Integer id) { this.idAsignacion = id; }
}