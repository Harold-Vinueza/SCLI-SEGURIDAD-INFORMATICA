package com.uteq.SCLI.controller;

import com.uteq.SCLI.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/_mail")
@ConditionalOnProperty(name = "app.mail.test.enabled", havingValue = "true", matchIfMissing = false)
public class MailTestController {

    private final EmailService emailService;

    /**
     * Ejemplo: GET http://localhost:8081/_mail/ping?to=tu_correo@uni.edu
     * Devuelve "OK" si el correo se pudo enviar.
     */
    @GetMapping("/ping")
    public String ping(@RequestParam String to) throws Exception {
        emailService.enviarCredenciales(
                to,
                "Prueba",
                "user_demo",
                "pass_demo",
                "Demo"
        );
        return "OK";
    }
}
