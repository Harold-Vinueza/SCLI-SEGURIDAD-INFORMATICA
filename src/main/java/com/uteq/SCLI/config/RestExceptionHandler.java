// src/main/java/com/uteq/SCLI/config/RestExceptionHandler.java
package com.uteq.SCLI.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> handleFK(DataIntegrityViolationException ex) {
    String msg = deepestMessage(ex);

    // Traducciones amigables según FK (ajusta a tus nombres reales si difieren)
    if (msg != null) {
      if (msg.contains("estudiante_id_carrera_fkey")) {
        msg = "No se puede eliminar la carrera: hay estudiantes asociados.";
      } else if (msg.contains("materia_id_carrera_fkey")) {
        msg = "No se puede eliminar la carrera: hay materias asociadas.";
      } else if (msg.contains("carrera_id_facultad_fkey")) {
        msg = "No se puede eliminar la facultad: hay carreras asociadas.";
      } else if (msg.toLowerCase().contains("foreign key")) {
        msg = "Operación no permitida: existen registros dependientes.";
      }
    }

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("ok", false, "error", msg != null ? msg : "Restricción de integridad."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getAllErrors().stream()
        .findFirst().map(e -> e.getDefaultMessage()).orElse("Datos inválidos.");
    return ResponseEntity.badRequest().body(Map.of("ok", false, "error", msg));
  }

  private String deepestMessage(Throwable ex) {
    Throwable t = ex;
    String last = null;
    while (t != null) {
      if (t.getMessage() != null) last = t.getMessage();
      if (t instanceof SQLException) break;
      t = t.getCause();
    }
    return last;
  }
}
