// src/main/java/com/uteq/SCLI/service/PersonaService.java
package com.uteq.SCLI.service;

import com.uteq.SCLI.repository.PersonaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository repo;

    /**
     * Devuelve filas: [ id_persona, nombres, apellidos ]
     * Si q es null/blank, lista todo (con paginación).
     */
    @Transactional
    public List<Object[]> buscar(String q, Integer limit, Integer offset) {
        int lim = (limit == null || limit < 1) ? 10 : limit;
        int off = (offset == null || offset < 0) ? 0  : offset;
        return repo.buscar(q, lim, off);
    }

    @Transactional
    public long contar(String q) {
        return repo.contar(q);
    }
}
