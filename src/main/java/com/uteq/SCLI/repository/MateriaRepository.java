package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Integer> {
    boolean existsByCodMateriaIgnoreCase(String codMateria);
    List<Materia> findByIdCarrera(Integer idCarrera);

    @Query("select m from Materia m where " +
           "(:idCarrera is null or m.idCarrera = :idCarrera) and " +
           "(coalesce(:q,'') = '' or lower(m.nombreMateria) like lower(concat('%', :q, '%')) or lower(m.codMateria) like lower(concat('%', :q, '%')))")
    List<Materia> search(Integer idCarrera, String q);
}
