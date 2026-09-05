// CarreraController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.CarreraDTO;
import com.uteq.SCLI.service.CarreraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carreras")
public class CarreraController {
    private final CarreraService service;
    public CarreraController(CarreraService service){ this.service = service; }

    @GetMapping
    public Page<CarreraDTO> list(
            @RequestParam(required = false) Integer idFacultad,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return service.list(idFacultad, q, page, size);
    }

    @GetMapping("/{id}")
    public CarreraDTO get(@PathVariable Integer id){ return service.get(id); }

    @PostMapping
    public CarreraDTO create(@Valid @RequestBody CarreraDTO dto){ return service.create(dto); }

    @PutMapping("/{id}")
    public CarreraDTO update(@PathVariable Integer id, @Valid @RequestBody CarreraDTO dto){ return service.update(id, dto); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
