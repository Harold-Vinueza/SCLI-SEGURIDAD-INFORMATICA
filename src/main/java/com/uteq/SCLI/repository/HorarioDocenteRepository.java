// src/main/java/com/uteq/SCLI/repository/HorarioDocenteRepository.java
package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.AsignacionLaboratorio; // <-- tu entidad
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioDocenteRepository extends JpaRepository<AsignacionLaboratorio, Long> {

    @Query(value = """
            SELECT
              pl.nombre                           AS periodo,
              h.jornada                           AS jornada,
              h.dia_semana                        AS diaSemana,
              h.hora_inicio                       AS horaInicio,
              h.hora_fin                          AS horaFin,
              l.cod_laboratorio                   AS codLaboratorio,
              l.nombre_laboratorio                AS nombreLaboratorio,
              COALESCE(m.cod_materia || ' - ' || m.nombre_materia, a.materia) AS materia,
              a.id_asignacion                     AS idAsignacion
            FROM asignacion_laboratorio a
            JOIN periodolectivo pl ON pl.id_periodo     = a.id_periodo
            JOIN laboratorio     l ON l.id_laboratorio  = a.id_laboratorio
            JOIN horario         h ON h.id_horario      = a.id_horario
            LEFT JOIN materia    m ON m.id_materia      = a.id_materia
            WHERE a.id_docente = :idDocente
              AND a.id_periodo = :idPeriodo
            ORDER BY
              h.jornada,
              ARRAY_POSITION(ARRAY['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'], h.dia_semana),
              h.hora_inicio
            """,
            nativeQuery = true)
    List<Object[]> findRawByDocentePeriodo(@Param("idDocente") long idDocente,
                                           @Param("idPeriodo") long idPeriodo);
}
