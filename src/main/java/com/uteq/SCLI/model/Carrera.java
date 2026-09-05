package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carrera")
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Integer idCarrera;

    @Column(name = "nombre_carrera", nullable = false, length = 100)
    private String nombreCarrera;

    // FK -> facultad.id_facultad
    @Column(name = "id_facultad")
    private Integer idFacultad;

    // FK -> persona.id_persona (coordinador)
    @Column(name = "id_persona_coordinador")
    private Integer idPersonaCoordinador;

    // --- Relaciones (solo lectura, opcionales) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_facultad", insertable = false, updatable = false)
    private Facultad facultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona_coordinador", insertable = false, updatable = false)
    private Persona coordinador;

    // Getters & Setters
    public Integer getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Integer idCarrera) { this.idCarrera = idCarrera; }

    public String getNombreCarrera() { return nombreCarrera; }
    public void setNombreCarrera(String nombreCarrera) { this.nombreCarrera = nombreCarrera; }

    public Integer getIdFacultad() { return idFacultad; }
    public void setIdFacultad(Integer idFacultad) { this.idFacultad = idFacultad; }

    public Integer getIdPersonaCoordinador() { return idPersonaCoordinador; }
    public void setIdPersonaCoordinador(Integer idPersonaCoordinador) { this.idPersonaCoordinador = idPersonaCoordinador; }

    public Facultad getFacultad() { return facultad; }
    public Persona getCoordinador() { return coordinador; }
}
