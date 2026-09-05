package com.uteq.SCLI.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "materia",
    uniqueConstraints = {
        @UniqueConstraint(name = "materia_cod_materia_key", columnNames = "cod_materia")
    }
)
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_materia")
    private Integer idMateria;

    // Campo legado en la tabla (nombre de carrera en texto)
    @Column(name = "carrera", length = 255)
    private String carrera;

    @Column(name = "cod_materia", nullable = false, length = 50)
    private String codMateria;

    @Column(name = "nombre_materia", nullable = false, length = 255)
    private String nombreMateria;

    @Column(name = "semestre", length = 50)
    private String semestre;

    // FK -> carrera.id_carrera (en tu BD puede venir null)
    @Column(name = "id_carrera")
    private Integer idCarrera;

    // --- Relación (solo lectura) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera", insertable = false, updatable = false)
    private Carrera carreraRef;

    // Getters & Setters
    public Integer getIdMateria() { return idMateria; }
    public void setIdMateria(Integer idMateria) { this.idMateria = idMateria; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public String getCodMateria() { return codMateria; }
    public void setCodMateria(String codMateria) { this.codMateria = codMateria; }

    public String getNombreMateria() { return nombreMateria; }
    public void setNombreMateria(String nombreMateria) { this.nombreMateria = nombreMateria; }

    public String getSemestre() { return semestre; }
    public void setSemestre(String semestre) { this.semestre = semestre; }

    public Integer getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Integer idCarrera) { this.idCarrera = idCarrera; }

    public Carrera getCarreraRef() { return carreraRef; }
}
