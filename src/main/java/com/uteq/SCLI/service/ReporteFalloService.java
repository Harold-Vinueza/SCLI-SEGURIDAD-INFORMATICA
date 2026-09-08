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
    @Autowired private CryptoService crypto;

     public ReporteFallo crear(Integer idEquipo, String descripcion, Integer idDocente, Integer idAdminPiso){
        Equipo eq = equipoRepo.findById(idEquipo).orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        ReporteFallo r = new ReporteFallo();
        r.setEquipo(eq);
        r.setDescripcionFallo(crypto.encriptar(descripcion));
        r.setFechaReporte(LocalDate.now());
        r.setEstadoReporte("pendiente");
        r.setIdDocente(idDocente);
        r.setIdAdminPiso(idAdminPiso);
        ReporteFallo guardado = repo.save(r);
        guardado.setDescripcionFallo(descripcion); // Devolvemos el objeto con el texto plano, no el cifrado
        return guardado;
    }

     public List<ReporteFallo> listarPorEquipo(Integer idEquipo){
        Equipo eq = equipoRepo.findById(idEquipo).orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        List<ReporteFallo> reportes = repo.findByEquipoOrderByFechaReporteDesc(eq);
        reportes.forEach(r -> r.setDescripcionFallo(crypto.desencriptar(r.getDescripcionFallo())));
        return reportes;
    }
}