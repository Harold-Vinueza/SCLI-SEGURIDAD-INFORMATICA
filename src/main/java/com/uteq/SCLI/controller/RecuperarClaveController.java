package com.uteq.SCLI.controller;

import com.uteq.SCLI.repository.AuthRepository;
import com.uteq.SCLI.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class RecuperarClaveController {

    private static final Logger log = LoggerFactory.getLogger(RecuperarClaveController.class);

    private static final Pattern POLITICA_CLAVE = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,}$");

    private final AuthRepository authRepository;
    private final EmailService emailService;

    public RecuperarClaveController(AuthRepository authRepository, EmailService emailService) {
        this.authRepository = authRepository;
        this.emailService = emailService;
    }

    @GetMapping("/recuperar-clave")
    public String formSolicitar() {
        return "login/recuperar-clave";
    }

    @PostMapping("/recuperar-clave")
    @Transactional
    public String procesarSolicitud(@RequestParam String identificador, Model model) {
        // Mensaje genérico siempre, exista o no la cuenta (evita revelar qué usuarios existen)
        model.addAttribute("mensaje",
            "Si el usuario o correo existe, se ha enviado un enlace de recuperación.");

        try {
            AuthRepository.RecuperacionUsuarioView u = authRepository.buscarParaRecuperacion(identificador);
            if (u != null && u.getCorreo() != null) {
                String token = UUID.randomUUID().toString();
                Instant expiraEn = Instant.now().plus(30, ChronoUnit.MINUTES);
                authRepository.guardarTokenRecuperacion(u.getId_usuario(), token, expiraEn);
                emailService.enviarTokenRecuperacion(u.getCorreo(), u.getNombres(), token);
            }
        } catch (Exception ex) {
            log.warn("Error procesando solicitud de recuperación: {}", ex.getMessage());
            // No se revela el error al usuario, solo se registra
        }

        return "login/recuperar-clave";
    }

    @GetMapping("/reset-clave")
    public String formReset(@RequestParam String token, Model model) {
        AuthRepository.TokenRecuperacionView t = authRepository.buscarToken(token);
        if (t == null || Boolean.TRUE.equals(t.getUsado()) || t.getExpira_en().isBefore(Instant.now())) {
            model.addAttribute("error", "El enlace no es válido o ya expiró. Solicita uno nuevo.");
            return "login/reset-clave-invalido";
        }
        model.addAttribute("token", token);
        return "login/reset-clave";
    }

    @PostMapping("/reset-clave")
    @Transactional
    public String procesarReset(@RequestParam String token,
                                 @RequestParam String nueva,
                                 @RequestParam String repetir,
                                 Model model) {
        AuthRepository.TokenRecuperacionView t = authRepository.buscarToken(token);
        if (t == null || Boolean.TRUE.equals(t.getUsado()) || t.getExpira_en().isBefore(Instant.now())) {
            model.addAttribute("error", "El enlace no es válido o ya expiró. Solicita uno nuevo.");
            return "login/reset-clave-invalido";
        }

        if (!nueva.equals(repetir)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "login/reset-clave";
        }

        if (!POLITICA_CLAVE.matcher(nueva).matches()) {
            model.addAttribute("error",
                "La contraseña debe tener al menos 10 caracteres, incluyendo mayúscula, minúscula, número y símbolo.");
            model.addAttribute("token", token);
            return "login/reset-clave";
        }

        authRepository.actualizarClaveDirecta(t.getId_usuario(), nueva);
        authRepository.marcarTokenUsado(token);

        model.addAttribute("ok", true);
        return "login/reset-clave-exito";
    }
}