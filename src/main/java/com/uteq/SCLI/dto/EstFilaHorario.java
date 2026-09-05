package com.uteq.SCLI.dto;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class EstFilaHorario {
    private final LocalTime inicio;
    private final LocalTime fin;
    // clave = "Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"
    private final Map<String, EstCeldaHorario> celdas = new LinkedHashMap<>();

    public EstFilaHorario(LocalTime inicio, LocalTime fin) {
        this.inicio = inicio;
        this.fin = fin;
    }

    public LocalTime getInicio() { return inicio; }
    public LocalTime getFin() { return fin; }
    public Map<String, EstCeldaHorario> getCeldas() { return celdas; }
}