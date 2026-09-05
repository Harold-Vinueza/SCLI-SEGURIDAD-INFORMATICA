package com.uteq.SCLI.service;

import com.uteq.SCLI.model.Piso;
import com.uteq.SCLI.repository.PisoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PisoServiceImpl implements PisoService {

    private final PisoRepository pisoRepo;

    @Override
    public List<Piso> listarTodos() {
        return pisoRepo.findAll(Sort.by(Sort.Direction.ASC, "numeroPiso"));
    }

    @Override
    public Optional<Piso> buscarPorId(Integer id) {
        return pisoRepo.findById(id);
    }

    @Override
    @Transactional
    public Piso guardar(Piso p) {
        return pisoRepo.save(p);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        pisoRepo.deleteById(id);
    }
}
