// com/uteq/SCLI/repository/EstudianteAsistenciaRepository.java
package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.AsistenciaRow;
import com.uteq.SCLI.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

public interface EstudianteAsistenciaRepository extends JpaRepository<Estudiante, Integer> {

    @Query(value = """
        SELECT 
          id_estudiante  AS idEstudiante,
          id_registro    AS idRegistro,
          fecha_clase    AS fechaClase,
          tema_clase     AS temaClase,
          id_materia     AS idMateria,
          cod_materia    AS codMateria,
          nombre_materia AS nombreMateria,
          dia_semana     AS diaSemana,
          jornada        AS jornada,
          hora_inicio    AS horaInicio,
          hora_fin       AS horaFin,
          laboratorio    AS laboratorio,
          asistencia     AS asistencia,
          observaciones  AS observaciones,
          docente        AS docente
        FROM public.vw_asistencia_estudiante
        WHERE id_estudiante = :idEst
          AND (:idMat IS NULL OR id_materia = :idMat)
          AND (:desde IS NULL OR fecha_clase >= :desde)
          AND (:hasta IS NULL OR fecha_clase <= :hasta)
        ORDER BY fecha_clase DESC
        """, nativeQuery = true)
    List<AsistenciaRow> findAsistenciaEst(
            @Param("idEst") Integer idEstudiante,
            @Param("idMat") Integer idMateria,
            @Param("desde") Date desde,
            @Param("hasta") Date hasta
    );
}