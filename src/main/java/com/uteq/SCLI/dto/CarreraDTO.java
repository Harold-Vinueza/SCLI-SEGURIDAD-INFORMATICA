package com.uteq.SCLI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CarreraDTO(
        Integer idCarrera,
        @NotBlank @Size(max = 100) String nombreCarrera,
        @NotNull Integer idFacultad,
        Integer idPersonaCoordinador
) {}
