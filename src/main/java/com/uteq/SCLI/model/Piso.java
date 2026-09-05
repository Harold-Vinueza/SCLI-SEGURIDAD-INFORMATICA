package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(name = "piso")
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piso")
    private Integer idPiso;

    @Column(name = "numero_piso", nullable = false)
    private Integer numeroPiso;

    @Column(length = 200)
    private String descripcion;

    // --- getters/setters ---

    public Integer getIdPiso() { return idPiso; }
    public void setIdPiso(Integer idPiso) { this.idPiso = idPiso; }

    public Integer getNumeroPiso() { return numeroPiso; }
    public void setNumeroPiso(Integer numeroPiso) { this.numeroPiso = numeroPiso; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return "Piso " + numeroPiso + (descripcion != null ? " - " + descripcion : "");
    }
}