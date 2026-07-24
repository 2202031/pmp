package com.example.pmp_back.model;

import jakarta.persistence.*;

@Entity
@Table(name = "supervision")
public class Supervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_supervision")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String folio;

    @Column(nullable = false)
    private float progreso;

    @Column(name = "area_delimitada", length = 100)
    private String areaDelimitada;

    @Column(name = "equipo_security", length = 100)
    private String equipoSecurity;

    @Column(name = "corte_potencial", length = 100)
    private String cortePotencial;

    @Column(name = "deteccion_potencial", length = 100)
    private String deteccionPotencial;

    @Column(name = "metales_cero", length = 100)
    private String metalesCero;

    @Column(name = "actividades_vida", length = 100)
    private String actividadesVida;

    @Column(name = "rim_correcto", length = 100)
    private String rimCorrecto;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // --- GETTERS Y SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public float getProgreso() { return progreso; }
    public void setProgreso(float progreso) { this.progreso = progreso; }

    public String getAreaDelimitada() { return areaDelimitada; }
    public void setAreaDelimitada(String areaDelimitada) { this.areaDelimitada = areaDelimitada; }

    public String getEquipoSecurity() { return equipoSecurity; }
    public void setEquipoSecurity(String equipoSecurity) { this.equipoSecurity = equipoSecurity; }

    public String getCortePotencial() { return cortePotencial; }
    public void setCortePotencial(String cortePotencial) { this.cortePotencial = cortePotencial; }

    public String getDeteccionPotencial() { return deteccionPotencial; }
    public void setDeteccionPotencial(String deteccionPotencial) { this.deteccionPotencial = deteccionPotencial; }

    public String getMetalesCero() { return metalesCero; }
    public void setMetalesCero(String metalesCero) { this.metalesCero = metalesCero; }

    public String getActividadesVida() { return actividadesVida; }
    public void setActividadesVida(String actividadesVida) { this.actividadesVida = actividadesVida; }

    public String getRimCorrecto() { return rimCorrecto; }
    public void setRimCorrecto(String rimCorrecto) { this.rimCorrecto = rimCorrecto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
