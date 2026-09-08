package com.uteq.SCLI.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearSolicitudRequest {

    @NotNull(message = "Debes seleccionar un horario")
    private Integer idHorario;

    @NotBlank(message = "El motivo no puede estar vacío")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;

    @NotBlank(message = "Debes indicar el tipo de solicitud")
    private String tipoSolicitud; // "Nueva" | "Cambio" | "Temporal"

    private Integer idAdminPiso;   // admin de piso a quien se enruta (si aplica)
    private Integer idMateria;     // opcional: tu tabla guarda 'materia' texto; el service lo resuelve

    @NotNull(message = "Debes indicar la fecha de uso")
    @FutureOrPresent(message = "La fecha de uso no puede ser en el pasado")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaUso;
}