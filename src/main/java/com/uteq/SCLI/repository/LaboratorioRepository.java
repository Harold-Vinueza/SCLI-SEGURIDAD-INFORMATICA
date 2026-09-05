package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Laboratorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer> {
    Page<Laboratorio> findByNombreLaboratorioContainingIgnoreCase(String q, Pageable pageable);

    // opcional si validas código
    boolean existsByCodLaboratorio(String codLaboratorio);
}
