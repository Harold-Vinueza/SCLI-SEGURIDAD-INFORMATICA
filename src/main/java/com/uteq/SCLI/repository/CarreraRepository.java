package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CarreraRepository extends JpaRepository<Carrera, Integer> {
    boolean existsByNombreCarreraIgnoreCaseAndIdFacultad(String nombreCarrera, Integer idFacultad);
    List<Carrera> findByIdFacultad(Integer idFacultad);

    @Query("select c from Carrera c where lower(c.nombreCarrera) like lower(concat('%', ?1, '%'))")
    List<Carrera> searchByNombre(String q);
}
