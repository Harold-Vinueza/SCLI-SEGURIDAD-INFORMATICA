package com.uteq.SCLI.dto;

import com.uteq.SCLI.model.ReservaEspecial;
import java.time.LocalDate;

public class AvisoDTO {
    private Integer id;
    private String  titulo;
    private String  motivo;
    private String  unidad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean prioridad;
    private String  observaciones;

    public static AvisoDTO from(ReservaEspecial r){
        AvisoDTO a = new AvisoDTO();
        a.id = r.getIdReserva();
        a.titulo = (r.getTitulo() != null && !r.getTitulo().isBlank()) ? r.getTitulo() : r.getMotivo();
        a.motivo = r.getMotivo();
        a.unidad = r.getUnidadSolicitante();
        a.fechaInicio = r.getFechaInicio();
        a.fechaFin = r.getFechaFin();
        a.prioridad = Boolean.TRUE.equals(r.getPrioridad());
        a.observaciones = r.getObservaciones();
        return a;
    }

    // getters/setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Boolean getPrioridad() { return prioridad; }
    public void setPrioridad(Boolean prioridad) { this.prioridad = prioridad; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
