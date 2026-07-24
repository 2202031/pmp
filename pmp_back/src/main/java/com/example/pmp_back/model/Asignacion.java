package com.example.pmp_back.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asigar_super")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asign_super")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String folio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_programada", length = 10)
    private String horaProgramada;

    @Column(name = "referencia", nullable = false, length = 200)
    private String lugar;

    @Column(nullable = false, length = 100)
    private String prioridad;

    @Column(nullable = false, length = 100)
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Username de los técnicos de apoyo, separados por coma. Ej: "juan.perez,ana.lopez"
    @Column(name = "personal_apoyo", columnDefinition = "TEXT")
    private String personalApoyo;

    @Column(nullable = false, length = 100)
    private String estado = "Asignada";

    @Column(name = "id_administrador")
    private Integer idAdministrador;

    @Column(name = "id_usuarios")
    private Integer idSupervisor;

    @Column(name = "hora_supervision", insertable = false, updatable = false)
    private LocalDateTime horaSupervisionRegistro;

    // --- GETTERS Y SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getHoraProgramada() { return horaProgramada; }
    public void setHoraProgramada(String horaProgramada) { this.horaProgramada = horaProgramada; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getPersonalApoyo() { return personalApoyo; }
    public void setPersonalApoyo(String personalApoyo) { this.personalApoyo = personalApoyo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getIdAdministrador() { return idAdministrador; }
    public void setIdAdministrador(Integer idAdministrador) { this.idAdministrador = idAdministrador; }

    public Integer getIdSupervisor() { return idSupervisor; }
    public void setIdSupervisor(Integer idSupervisor) { this.idSupervisor = idSupervisor; }

    public LocalDateTime getHoraSupervisionRegistro() { return horaSupervisionRegistro; }
}
