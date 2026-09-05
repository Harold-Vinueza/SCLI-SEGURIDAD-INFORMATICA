package com.uteq.SCLI.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.uteq.SCLI.dto.LaboratorioDTO;
import com.uteq.SCLI.model.Laboratorio;
import com.uteq.SCLI.model.Piso;
import com.uteq.SCLI.repository.LaboratorioRepository;
import com.uteq.SCLI.repository.PisoRepository;

@Service
@RequiredArgsConstructor
public class LaboratorioService {
    private final LaboratorioRepository labRepo;
    private final PisoRepository pisoRepo;

    public Page<Laboratorio> listar(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (q == null || q.isBlank()) return labRepo.findAll(pageable);
        return labRepo.findByNombreLaboratorioContainingIgnoreCase(q.trim(), pageable);
    }

    public Laboratorio obtener(Integer id) {
        return labRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Laboratorio no encontrado"));
    }

    @Transactional
    public Laboratorio crear(LaboratorioDTO dto) {
        Laboratorio l = new Laboratorio();
        map(dto, l);
        return labRepo.save(l);
    }

    @Transactional
    public Laboratorio actualizar(Integer id, LaboratorioDTO dto) {
        Laboratorio l = obtener(id);
        map(dto, l);
        return labRepo.save(l);
    }

    @Transactional
    public void eliminar(Integer id) {
        labRepo.deleteById(id);
        // DB ya audita con trigger audit_laboratorio
    }

    private void map(LaboratorioDTO dto, Laboratorio l) {
        l.setNombreLaboratorio(dto.getNombreLaboratorio());
        l.setCapacidad(dto.getCapacidad());
        l.setEstado(dto.getEstado());
        l.setCodLaboratorio(dto.getCodLaboratorio());

        Piso piso = pisoRepo.findById(dto.getIdPiso())
                .orElseThrow(() -> new EntityNotFoundException("Piso no válido"));
        l.setPiso(piso);
    }
}