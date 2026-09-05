package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.FacultadDTO;
import com.uteq.SCLI.model.Facultad;
import com.uteq.SCLI.repository.FacultadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacultadService {
    private final FacultadRepository repo;
    public FacultadService(FacultadRepository repo){ this.repo = repo; }

    public Page<FacultadDTO> list(String q, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("idFacultad").ascending());
        Page<Facultad> res = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findAll(pageable).map(f -> f); // simple paginado; para búsq rápida podrías crear query por nombre
        return res.map(f -> new FacultadDTO(f.getIdFacultad(), f.getNombreFacultad()));
    }

    @Transactional
    public FacultadDTO create(FacultadDTO dto){
        if (repo.existsByNombreFacultadIgnoreCase(dto.nombreFacultad()))
            throw new DataIntegrityViolationException("Ya existe una facultad con ese nombre");
        Facultad f = new Facultad();
        f.setNombreFacultad(dto.nombreFacultad());
        f = repo.save(f);
        return new FacultadDTO(f.getIdFacultad(), f.getNombreFacultad());
    }

    @Transactional
    public FacultadDTO update(Integer id, FacultadDTO dto){
        Facultad f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada"));
        if (!f.getNombreFacultad().equalsIgnoreCase(dto.nombreFacultad())
                && repo.existsByNombreFacultadIgnoreCase(dto.nombreFacultad()))
            throw new DataIntegrityViolationException("Ya existe una facultad con ese nombre");
        f.setNombreFacultad(dto.nombreFacultad());
        f = repo.save(f);
        return new FacultadDTO(f.getIdFacultad(), f.getNombreFacultad());
    }

    @Transactional
    public void delete(Integer id){
        try{
            repo.deleteById(id);
        }catch (DataIntegrityViolationException ex){
            throw new DataIntegrityViolationException("No se puede eliminar la facultad: está referenciada por carreras");
        }
    }

    public FacultadDTO get(Integer id){
        Facultad f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada"));
        return new FacultadDTO(f.getIdFacultad(), f.getNombreFacultad());
    }
}
