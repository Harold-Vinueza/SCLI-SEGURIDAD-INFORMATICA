package com.uteq.SCLI.dto;

import java.sql.Date;
import java.sql.Time;

public interface AsistenciaRow {
    Integer getIdEstudiante();
    Integer getIdRegistro();
    Date getFechaClase();
    String getTemaClase();
    Integer getIdMateria();
    String getCodMateria();
    String getNombreMateria();
    String getDiaSemana();
    String getJornada();
    Time getHoraInicio();
    Time getHoraFin();
    String getLaboratorio();
    Boolean getAsistencia();
    String getObservaciones();
    String getDocente();
}