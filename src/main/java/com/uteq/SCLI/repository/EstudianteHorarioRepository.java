package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Horario; // <- entidad JPA real
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface EstudianteHorarioRepository extends Repository<Horario, Integer> {

    // Proyección por interfaz (no necesita @Entity)
    interface Slot {
        String getDiaSemana();
        LocalTime getHoraInicio();
        LocalTime getHoraFin();
        String getMateria();
        String getLaboratorio();
        String getJornada();
    }

    @Query(value = """
      SELECT
        v.dia_semana  AS diaSemana,
        v.hora_inicio AS horaInicio,
        v.hora_fin    AS horaFin,
        v.materia     AS materia,
        v.laboratorio AS laboratorio,
        v.jornada     AS jornada
      FROM app.vw_estudiante_horario v
      WHERE v.id_estudiante = :idEst
        AND (:idMateria IS NULL OR v.id_materia = :idMateria)
      ORDER BY v.hora_inicio, v.hora_fin, v.dia_semana
      """, nativeQuery = true)
    List<Slot> slotsPorEstudiante(@Param("idEst") Integer idEst,
                                  @Param("idMateria") Integer idMateria);
}