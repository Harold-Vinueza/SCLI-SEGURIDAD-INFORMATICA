package com.uteq.SCLI.controller;

import com.uteq.SCLI.model.Equipo;
import com.uteq.SCLI.repository.LaboratorioRepository;
import com.uteq.SCLI.service.EquipoService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/equipos")

public class EquipoApiController {

    private final EquipoService equipoService;
    private final LaboratorioRepository laboratorioRepo;

    public EquipoApiController(EquipoService equipoService, LaboratorioRepository laboratorioRepo) {
        this.equipoService = equipoService;
        this.laboratorioRepo = laboratorioRepo;
    }

    // === LISTAR ===
    @GetMapping
    public List<EquipoRes> listar(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Integer labId,
                                  @RequestParam(required = false) String estado) {
        return equipoService.listar(q, labId, estado).stream().map(this::toRes).toList();
    }

    // === OBTENER POR ID ===
    @GetMapping("/{id}")
    public ResponseEntity<EquipoRes> obtener(@PathVariable Integer id) {
        var e = equipoService.buscarPorId(id);
        return (e == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(toRes(e));
    }

    // === CREAR ===
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody EquipoReq req) {
        var e = new Equipo();
        apply(req, e);
        try {
            var guardado = equipoService.guardar(e);
            return ResponseEntity.status(HttpStatus.CREATED).body(toRes(guardado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
    }

    // === EDITAR ===
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody EquipoReq req) {
        var e = equipoService.buscarPorId(id);
        if (e == null) return ResponseEntity.notFound().build();
        apply(req, e);
        try {
            var guardado = equipoService.guardar(e);
            return ResponseEntity.ok(toRes(guardado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
    }

    // === ELIMINAR ===
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        var e = equipoService.buscarPorId(id);
        if (e == null) return ResponseEntity.notFound().build();
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ----- helpers -----
    private void apply(EquipoReq r, Equipo e) {
        e.setCodigoEquipo(r.codigoEquipo());
        e.setTipoEquipo(r.tipoEquipo());
        e.setMarca(r.marca());
        e.setModelo(r.modelo());
        e.setEstado(r.estado());
        e.setLaboratorio(r.labId() == null ? null :
            laboratorioRepo.findById(r.labId()).orElse(null));
    }

    private EquipoRes toRes(Equipo e) {
        Integer labId = (e.getLaboratorio() != null) ? e.getLaboratorio().getIdLaboratorio() : null;
        String labNombre = (e.getLaboratorio() != null) ? e.getLaboratorio().getNombreLaboratorio() : null; // ajusta nombre
        return new EquipoRes(
            e.getIdEquipo(), e.getCodigoEquipo(), e.getTipoEquipo(),
            e.getMarca(), e.getModelo(), e.getEstado(), labId, labNombre
        );
    }

     @GetMapping("/vue")
    public String vistaVue() {
        return "dashboard/equipos-vue";
    }

    // DTOs simples 
    public record EquipoReq(String codigoEquipo, String tipoEquipo, String marca,
                            String modelo, String estado, Integer labId) {}
    public record EquipoRes(Integer idEquipo, String codigoEquipo, String tipoEquipo,
                            String marca, String modelo, String estado,
                            Integer labId, String laboratorioNombre) {}
}
