package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.MateriaDTO;
import com.uteq.SCLI.model.Materia;
import com.uteq.SCLI.repository.MateriaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MateriaService {
    private final MateriaRepository repo;
    public MateriaService(MateriaRepository repo){ this.repo = repo; }

    public Page<MateriaDTO> list(Integer idCarrera, String q, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("idMateria").ascending());
        List<Materia> base = repo.search(idCarrera, (q == null ? "" : q.trim()));
        int from = Math.min(page*size, base.size());
        int to = Math.min(from+size, base.size());
        Page<Materia> pageRes = new PageImpl<>(base.subList(from, to), pageable, base.size());
        return pageRes.map(this::toDto);
    }

    public MateriaDTO get(Integer id){
        Materia m = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        return toDto(m);
    }

    @Transactional
    public MateriaDTO create(MateriaDTO dto){
        if (repo.existsByCodMateriaIgnoreCase(dto.codMateria()))
            throw new DataIntegrityViolationException("El código de materia ya existe");
        Materia m = new Materia();
        m.setCarrera(dto.carrera());
        m.setCodMateria(dto.codMateria());
        m.setNombreMateria(dto.nombreMateria());
        m.setSemestre(dto.semestre());
        m.setIdCarrera(dto.idCarrera());
        m = repo.save(m);
        return toDto(m);
    }

    @Transactional
    public MateriaDTO update(Integer id, MateriaDTO dto){
        Materia m = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        if (!m.getCodMateria().equalsIgnoreCase(dto.codMateria()) && repo.existsByCodMateriaIgnoreCase(dto.codMateria()))
            throw new DataIntegrityViolationException("El código de materia ya existe");
        m.setCarrera(dto.carrera());
        m.setCodMateria(dto.codMateria());
        m.setNombreMateria(dto.nombreMateria());
        m.setSemestre(dto.semestre());
        m.setIdCarrera(dto.idCarrera());
        m = repo.save(m);
        return toDto(m);
    }

    @Transactional
    public void delete(Integer id){
        try{
            repo.deleteById(id);
        }catch (DataIntegrityViolationException ex){
            throw new DataIntegrityViolationException("No se puede eliminar la materia: está referenciada por asignaciones/horarios");
        }
    }

    private MateriaDTO toDto(Materia m){
        return new MateriaDTO(
                m.getIdMateria(),
                m.getCarrera(),
                m.getCodMateria(),
                m.getNombreMateria(),
                m.getSemestre(),
                m.getIdCarrera()
        );
    }
}
