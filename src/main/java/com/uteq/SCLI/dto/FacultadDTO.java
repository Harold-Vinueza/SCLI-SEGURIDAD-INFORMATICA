package com.uteq.SCLI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FacultadDTO(
        Integer idFacultad,
        @NotBlank @Size(max = 100) String nombreFacultad
) {}
