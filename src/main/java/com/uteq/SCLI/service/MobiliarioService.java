package com.uteq.SCLI.service;

import com.uteq.SCLI.model.Laboratorio;
import com.uteq.SCLI.model.Mobiliario;
import com.uteq.SCLI.repository.LaboratorioRepository;
import com.uteq.SCLI.repository.MobiliarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MobiliarioService {
    @Autowired private MobiliarioRepository mobiliarioRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;

    public List<Mobiliario> listar(String q, Integer labId, String estado) {
        if (labId != null) {
            Laboratorio lab = laboratorioRepository.findById(labId).orElse(null);
            if (lab != null) return mobiliarioRepository.findByLaboratorio(lab);
        }
        if (estado != null && !estado.isBlank()) {
            return mobiliarioRepository.findByEstadoIgnoreCase(estado.trim());
        }
        if (q != null && !q.isBlank()) {
            return mobiliarioRepository.findByTipoMobiliarioContainingIgnoreCase(q.trim());
        }
        return mobiliarioRepository.findAll();
    }

    public Mobiliario guardarResolviendoLaboratorio(Mobiliario m) {
        if (m.getLaboratorio() != null && m.getLaboratorio().getIdLaboratorio() != null) {
            Laboratorio lab = laboratorioRepository.findById(m.getLaboratorio().getIdLaboratorio()).orElse(null);
            m.setLaboratorio(lab);
        } else {
            m.setLaboratorio(null);
        }
        try {
            return mobiliarioRepository.save(m);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("No se pudo guardar el mobiliario.");
        }
    }

    public Mobiliario editar(Integer id, Mobiliario data) {
        Mobiliario existente = mobiliarioRepository.findById(id).orElse(null);
        if (existente == null) throw new IllegalArgumentException("El mobiliario no existe.");
        Laboratorio lab = null;
        if (data.getLaboratorio() != null && data.getLaboratorio().getIdLaboratorio() != null) {
            lab = laboratorioRepository.findById(data.getLaboratorio().getIdLaboratorio()).orElse(null);
        }
        existente.setTipoMobiliario(data.getTipoMobiliario());
        existente.setCantidad(data.getCantidad());
        existente.setEstado(data.getEstado());
        existente.setLaboratorio(lab);
        return guardarResolviendoLaboratorio(existente);
    }

    public void eliminar(Integer id) {
        if (id == null) throw new IllegalArgumentException("Id inválido.");
        mobiliarioRepository.deleteById(id);
    }

    public Mobiliario buscarPorId(Integer id) {
    return mobiliarioRepository.findById(id).orElse(null);
}

}