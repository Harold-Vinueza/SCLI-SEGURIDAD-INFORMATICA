package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Piso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PisoRepository extends JpaRepository<Piso, Integer> {
    // opcional: boolean existsByNumeroPiso(Integer numeroPiso);
}
