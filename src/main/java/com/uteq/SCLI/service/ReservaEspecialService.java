package com.uteq.SCLI.service;

import com.uteq.SCLI.model.ReservaEspecial;
import com.uteq.SCLI.repository.ReservaEspecialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaEspecialService {

    private final ReservaEspecialRepository repo;
    private final CryptoService crypto;

    public ReservaEspecialService(ReservaEspecialRepository repo, CryptoService crypto) {
        this.repo = repo;
        this.crypto = crypto;
    }

    public List<ReservaEspecial> listar() {
        List<ReservaEspecial> lista = repo.findAll();
        lista.forEach(r -> r.setObservaciones(crypto.desencriptar(r.getObservaciones())));
        return lista;
    }

    public Optional<ReservaEspecial> porId(Integer id) {
        return repo.findById(id).map(r -> {
            r.setObservaciones(crypto.desencriptar(r.getObservaciones()));
            return r;
        });
    }

    public ReservaEspecial guardar(ReservaEspecial r) {
        if (r.getPublicado() == null) r.setPublicado(false);
        String observacionesPlano = r.getObservaciones();
        r.setObservaciones(crypto.encriptar(observacionesPlano));
        ReservaEspecial guardado = repo.save(r);
        guardado.setObservaciones(observacionesPlano); // Devuelve el objeto con el texto plano, no el cifrado
        return guardado;
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
        List<ReservaEspecial> lista = repo.findByPublicadoTrueOrderByFechaInicioDesc();
        lista.forEach(r -> r.setObservaciones(crypto.desencriptar(r.getObservaciones())));
        return lista;
    }

    public List<ReservaEspecial> ultimas5Publicadas() {
        List<ReservaEspecial> lista = repo.findTop5ByPublicadoTrueOrderByFechaInicioDesc();
        lista.forEach(r -> r.setObservaciones(crypto.desencriptar(r.getObservaciones())));
        return lista;
    }
}