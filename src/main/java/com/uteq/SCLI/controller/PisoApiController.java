package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.PisoDTO;
import com.uteq.SCLI.model.Piso;
import com.uteq.SCLI.service.PisoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/pisos")
public class PisoApiController {

    private final PisoService pisoService;

    @GetMapping
    public List<Piso> listar() {
        return pisoService.listarTodos();
    }

    @GetMapping("{id}")
    public Piso obtener(@PathVariable Integer id) {
        return pisoService.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Piso no encontrado"));
    }

    @PostMapping
    public ResponseEntity<Piso> crear(@Valid @RequestBody PisoDTO dto) {
        Piso p = new Piso();
        p.setNumeroPiso(dto.getNumeroPiso());
        p.setDescripcion(dto.getDescripcion());
        Piso created = pisoService.guardar(p);
        return ResponseEntity
                .created(URI.create("/api/admin/pisos/" + created.getIdPiso()))
                .body(created);
    }

    @PutMapping("{id}")
    public Piso actualizar(@PathVariable Integer id, @Valid @RequestBody PisoDTO dto) {
        Piso p = pisoService.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Piso no encontrado"));
        p.setNumeroPiso(dto.getNumeroPiso());
        p.setDescripcion(dto.getDescripcion());
        return pisoService.guardar(p);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        pisoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
