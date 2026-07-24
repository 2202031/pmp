package com.example.pmp_back.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios") 
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuarios") 
    private Integer idUsuarios;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String email;

    @Column(nullable = false)
    private String rpe;

    @Column(name = "contraseña", nullable = false) 
    private String contrasenia;

    private String telefono;

    // NUEVO: Campo zona agregado formalmente
    private String zona;

    @Column(name = "id_rol")
    private Integer idRol;
    
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    // --- CONSTRUCTORES ---
    public Usuario() {
    }

    public Usuario(Integer idUsuarios, String nombre, String email, String rpe, String contrasenia, String telefono, String zona, Integer idRol, String username) {
        this.idUsuarios = idUsuarios;
        this.nombre = nombre;
        this.email = email;
        this.rpe = rpe;
        this.contrasenia = contrasenia;
        this.telefono = telefono;
        this.zona = zona;
        this.idRol = idRol;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // --- GETTERS Y SETTERS ---
    public Integer getIdUsuarios() { return idUsuarios; }
    public void setIdUsuarios(Integer idUsuarios) { this.idUsuarios = idUsuarios; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRpe() { return rpe; }
    public void setRpe(String rpe) { this.rpe = rpe; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    
    
    
    
}