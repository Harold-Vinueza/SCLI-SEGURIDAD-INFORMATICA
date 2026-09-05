// src/main/java/com/uteq/SCLI/repository/EstudianteRepository.java
package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Estudiante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EstudianteRepository extends Repository<Estudiante, Integer> {

     @Query(value = "SELECT id_estudiante FROM public.estudiante WHERE id_persona = :idPersona", nativeQuery = true)
    Integer findIdByPersona(@Param("idPersona") Integer idPersona);

    // (Opcional) si te sirve el current de la sesión de BD
    @Query(value = "SELECT NULLIF(current_setting('app.current_estudiante_id', true),'')::int", nativeQuery = true)
    Integer currentIdFromDb();


     @Query(value = "SELECT e.id_estudiante " +
            "FROM public.estudiante e " +
            "WHERE e.id_persona = :idPersona " +
            "LIMIT 1", nativeQuery = true)
    Integer findIdEstudianteByIdPersona(@Param("idPersona") Integer idPersona);

    @Query(value = "SELECT e.id_estudiante " +
            "FROM public.estudiante e " +
            "JOIN public.usuario u ON u.id_persona = e.id_persona " +
            "WHERE u.id_usuario = :idUsuario " +
            "LIMIT 1", nativeQuery = true)
    Integer findIdEstudianteByIdUsuario(@Param("idUsuario") Integer idUsuario);
}
