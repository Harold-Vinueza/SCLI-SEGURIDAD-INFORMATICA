package com.uteq.SCLI.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reportefallo")
public class ReporteFallo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;

    @Column(name = "id_docente")
    private Integer idDocente;

    @Column(name = "id_admin_piso")
    private Integer idAdminPiso;

    @Column(name = "descripcion_fallo")
    private String descripcionFallo;

    @Column(name = "fecha_reporte")
    private LocalDate fechaReporte;

    @Column(name = "estado_reporte")
    private String estadoReporte;

    // -------- getters & setters ----------
    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }

    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public Integer getIdAdminPiso() { return idAdminPiso; }
    public void setIdAdminPiso(Integer idAdminPiso) { this.idAdminPiso = idAdminPiso; }

    public String getDescripcionFallo() { return descripcionFallo; }
    public void setDescripcionFallo(String descripcionFallo) { this.descripcionFallo = descripcionFallo; }

    public LocalDate getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDate fechaReporte) { this.fechaReporte = fechaReporte; }

    public String getEstadoReporte() { return estadoReporte; }
    public void setEstadoReporte(String estadoReporte) { this.estadoReporte = estadoReporte; }

    // en com.uteq.SCLI.model.ReporteFallo
public Integer getIdReporteFallo() { return this.idReporte; }
public void setIdReporteFallo(Integer id) { this.idReporte = id; }

}