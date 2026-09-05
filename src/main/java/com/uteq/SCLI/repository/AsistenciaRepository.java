package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.AsistenciaRow;
import com.uteq.SCLI.model.Estudiante;               // ✅ usa una @Entity REAL que exista (Estudiante, Persona, etc.)
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

public interface AsistenciaRepository extends Repository<Estudiante, Integer> {  // 👈 NUNCA uses Object aquí

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
    List<AsistenciaRow> findAsistencia(
            @Param("idEst") Integer idEstudiante,
            @Param("idMat") Integer idMateria,
            @Param("desde") Date desde,
            @Param("hasta") Date hasta
    );
}