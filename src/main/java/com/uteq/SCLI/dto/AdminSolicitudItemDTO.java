package com.uteq.SCLI.dto;

import java.time.LocalTime;

public class AdminSolicitudItemDTO {
    private Integer idSolicitud;
    private String  docente;
    private String  materia;
    private String  diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String  jornada;
    private String  estado;
    private String  estadoRedireccion;

    // Campos opcionales para UI (no obligatorios en queries)
    private Integer labsDisponibles; // null o 0 si no calculas disponibilidad
    private String  semaforo;        // "VERDE"/"ROJO" si quieres

    // getters/setters
    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }
    public String getDocente() { return docente; }
    public void setDocente(String docente) { this.docente = docente; }
    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getJornada() { return jornada; }
    public void setJornada(String jornada) { this.jornada = jornada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoRedireccion() { return estadoRedireccion; }
    public void setEstadoRedireccion(String estadoRedireccion) { this.estadoRedireccion = estadoRedireccion; }

    public Integer getLabsDisponibles() { return labsDisponibles; }
    public void setLabsDisponibles(Integer labsDisponibles) { this.labsDisponibles = labsDisponibles; }
    public String getSemaforo() { return semaforo; }
    public void setSemaforo(String semaforo) { this.semaforo = semaforo; }
}
