package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.AsistenciaRow;
import com.uteq.SCLI.repository.EstudianteAsistenciaRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

@Service
public class EstudianteAsistenciaService {

    private final EstudianteAsistenciaRepository repo;

    public EstudianteAsistenciaService(EstudianteAsistenciaRepository repo) {
        this.repo = repo;
    }

    public List<AsistenciaRow> listar(Integer idEst, Integer idMateria, Date desde, Date hasta) {
        return repo.findAsistenciaEst(idEst, idMateria, desde, hasta);
    }
}