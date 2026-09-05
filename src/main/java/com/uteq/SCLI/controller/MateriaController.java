// MateriaController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.MateriaDTO;
import com.uteq.SCLI.service.MateriaService;
import com.uteq.SCLI.service.DocenteMateriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService service;
    private final DocenteMateriaService dmService;

    @GetMapping
    public Page<MateriaDTO> list(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return service.list(idCarrera, q, page, size);
    }

    @GetMapping("/{id}")
    public MateriaDTO get(@PathVariable Integer id){ return service.get(id); }

    @PostMapping
    public MateriaDTO create(@Valid @RequestBody MateriaDTO dto){ return service.create(dto); }

    @PutMapping("/{id}")
    public MateriaDTO update(@PathVariable Integer id, @Valid @RequestBody MateriaDTO dto){ return service.update(id, dto); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

     // ===== Docente responsable de la materia =====

  // Devuelve { "idDocente": 123 } o {} si no hay
  @GetMapping("/{id}/docente")
  public Map<String, Integer> getDocente(@PathVariable Integer id) {
    Integer d = dmService.findDocenteByMateria(id);
    return d == null ? Map.of() : Map.of("idDocente", d);
    // Si prefieres siempre la clave, usa: return Map.of("idDocente", d);
  }

  // Body: { "idDocente": 123 }
  @PutMapping("/{id}/docente")
  public ResponseEntity<Void> setDocente(@PathVariable Integer id,
                                         @RequestBody Map<String,Integer> body) {
    Integer idDocente = body.get("idDocente");
    if (idDocente == null) return ResponseEntity.badRequest().build();
    dmService.asignarDocente(id, idDocente);
    return ResponseEntity.noContent().build();
  }
}
/*import com.uteq.SCLI.dto.MateriaDTO;
import com.uteq.SCLI.service.MateriaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uteq.SCLI.service.DocenteMateriaService;
import java.util.Map;

@RestController
@RequestMapping("/api/materias")
public class MateriaController {
    private final MateriaService service;
    public MateriaController(MateriaService service){ this.service = service; }

    @GetMapping
    public Page<MateriaDTO> list(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return service.list(idCarrera, q, page, size);
    }

    @GetMapping("/{id}")
    public MateriaDTO get(@PathVariable Integer id){ return service.get(id); }

    @PostMapping
    public MateriaDTO create(@Valid @RequestBody MateriaDTO dto){ return service.create(dto); }

    @PutMapping("/{id}")
    public MateriaDTO update(@PathVariable Integer id, @Valid @RequestBody MateriaDTO dto){ return service.update(id, dto); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }




}*/
