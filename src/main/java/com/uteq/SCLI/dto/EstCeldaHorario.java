package com.uteq.SCLI.dto;



public class EstCeldaHorario {

    private final boolean ocupado;

    private final String texto;        // "IS101 - Algoritmos y programación"

    private final String laboratorio;  // "LAB-C1 (Lab Estructura de Datos)"

    private final String jornada;      // "Matutina" / "Vespertina"



    public EstCeldaHorario(boolean ocupado, String texto, String laboratorio, String jornada) {

        this.ocupado = ocupado;

        this.texto = texto;

        this.laboratorio = laboratorio;

        this.jornada = jornada;

    }



    public boolean isOcupado() { return ocupado; }

    public String getTexto() { return texto; }

    public String getLaboratorio() { return laboratorio; }

    public String getJornada() { return jornada; }

}