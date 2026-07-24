package com.example.pmp_back.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String emailUsuario;

    @Column(nullable = false, length = 50)
    private String rolUsuario;

    @Column(nullable = false, length = 150)
    private String titulo;

   @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;
    
    @Column(length = 50)
    private String folio;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Getters y Setters tradicionales
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }

    public String getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(String rolUsuario) { this.rolUsuario = rolUsuario; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public String getFechaFormateada() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return fechaCreacion.format(formatter);
    }
}