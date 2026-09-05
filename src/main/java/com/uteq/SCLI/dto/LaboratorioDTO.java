package com.uteq.SCLI.dto;

public class LaboratorioDTO {
    private Integer idLaboratorio;
    private String codLaboratorio;      // si lo usas
    private String nombreLaboratorio;
    private Integer capacidad;
    private String estado;
    private Integer idPiso;             // <<-- necesario para tu service

    public Integer getIdLaboratorio() { return idLaboratorio; }
    public void setIdLaboratorio(Integer idLaboratorio) { this.idLaboratorio = idLaboratorio; }
    public String getCodLaboratorio() { return codLaboratorio; }
    public void setCodLaboratorio(String codLaboratorio) { this.codLaboratorio = codLaboratorio; }
    public String getNombreLaboratorio() { return nombreLaboratorio; }
    public void setNombreLaboratorio(String nombreLaboratorio) { this.nombreLaboratorio = nombreLaboratorio; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIdPiso() { return idPiso; }
    public void setIdPiso(Integer idPiso) { this.idPiso = idPiso; }
}
