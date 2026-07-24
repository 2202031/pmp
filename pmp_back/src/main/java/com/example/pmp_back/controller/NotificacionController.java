package com.example.pmp_back.controller;

import com.example.pmp_back.model.Notificacion;
import com.example.pmp_back.model.Usuario;
import com.example.pmp_back.repository.NotificacionRepository;
import com.example.pmp_back.repository.UsuarioRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // =========================================================================
    // 1. ENDPOINT PARA CREAR UNA NOTIFICACIÓN
    // =========================================================================
    @PostMapping("/crear")
    public ResponseEntity<?> guardarNotificacion(@RequestBody Map<String, Object> datos) {
        try {
            String username = Validador.texto(datos, "username");
            if (username.isEmpty()) username = Validador.texto(datos, "emailUsuario");
            String rolUsuario = Validador.texto(datos, "rolUsuario");
            String titulo = Validador.texto(datos, "titulo");
            String mensaje = Validador.texto(datos, "mensaje");
            String folio = Validador.texto(datos, "folio");

            // ---- VALIDACIÓN según columnas de la tabla notificaciones ----
            String error = Validador.obligatorio(username, "username", 100);
            if (error == null) error = Validador.obligatorio(rolUsuario, "rolUsuario", 50);
            if (error == null) error = Validador.obligatorio(titulo, "titulo", 150);
            if (error == null) error = Validador.obligatorio(mensaje, "mensaje", 5000);
            if (error == null) error = Validador.opcional(folio, "folio", 50);

            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            Notificacion nueva = new Notificacion();

            // Se guarda el username (si llega un nombre, se traduce automáticamente)
            nueva.setEmailUsuario(normalizarDestinatario(username));
            nueva.setRolUsuario(rolUsuario);
            nueva.setTitulo(titulo);
            nueva.setMensaje(mensaje);
            nueva.setLeida(false);

            if (!folio.isEmpty()) {
                nueva.setFolio(folio);
            }
            
            notificacionRepository.save(nueva);

            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Notificación creada de forma remota");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    // =========================================================================
    // 2. OBTENER NOTIFICACIONES POR USERNAME Y ROL
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<Notificacion>> obtenerNotificaciones(@RequestParam String username, @RequestParam String rol) {
        // Trae las notificaciones dirigidas a este usuario específico, MÁS las
        // dirigidas a "*" (cualquier usuario de ese rol, ej. cualquier administrador).
        List<Notificacion> propias = notificacionRepository.findByEmailUsuarioAndRolUsuarioOrderByIdDesc(username, rol);
        List<Notificacion> comodin = notificacionRepository.findByEmailUsuarioAndRolUsuarioOrderByIdDesc("*", rol);

        List<Notificacion> combinada = new java.util.ArrayList<>();
        combinada.addAll(propias);
        combinada.addAll(comodin);
        combinada.sort((a, b) -> b.getId().compareTo(a.getId()));

        return ResponseEntity.ok(combinada);
    }

    // =========================================================================
    // 3. MARCAR UNA NOTIFICACIÓN COMO LEÍDA
    // =========================================================================
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Integer id) {
        return notificacionRepository.findById(id).map(notif -> {
            notif.setLeida(true);
            notificacionRepository.save(notif);
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // 4. MARCAR TODAS COMO LEÍDAS
    // =========================================================================
    @PutMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas(@RequestParam String username, @RequestParam String rol) {
        // CORREGIDO: Buscamos usando el username de la sesión activa
        List<Notificacion> lista = notificacionRepository.findByEmailUsuarioAndRolUsuarioOrderByIdDesc(username, rol);
        for (Notificacion notif : lista) {
            if (!notif.isLeida()) {
                notif.setLeida(true);
                notificacionRepository.save(notif);
            }
        }
        Map<String, String> res = new HashMap<>();
        res.put("status", "success");
        return ResponseEntity.ok(res);
    }

    // =========================================================================
    // 5. ELIMINAR NOTIFICACIÓN
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarNotificacion(@PathVariable Integer id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
    }

    // Si el destinatario llega como nombre completo (ej. "Juan Pérez") en vez de
    // username (ej. "juan.perez"), lo traduce buscando en la tabla usuarios.
    // El comodín "*" se deja tal cual. Si no encuentra coincidencia, deja el valor original.
    private String normalizarDestinatario(String valor) {
        if (valor == null) return "";
        String v = valor.trim();
        if (v.isEmpty() || v.equals("*")) return v;

        // Si ya es un username existente, lo dejamos.
        Optional<Usuario> porUsername = usuarioRepository.findByUsername(v);
        if (porUsername.isPresent()) {
            return v;
        }

        // Si coincide con el nombre de algún usuario, devolvemos su username.
        for (Usuario u : usuarioRepository.findAll()) {
            if (u.getNombre() != null && u.getNombre().trim().equalsIgnoreCase(v)) {
                return u.getUsername();
            }
        }

        return v;
    }
}