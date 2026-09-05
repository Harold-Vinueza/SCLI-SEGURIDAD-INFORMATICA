// FacultadController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.FacultadDTO;
import com.uteq.SCLI.service.FacultadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facultades")
public class FacultadController {
    private final FacultadService service;
    public FacultadController(FacultadService service){ this.service = service; }

    @GetMapping
    public Page<FacultadDTO> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    public FacultadDTO get(@PathVariable Integer id){ return service.get(id); }

    @PostMapping
    public FacultadDTO create(@Valid @RequestBody FacultadDTO dto){ return service.create(dto); }

    @PutMapping("/{id}")
    public FacultadDTO update(@PathVariable Integer id, @Valid @RequestBody FacultadDTO dto){ return service.update(id, dto); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
