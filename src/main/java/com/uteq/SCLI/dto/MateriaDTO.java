package com.uteq.SCLI.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

public record MateriaDTO(
        Integer idMateria,
        @Size(max = 255) String carrera,           // opcional/legacy en tu tabla
        @NotBlank @Size(max = 50) String codMateria,
        @NotBlank @Size(max = 255) String nombreMateria,
        @Size(max = 50) String semestre,
        Integer idCarrera                           // puede ser null (según tus datos actuales), pero recomendamos usarlo
) {}
