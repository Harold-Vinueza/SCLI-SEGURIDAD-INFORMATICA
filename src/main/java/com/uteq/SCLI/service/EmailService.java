package com.uteq.SCLI.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:no-reply@scli.local}}")
private String defaultFrom;


    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public void enviarCredenciales(String to, String nombrePersona,
                                   String username, String rawPassword,
                                   String rolNombre) throws Exception {
        if (!mailEnabled) {
            log.warn("Email deshabilitado por configuración; no se envían credenciales a {}", to);
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(defaultFrom);
        helper.setTo(to);
        helper.setSubject("Tus credenciales de acceso - SCLI");

        // HTML simple (sin adjuntos)
        String html = """
            <div style="font-family:Arial,Helvetica,sans-serif">
              <h2>Bienvenido(a) al Sistema de Control de Laboratorios</h2>
              <p>Hola <b>%s</b>, se ha creado tu cuenta.</p>
              <p><b>Rol:</b> %s</p>
              <p><b>Usuario:</b> %s<br>
                 <b>Contraseña temporal:</b> %s</p>
              <p>Por seguridad, cámbiala al iniciar sesión.</p>
              <hr>
              <small>Si no reconoces este correo, ignóralo.</small>
            </div>
            """.formatted(nombrePersona != null ? nombrePersona : "Usuario",
                         rolNombre != null ? rolNombre : "(sin rol)",
                         username, rawPassword);

        helper.setText(html, true);
        mailSender.send(message);

        // IMPORTANTE: nunca hagas log de la contraseña
        log.info("Credenciales enviadas a {}", to);
    }
}
