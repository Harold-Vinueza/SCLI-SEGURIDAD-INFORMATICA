package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultadRepository extends JpaRepository<Facultad, Integer> {
    boolean existsByNombreFacultadIgnoreCase(String nombre);
    Optional<Facultad> findByNombreFacultadIgnoreCase(String nombre);
}
