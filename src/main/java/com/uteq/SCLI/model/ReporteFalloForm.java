package com.uteq.SCLI.model;

public class ReporteFalloForm {
    private Integer idEquipo;
    private String descripcionFallo;
    private Integer idDocente;     // opcional
    private Integer idAdminPiso;   // opcional

    public Integer getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Integer idEquipo) { this.idEquipo = idEquipo; }

    public String getDescripcionFallo() { return descripcionFallo; }
    public void setDescripcionFallo(String descripcionFallo) { this.descripcionFallo = descripcionFallo; }

    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public Integer getIdAdminPiso() { return idAdminPiso; }
    public void setIdAdminPiso(Integer idAdminPiso) { this.idAdminPiso = idAdminPiso; }
}