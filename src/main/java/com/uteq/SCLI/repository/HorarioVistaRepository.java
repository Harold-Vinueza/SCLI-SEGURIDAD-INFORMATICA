package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Horario;          // ✅ entidad JPA existente
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Consulta la vista app.vw_estudiante_horario sin tocar HorarioRepository existente.
 */
public interface HorarioVistaRepository extends Repository<Horario, Integer> {

    // Proyección tipada para evitar Object[]
    interface MateriaLabel {
        Integer getIdMateria();
        String getMateria();
    }

    @Query(value = """
        SELECT DISTINCT 
               id_materia   AS idMateria,
               materia      AS materia
        FROM app.vw_estudiante_horario
        WHERE id_estudiante = :idEst
        """, nativeQuery = true)
    List<MateriaLabel> listarMateriasHorario(@Param("idEst") Integer idEstudiante);
}