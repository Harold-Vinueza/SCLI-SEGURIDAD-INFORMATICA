// Paquete donde se ubica el manejador global de excepciones
package com.uteq.SCLI.exception;

// Clases para construir respuestas HTTP con estado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Permite interceptar excepciones desde cualquier @Controller o @RestController
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Para enviar datos a la vista Thymeleaf
import org.springframework.ui.Model;

// Para construir respuestas con fecha y detalles
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Maneja excepciones cuando el usuario ingresa credenciales incorrectas
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<String> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        // Devuelve un mensaje con código 401 (Unauthorized)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    // Captura cualquier excepción no controlada que no tenga manejador específico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarExcepcionesGenerales(Exception ex) {
        // Crea un mapa con detalles del error
        Map<String, Object> error = new HashMap<>();
        error.put("mensaje", "Error interno del servidor");  // Mensaje genérico para el usuario
        error.put("detalle", ex.getMessage());              // Mensaje técnico para el desarrollador
        error.put("fecha", LocalDateTime.now());            // Marca de tiempo del error

        // Devuelve código 500 con el cuerpo detallado en formato JSON
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Maneja errores de acceso a vistas protegidas (rol incorrecto o sin sesión)
    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public String manejarAccesoNoAutorizado(AccesoNoAutorizadoException ex, Model model) {
        // Se pasa el mensaje de error a la vista como atributo
        model.addAttribute("error", ex.getMessage());

        // Retorna la vista personalizada: templates/error/acceso-denegado.html
        return "error/acceso-denegado";
    }

    // Maneja errores cuando no se puede cargar correctamente una vista HTML
    @ExceptionHandler(VistaNoDisponibleException.class)
    public String manejarVistaNoDisponible(VistaNoDisponibleException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        // Retorna una vista de error: templates/error/vista-no-disponible.html
        return "error/vista-no-disponible";
    }
}
