package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(name = "docente") // nombre real de tu tabla
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    private Integer idDocente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    // Getters & setters
    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
}
