package com.uteq.SCLI.dto;


public class CrearUsuarioResponse {
    private boolean ok;
    private Integer idUsuario; // puede venir null cuando ok=false
    private String mensaje;

    public CrearUsuarioResponse() {}
    public CrearUsuarioResponse(boolean ok, Integer idUsuario, String mensaje) {
        this.ok = ok;
        this.idUsuario = idUsuario;
        this.mensaje = mensaje;
    }
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}