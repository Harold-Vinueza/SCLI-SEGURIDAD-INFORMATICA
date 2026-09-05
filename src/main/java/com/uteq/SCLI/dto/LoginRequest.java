package com.uteq.SCLI.dto;
import jakarta.validation.constraints.NotBlank;


public class LoginRequest {

   @NotBlank(message = "Completa este campo")
    private String correo;

    @NotBlank(message = "Completa este campo")
    private String contrasena;

    // Getters y setters

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
