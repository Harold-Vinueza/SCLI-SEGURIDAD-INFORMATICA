package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.ReporteFallo;
import com.uteq.SCLI.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReporteFalloRepository extends JpaRepository<ReporteFallo, Integer> {
    List<ReporteFallo> findByEquipoOrderByFechaReporteDesc(Equipo equipo);
     long countByEquipoAndEstadoReporteIgnoreCase(Equipo equipo, String estado);
}