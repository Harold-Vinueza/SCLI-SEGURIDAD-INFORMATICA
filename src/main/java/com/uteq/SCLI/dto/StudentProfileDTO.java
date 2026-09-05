package com.uteq.SCLI.dto;

public class StudentProfileDTO {
    private Integer idPersona;
    private Integer idEstudiante;   // opcional (puede ser null si aún no existe fila en estudiante)
    private String  nombres;
    private String  apellidos;
    private String  correo;
    private String  telefono;
    private String  fotoUrl;

    // Estudiante
    private String  matricula;
    private String  nivel;
    private Integer idCarrera;      // FK hacia carrera
    private String  carreraNombre;  // útil para mostrar/buscar por nombre

    public Integer getIdPersona() { return idPersona; }
    public void setIdPersona(Integer idPersona) { this.idPersona = idPersona; }

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public Integer getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Integer idCarrera) { this.idCarrera = idCarrera; }

    public String getCarreraNombre() { return carreraNombre; }
    public void setCarreraNombre(String carreraNombre) { this.carreraNombre = carreraNombre; }
}