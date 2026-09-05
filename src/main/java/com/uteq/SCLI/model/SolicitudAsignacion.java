package com.uteq.SCLI.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "solicitudasignacion", schema = "public")
public class SolicitudAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(name = "id_docente")
    private Integer idDocente;

    @Column(name = "id_horario")
    private Integer idHorario;

    @Column(name = "materia")
    private String materia;

    @Column(name = "tipo_solicitud")
    private String tipoSolicitud; // ej: "Nueva", "Cambio"

    @Column(name = "estado")
    private String estado; // Pendiente, Revisión, Aprobada, Rechazada, Expirada

    @Column(name = "estado_redireccion")
    private String estadoRedireccion; // texto libre para propuesta

    @Column(name = "observaciones_admin")
    private String observacionesAdmin;

    @Column(name = "fecha_solicitud")
    private LocalDate fechaSolicitud;

    @Column(name = "id_admin_piso")
    private Integer idAdminPiso; // routing al admin de piso

    // getters y setters
    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public Integer getIdDocente() { return idDocente; }
    public void setIdDocente(Integer idDocente) { this.idDocente = idDocente; }

    public Integer getIdHorario() { return idHorario; }
    public void setIdHorario(Integer idHorario) { this.idHorario = idHorario; }

    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }

    public String getTipoSolicitud() { return tipoSolicitud; }
    public void setTipoSolicitud(String tipoSolicitud) { this.tipoSolicitud = tipoSolicitud; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoRedireccion() { return estadoRedireccion; }
    public void setEstadoRedireccion(String estadoRedireccion) { this.estadoRedireccion = estadoRedireccion; }

    public String getObservacionesAdmin() { return observacionesAdmin; }
    public void setObservacionesAdmin(String observacionesAdmin) { this.observacionesAdmin = observacionesAdmin; }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public Integer getIdAdminPiso() { return idAdminPiso; }
    public void setIdAdminPiso(Integer idAdminPiso) { this.idAdminPiso = idAdminPiso; }
}
