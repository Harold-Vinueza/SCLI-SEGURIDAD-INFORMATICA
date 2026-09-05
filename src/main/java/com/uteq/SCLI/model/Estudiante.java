package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estudiante")   // nombre real de tu tabla
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Integer idEstudiante;

    @ManyToOne
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    // otros atributos...

    // Getters y setters
}
