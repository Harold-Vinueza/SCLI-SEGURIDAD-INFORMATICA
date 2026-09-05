package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    @Query(value = "SELECT id_docente FROM public.docente WHERE id_persona = :idPersona", nativeQuery = true)
Integer findIdByPersona(@Param("idPersona") Integer idPersona);

 @Query(value = "select id_docente from docente where id_persona = :idPersona", nativeQuery = true)
    Long findIdDocenteByIdPersona(@Param("idPersona") Integer idPersona);
}
