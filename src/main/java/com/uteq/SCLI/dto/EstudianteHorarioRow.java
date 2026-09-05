package com.uteq.SCLI.dto;

import java.time.LocalTime;

public interface EstudianteHorarioRow {
    Integer getIdEstudiante();
    Integer getIdMateria();
    String  getCodMateria();
    String  getNombreMateria();
    Integer getIdDocente();
    String  getDocente();
    Integer getIdLaboratorio();
    String  getCodLaboratorio();
    String  getDiaSemana();
    String  getJornada();
    LocalTime getHoraInicio();
    LocalTime getHoraFin();
    Integer getIdPeriodo();
    String  getPeriodo();
}
