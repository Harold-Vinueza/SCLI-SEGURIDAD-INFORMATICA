package com.uteq.SCLI.model;


import jakarta.persistence.*;



@Entity
@Table(name = "equipo")
public class Equipo {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Integer idEquipo;

    @Column(name = "codigo_equipo", unique = true, length = 50)
    private String codigoEquipo;

    @Column(name = "tipo_equipo", length = 30)
    private String tipoEquipo;

    @Column(length = 30)
    private String marca;

    @Column(length = 30)
    private String modelo;

    @Column(length = 20)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "id_laboratorio")
    private Laboratorio laboratorio;

    // getters/setters
    public Integer getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Integer id) { this.idEquipo = id; }

    public String getCodigoEquipo() { return codigoEquipo; }
    public void setCodigoEquipo(String c) { this.codigoEquipo = c; }

    public String getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(String t) { this.tipoEquipo = t; }

    public String getMarca() { return marca; }
    public void setMarca(String m) { this.marca = m; }

    public String getModelo() { return modelo; }
    public void setModelo(String m) { this.modelo = m; }

    public String getEstado() { return estado; }
    public void setEstado(String e) { this.estado = e; }

    public Laboratorio getLaboratorio() { return laboratorio; }
    public void setLaboratorio(Laboratorio l) { this.laboratorio = l; }
    
}
