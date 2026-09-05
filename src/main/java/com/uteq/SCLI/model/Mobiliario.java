package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mobiliario")
public class Mobiliario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mobiliario")
    private Integer idMobiliario;

    @Column(name = "tipo_mobiliario")
    private String tipoMobiliario;

    private Integer cantidad;

    private String estado;

    @ManyToOne
    @JoinColumn(name = "id_laboratorio")
    private Laboratorio laboratorio;

    // --- getters & setters ---
    public Integer getIdMobiliario() { return idMobiliario; }
    public void setIdMobiliario(Integer idMobiliario) { this.idMobiliario = idMobiliario; }

    public String getTipoMobiliario() { return tipoMobiliario; }
    public void setTipoMobiliario(String tipoMobiliario) { this.tipoMobiliario = tipoMobiliario; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Laboratorio getLaboratorio() { return laboratorio; }
    public void setLaboratorio(Laboratorio laboratorio) { this.laboratorio = laboratorio; }
}