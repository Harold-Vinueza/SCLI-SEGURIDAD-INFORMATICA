package com.uteq.SCLI.service;

import com.uteq.SCLI.model.Equipo;
import com.uteq.SCLI.model.ReporteFallo;
import com.uteq.SCLI.repository.EquipoRepository;
import com.uteq.SCLI.repository.ReporteFalloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteFalloService {
    @Autowired private ReporteFalloRepository repo;
    @Autowired private EquipoRepository equipoRepo;

     public ReporteFallo crear(Integer idEquipo, String descripcion, Integer idDocente, Integer idAdminPiso){
        Equipo eq = equipoRepo.findById(idEquipo).orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        ReporteFallo r = new ReporteFallo();
        r.setEquipo(eq);
        r.setDescripcionFallo(descripcion);
        r.setFechaReporte(LocalDate.now());
        r.setEstadoReporte("pendiente");
        r.setIdDocente(idDocente);
        r.setIdAdminPiso(idAdminPiso);
        return repo.save(r);
    }

     public List<ReporteFallo> listarPorEquipo(Integer idEquipo){
        Equipo eq = equipoRepo.findById(idEquipo).orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        return repo.findByEquipoOrderByFechaReporteDesc(eq);
    }
}