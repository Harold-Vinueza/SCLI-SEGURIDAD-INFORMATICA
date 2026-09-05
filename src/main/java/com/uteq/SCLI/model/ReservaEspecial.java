package com.uteq.SCLI.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reservaespecial")
public class ReservaEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    @Column(name = "unidad_solicitante")
    private String unidadSolicitante;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "id_laboratorio")
    private Integer idLaboratorio;

    @Column(name = "prioridad")
    private Boolean prioridad;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "publicado")
    private Boolean publicado = false;

    // Opcional
    @Column(name = "titulo")
    private String titulo;

    // ===== Getters/Setters =====
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }

    public String getUnidadSolicitante() { return unidadSolicitante; }
    public void setUnidadSolicitante(String unidadSolicitante) { this.unidadSolicitante = unidadSolicitante; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Integer getIdLaboratorio() { return idLaboratorio; }
    public void setIdLaboratorio(Integer idLaboratorio) { this.idLaboratorio = idLaboratorio; }

    public Boolean getPrioridad() { return prioridad; }
    public void setPrioridad(Boolean prioridad) { this.prioridad = prioridad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Boolean getPublicado() { return publicado; }
    public void setPublicado(Boolean publicado) { this.publicado = publicado; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
}
