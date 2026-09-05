package com.uteq.SCLI.model;


import jakarta.persistence.*;

@Entity
@Table(name = "laboratorio")
public class Laboratorio {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_laboratorio")
    private Integer idLaboratorio;

    @Column(name = "nombre_laboratorio", length = 50)
    private String nombreLaboratorio;

    private Integer capacidad;

    @Column(length = 20)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "id_piso")
    private Piso piso;

        // NUEVO: columna opcional (en tu BD es nullable, índice único parcial)
    @Column(name = "cod_laboratorio")
    private String codLaboratorio;
    // getters/setters
    public Integer  getIdLaboratorio() { return idLaboratorio; }
    public void setIdLaboratorio(Integer id) { this.idLaboratorio = id; }

    public String getNombreLaboratorio() { return nombreLaboratorio; }
    public void setNombreLaboratorio(String n) { this.nombreLaboratorio = n; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer c) { this.capacidad = c; }

    public String getEstado() { return estado; }
    public void setEstado(String e) { this.estado = e; }



     public Piso getPiso() { return piso; }
    public void setPiso(Piso piso) { this.piso = piso; }

    public String getCodLaboratorio() { return codLaboratorio; }
    public void setCodLaboratorio(String codLaboratorio) { this.codLaboratorio = codLaboratorio; }





    @Override public String toString() { return nombreLaboratorio; }
    
}
