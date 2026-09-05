package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.ReservaEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaEspecialRepository extends JpaRepository<ReservaEspecial, Integer> {
    List<ReservaEspecial> findByPublicadoTrueOrderByFechaInicioDesc();
    List<ReservaEspecial> findTop5ByPublicadoTrueOrderByFechaInicioDesc();
}
