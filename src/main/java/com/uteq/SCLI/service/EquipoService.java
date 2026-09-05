package com.uteq.SCLI.service;

import com.uteq.SCLI.model.Equipo;
import com.uteq.SCLI.model.Laboratorio;
import com.uteq.SCLI.repository.EquipoRepository;
import com.uteq.SCLI.repository.LaboratorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipoService {

     @Autowired private EquipoRepository equipoRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;

    public List<Equipo> listar(String q, Integer labId, String estado) {
    if (labId != null) {
        Laboratorio lab = laboratorioRepository.findById(labId).orElse(null);
        if (lab != null) return equipoRepository.findByLaboratorio(lab);
    }
    if (estado != null && !estado.isBlank()) {
        String e = estado.trim();
        // Mapeo UI -> BD (ajústalo a tus valores reales)
        if (e.equalsIgnoreCase("activo"))        e = "Operativo";
        if (e.equalsIgnoreCase("mantenimiento")) e = "En reparación";
        return equipoRepository.findByEstadoIgnoreCase(e);
    }
    if (q != null && !q.isBlank()) {
        String s = q.trim();
        return equipoRepository
            .findByCodigoEquipoContainingIgnoreCaseOrTipoEquipoContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(
                s, s, s, s);
    }
    return equipoRepository.findAll();
}
    public Equipo guardar(Equipo e) {
        // Validar duplicado por aplicación
        String codigo = e.getCodigoEquipo() == null ? "" : e.getCodigoEquipo().trim();
        if (!codigo.isEmpty()) {
            Equipo existente = equipoRepository.findByCodigoEquipoIgnoreCase(codigo);
            if (existente != null && (e.getIdEquipo() == null || !existente.getIdEquipo().equals(e.getIdEquipo()))) {
                throw new IllegalArgumentException("El código de equipo ya existe.");
            }
        }
        try {
            return equipoRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("codigo_equipo")) {
                throw new IllegalArgumentException("El código de equipo ya existe.");
            }
            if (msg != null && (msg.toLowerCase().contains("id_laboratorio") || msg.toLowerCase().contains("laboratorio"))) {
                throw new IllegalArgumentException("Selecciona un laboratorio válido.");
            }
            throw new IllegalArgumentException("No se pudo guardar el equipo. Verifica los datos.");
        }
    }

    public Equipo buscarPorId(Integer id) {
        return equipoRepository.findById(id).orElse(null);
    }

    @Transactional
    public void eliminar(Integer id) {
    if (id == null) throw new IllegalArgumentException("Id inválido.");
    if (!equipoRepository.existsById(id)) {
        throw new IllegalStateException("El equipo ya no existe.");
    }
    try {
        equipoRepository.deleteById(id);
        // flush para forzar validación de FK ahora, no al final de la transacción
        equipoRepository.flush();
    } catch (DataIntegrityViolationException ex) {
        // p.ej. hay ReporteFallo que referencia al equipo
        throw new IllegalStateException(
            "No se puede eliminar: el equipo está referenciado por otros registros.");
    }
}

    public List<Laboratorio> laboratorios() {
        return laboratorioRepository.findAll();
    }


    public Equipo guardarResolviendoLaboratorio(Equipo e) {
        if (e.getLaboratorio() != null && e.getLaboratorio().getIdLaboratorio() != null) {
            Laboratorio lab = laboratorioRepository.findById(e.getLaboratorio().getIdLaboratorio()).orElse(null);
            e.setLaboratorio(lab);
        } else {
            e.setLaboratorio(null);
        }
        return guardar(e);
    }

    public Equipo editar(Integer id, Equipo data) {
        Equipo existente = buscarPorId(id);
        if (existente == null) throw new IllegalArgumentException("El equipo no existe.");

        Laboratorio lab = null;
        if (data.getLaboratorio() != null && data.getLaboratorio().getIdLaboratorio() != null) {
            lab = laboratorioRepository.findById(data.getLaboratorio().getIdLaboratorio()).orElse(null);
        }
        existente.setCodigoEquipo(data.getCodigoEquipo());
        existente.setTipoEquipo(data.getTipoEquipo());
        existente.setMarca(data.getMarca());
        existente.setModelo(data.getModelo());
        existente.setEstado(data.getEstado());
        existente.setLaboratorio(lab);
        return guardar(existente);
    }



}