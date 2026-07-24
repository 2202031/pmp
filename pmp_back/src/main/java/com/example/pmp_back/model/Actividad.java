package com.example.pmp_back.model;

import jakarta.persistence.*;

// Actividad libre del calendario (nota/evento) que el Administrador puede crear,
// opcionalmente ligada a una supervisión (folio) o asignada a un nombre libre.
@Entity
@Table(name = "calendario_actividades")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_actividad", nullable = false, unique = true, length = 50)
    private String idActividad;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 20)
    private String fecha;

    @Column(length = 10)
    private String hora;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 50)
    private String prioridad;

    // Folio opcional de una supervisión relacionada (sin FK estricta: puede
    // referirse a una supervisión que ya no exista si se borró después).
    @Column(length = 50)
    private String folio;

    // Nombre libre de la persona asignada (no siempre coincide con un username real)
    @Column(length = 200)
    private String tecnico;

    @Column(length = 50)
    private String estado;

    @Column(name = "fecha_registro", length = 30)
    private String fechaRegistro;

    @Column(name = "id_administrador")
    private Integer idAdministrador;

    // --- GETTERS Y SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getIdActividad() { return idActividad; }
    public void setIdActividad(String idActividad) { this.idActividad = idActividad; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public String getTecnico() { return tecnico; }
    public void setTecnico(String tecnico) { this.tecnico = tecnico; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Integer getIdAdministrador() { return idAdministrador; }
    public void setIdAdministrador(Integer idAdministrador) { this.idAdministrador = idAdministrador; }
}
