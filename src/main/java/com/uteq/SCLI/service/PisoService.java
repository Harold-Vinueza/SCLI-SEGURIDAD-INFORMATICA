package com.uteq.SCLI.service;

import com.uteq.SCLI.model.Piso;
import java.util.List;
import java.util.Optional;

public interface PisoService {
    List<Piso> listarTodos();
    Optional<Piso> buscarPorId(Integer id);
    Piso guardar(Piso p);
    void eliminar(Integer id);
}
