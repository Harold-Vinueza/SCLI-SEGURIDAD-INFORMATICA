package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.CarreraDTO;
import com.uteq.SCLI.model.Carrera;
import com.uteq.SCLI.repository.CarreraRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarreraService {
    private final CarreraRepository repo;
    public CarreraService(CarreraRepository repo){ this.repo = repo; }

    public Page<CarreraDTO> list(Integer idFacultad, String q, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("idCarrera").ascending());
        List<Carrera> base = (idFacultad != null) ? repo.findByIdFacultad(idFacultad)
                                                  : repo.findAll();
        if (q != null && !q.isBlank()) base = repo.searchByNombre(q);
        int from = Math.min(page*size, base.size());
        int to = Math.min(from+size, base.size());
        Page<Carrera> pageRes = new PageImpl<>(base.subList(from, to), pageable, base.size());
        return pageRes.map(c -> new CarreraDTO(c.getIdCarrera(), c.getNombreCarrera(), c.getIdFacultad(), c.getIdPersonaCoordinador()));
    }

    @Transactional
    public CarreraDTO create(CarreraDTO dto){
        if (repo.existsByNombreCarreraIgnoreCaseAndIdFacultad(dto.nombreCarrera(), dto.idFacultad()))
            throw new DataIntegrityViolationException("Ya existe una carrera con ese nombre en la misma facultad");
        Carrera c = new Carrera();
        c.setNombreCarrera(dto.nombreCarrera());
        c.setIdFacultad(dto.idFacultad());
        c.setIdPersonaCoordinador(dto.idPersonaCoordinador());
        c = repo.save(c);
        return new CarreraDTO(c.getIdCarrera(), c.getNombreCarrera(), c.getIdFacultad(), c.getIdPersonaCoordinador());
    }

    @Transactional
    public CarreraDTO update(Integer id, CarreraDTO dto){
        Carrera c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada"));
        boolean nombreCambio = !c.getNombreCarrera().equalsIgnoreCase(dto.nombreCarrera()) || !c.getIdFacultad().equals(dto.idFacultad());
        if (nombreCambio && repo.existsByNombreCarreraIgnoreCaseAndIdFacultad(dto.nombreCarrera(), dto.idFacultad()))
            throw new DataIntegrityViolationException("Ya existe una carrera con ese nombre en la misma facultad");
        c.setNombreCarrera(dto.nombreCarrera());
        c.setIdFacultad(dto.idFacultad());
        c.setIdPersonaCoordinador(dto.idPersonaCoordinador());
        c = repo.save(c);
        return new CarreraDTO(c.getIdCarrera(), c.getNombreCarrera(), c.getIdFacultad(), c.getIdPersonaCoordinador());
    }

    @Transactional
    public void delete(Integer id){
        try{
            repo.deleteById(id);
        }catch (DataIntegrityViolationException ex){
            throw new DataIntegrityViolationException("No se puede eliminar la carrera: tiene materias o estudiantes asociados");
        }
    }

    public CarreraDTO get(Integer id){
        Carrera c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada"));
        return new CarreraDTO(c.getIdCarrera(), c.getNombreCarrera(), c.getIdFacultad(), c.getIdPersonaCoordinador());
    }
}
