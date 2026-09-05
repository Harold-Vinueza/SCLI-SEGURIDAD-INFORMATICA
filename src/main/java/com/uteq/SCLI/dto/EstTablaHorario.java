package com.uteq.SCLI.dto;

import java.util.ArrayList;
import java.util.List;

public class EstTablaHorario {
    private final String periodo;
    private final List<EstFilaHorario> filas = new ArrayList<>();

    public EstTablaHorario(String periodo) { this.periodo = periodo; }
    public String getPeriodo() { return periodo; }
    public List<EstFilaHorario> getFilas() { return filas; }
}