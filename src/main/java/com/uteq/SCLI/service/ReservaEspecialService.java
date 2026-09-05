package com.uteq.SCLI.service;

import com.uteq.SCLI.model.ReservaEspecial;
import com.uteq.SCLI.repository.ReservaEspecialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaEspecialService {

    private final ReservaEspecialRepository repo;

    public ReservaEspecialService(ReservaEspecialRepository repo) {
        this.repo = repo;
    }

    public List<ReservaEspecial> listar() {
        return repo.findAll();
    }

    public Optional<ReservaEspecial> porId(Integer id) {
        return repo.findById(id);
    }

    public ReservaEspecial guardar(ReservaEspecial r) {
        if (r.getPublicado() == null) r.setPublicado(false);
        return repo.save(r);
    }

    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

    public void togglePublicar(Integer id) {
        repo.findById(id).ifPresent(r -> {
            r.setPublicado(!Boolean.TRUE.equals(r.getPublicado()));
            repo.save(r);
        });
    }

    public List<ReservaEspecial> publicadas() {
        return repo.findByPublicadoTrueOrderByFechaInicioDesc();
    }

    public List<ReservaEspecial> ultimas5Publicadas() {
        return repo.findTop5ByPublicadoTrueOrderByFechaInicioDesc();
    }
}
