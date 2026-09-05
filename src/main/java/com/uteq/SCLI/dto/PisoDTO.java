package com.uteq.SCLI.dto;



import lombok.Data;

@Data
public class PisoDTO {
     private Integer idPiso;       // opcional
    private Integer numeroPiso;
    private String descripcion;

    public Integer getIdPiso() { return idPiso; }
    public void setIdPiso(Integer idPiso) { this.idPiso = idPiso; }

    public Integer getNumeroPiso() { return numeroPiso; }
    public void setNumeroPiso(Integer numeroPiso) { this.numeroPiso = numeroPiso; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
