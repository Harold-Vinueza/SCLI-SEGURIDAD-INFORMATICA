// src/main/java/com/uteq/SCLI/controller/PersonaApiController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.PersonaDto;
import com.uteq.SCLI.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/personas")
public class PersonaApiController {

    private final PersonaService service;

    @GetMapping
    public Map<String, Object> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // saneo de parámetros
        int limit  = Math.min(Math.max(size, 1), 50);
        int safePg = Math.max(page, 0);
        int offset = safePg * limit;

        // tratar q vacío como null para que el repo liste todo
        String filtro = (q == null || q.trim().isEmpty()) ? null : q.trim();

        // filas crudas: [id_persona, nombres, apellidos]
        List<Object[]> filas = service.buscar(filtro, limit, offset);

        // mapear a DTO
        List<PersonaDto> personas = filas.stream()
                .map(r -> new PersonaDto(
                        (Integer) r[0],
                        r[1] != null ? r[1].toString() : "",
                        r[2] != null ? r[2].toString() : ""
                ))
                .collect(Collectors.toList());

        long total = service.contar(filtro);

        return Map.of(
                "data", personas,
                "total", total,
                "page", safePg,
                "size", limit
        );
    }
}
