package com.example.pmp_back.controller;

import com.example.pmp_back.model.Asignacion;
import com.example.pmp_back.model.Usuario;
import com.example.pmp_back.repository.AsignacionRepository;
import com.example.pmp_back.repository.UsuarioRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/asignaciones")
@CrossOrigin(origins = "*")
public class AsignacionController {

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PostMapping("/guardar")
    public ResponseEntity<?> guardarAsignacion(@RequestBody Map<String, Object> datos) {
        try {
            // ---- VALIDACIÓN antes de tocar la base de datos ----
            String folioTexto = Validador.texto(datos, "folio");
            String fechaTexto = Validador.texto(datos, "fecha");
            String lugar = Validador.texto(datos, "lugar");
            String prioridad = Validador.texto(datos, "prioridad");
            String descripcion = Validador.texto(datos, "descripcion");
            String horaProgramada = Validador.texto(datos, "horaProgramada");
            String observaciones = Validador.texto(datos, "observaciones");
            String usuarioResponsable = Validador.texto(datos, "usuarioResponsable");

            String error = Validador.validarFolio(folioTexto);
            if (error == null) error = Validador.validarFecha(fechaTexto);
            if (error == null) error = Validador.obligatorio(lugar, "lugar", 200);
            if (error == null) error = Validador.opcional(prioridad, "prioridad", 100);
            if (error == null) error = Validador.obligatorio(descripcion, "descripcion", 100);
            if (error == null) error = Validador.opcional(horaProgramada, "horaProgramada", 10);
            if (error == null) error = Validador.obligatorio(usuarioResponsable, "usuarioResponsable", 255);

            if (error != null) {
                Map<String, String> badRequest = new HashMap<>();
                badRequest.put("status", "error");
                badRequest.put("message", error);
                return ResponseEntity.badRequest().body(badRequest);
            }

            String folio = folioTexto;
            LocalDate fecha;
            try {
                fecha = LocalDate.parse(fechaTexto, FORMATO_FECHA);
            } catch (Exception e) {
                Map<String, String> badRequest = new HashMap<>();
                badRequest.put("status", "error");
                badRequest.put("message", "La fecha no es válida. Debe tener el formato dd/MM/yyyy. Valor recibido: '" + fechaTexto + "'.");
                return ResponseEntity.badRequest().body(badRequest);
            }

            Optional<Usuario> supervisorOpt = buscarUsuarioPorRpeOUsername(usuarioResponsable);
            if (supervisorOpt.isEmpty()) {
                Map<String, String> errorResp = new HashMap<>();
                errorResp.put("status", "error");
                errorResp.put("message", "No se encontró al supervisor responsable indicado: '" + usuarioResponsable + "'.");
                return ResponseEntity.badRequest().body(errorResp);
            }

            Asignacion asignacion = asignacionRepository.findByFolio(folio).orElse(new Asignacion());
            asignacion.setFolio(folio);
            asignacion.setFecha(fecha);
            asignacion.setHoraProgramada(horaProgramada.isEmpty() ? null : horaProgramada);
            asignacion.setLugar(lugar);
            asignacion.setPrioridad(prioridad);
            asignacion.setDescripcion(descripcion);
            asignacion.setObservaciones(observaciones.isEmpty() ? null : observaciones);
            String personalApoyo = Validador.texto(datos, "personalApoyo");
            asignacion.setPersonalApoyo(personalApoyo.isEmpty() ? null : personalApoyo);
            asignacion.setIdSupervisor(supervisorOpt.get().getIdUsuarios());

            String adminUsername = Validador.texto(datos, "administradorUsername");
            if (!adminUsername.isEmpty()) {
                usuarioRepository.findByUsername(adminUsername)
                        .ifPresent(admin -> asignacion.setIdAdministrador(admin.getIdUsuarios()));
            }

            if (asignacion.getEstado() == null) {
                asignacion.setEstado("Asignada");
            }

            asignacionRepository.save(asignacion);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Asignación guardada correctamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error interno en el servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Lista todas las asignaciones (Administrador), o solo las de un supervisor si se manda ?username=
    @GetMapping
    public ResponseEntity<?> listarAsignaciones(@RequestParam(required = false) String username) {
        List<Asignacion> asignaciones;

        if (username != null && !username.isEmpty()) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            asignaciones = asignacionRepository.findByIdSupervisor(usuarioOpt.get().getIdUsuarios());
        } else {
            asignaciones = asignacionRepository.findAll();
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Asignacion a : asignaciones) {
            Map<String, Object> m = aMapaBasico(a);
            if (a.getIdSupervisor() != null) {
                usuarioRepository.findById(a.getIdSupervisor()).ifPresent(sup -> {
                    m.put("tecnico", sup.getNombre());
                    m.put("usernameSupervisor", sup.getUsername());
                });
            }
            resultado.add(m);
        }

        return ResponseEntity.ok(resultado);
    }

    // Datos listos para pintar el calendario en la app.
    @GetMapping("/calendario")
    public ResponseEntity<?> calendario(@RequestParam String rol, @RequestParam(required = false) String username) {
        List<Asignacion> asignaciones;

        if ("administrador".equalsIgnoreCase(rol)) {
            asignaciones = asignacionRepository.findAll();
        } else {
            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "username es obligatorio para el rol supervisor"));
            }
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            asignaciones = asignacionRepository.findByIdSupervisor(usuarioOpt.get().getIdUsuarios());
        }

        List<Map<String, Object>> eventos = new ArrayList<>();
        for (Asignacion a : asignaciones) {
            Map<String, Object> evento = new HashMap<>();
            evento.put("id", a.getId());
            evento.put("folio", a.getFolio());
            evento.put("titulo", a.getLugar());
            evento.put("lugar", a.getLugar());
            evento.put("fecha", a.getFecha() != null ? a.getFecha().format(FORMATO_FECHA) : "");
            evento.put("hora", a.getHoraProgramada() != null ? a.getHoraProgramada() : "");
            evento.put("horaProgramada", a.getHoraProgramada() != null ? a.getHoraProgramada() : "");
            evento.put("descripcion", a.getDescripcion());
            evento.put("observaciones", a.getObservaciones() != null ? a.getObservaciones() : "");
            evento.put("prioridad", a.getPrioridad());
            evento.put("estado", a.getEstado());
            evento.put("esSupervision", true);

            if (a.getIdSupervisor() != null) {
                usuarioRepository.findById(a.getIdSupervisor()).ifPresent(sup -> {
                    evento.put("tecnico", sup.getNombre());
                    evento.put("usernameSupervisor", sup.getUsername());
                });
            }

            eventos.add(evento);
        }

        return ResponseEntity.ok(eventos);
    }

    // Detalle de una asignación por folio (para pantallas de detalle/revisión)
    @GetMapping("/{folio}")
    public ResponseEntity<?> obtenerPorFolio(@PathVariable String folio) {
        Optional<Asignacion> asignacionOpt = asignacionRepository.findByFolio(folio);
        if (asignacionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "No existe una asignación con ese folio"));
        }

        Asignacion a = asignacionOpt.get();
        Map<String, Object> m = aMapaBasico(a);
        if (a.getIdSupervisor() != null) {
            usuarioRepository.findById(a.getIdSupervisor()).ifPresent(sup -> {
                m.put("tecnico", sup.getNombre());
                m.put("usernameSupervisor", sup.getUsername());
            });
        }
        if (a.getIdAdministrador() != null) {
            usuarioRepository.findById(a.getIdAdministrador()).ifPresent(admin -> m.put("administrador", admin.getNombre()));
        }

        return ResponseEntity.ok(m);
    }

    // Elimina una asignación por folio. Gracias a los ON DELETE CASCADE que ya
    // tiene la base de datos, esto también borra su checklist (supervision) y
    // sus reportes PDF asociados automáticamente.
    @DeleteMapping("/{folio}")
    public ResponseEntity<?> eliminarPorFolio(@PathVariable String folio) {
        Optional<Asignacion> asignacionOpt = asignacionRepository.findByFolio(folio);
        if (asignacionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "No existe una asignación con ese folio"));
        }

        asignacionRepository.delete(asignacionOpt.get());

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Asignación eliminada correctamente");
        return ResponseEntity.ok(response);
    }

    // Actualiza solo el estado de una asignación (ej. cuando el Supervisor envía su reporte a revisión).
    @PutMapping("/{folio}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable String folio, @RequestBody Map<String, String> datos) {
        Optional<Asignacion> asignacionOpt = asignacionRepository.findByFolio(folio);
        if (asignacionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "No existe una asignación con ese folio"));
        }

        Asignacion asignacion = asignacionOpt.get();
        asignacion.setEstado(datos.get("estado"));
        asignacionRepository.save(asignacion);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Estado actualizado correctamente");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> aMapaBasico(Asignacion a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("folio", a.getFolio());
        m.put("fecha", a.getFecha() != null ? a.getFecha().format(FORMATO_FECHA) : "");
        m.put("horaProgramada", a.getHoraProgramada());
        m.put("lugar", a.getLugar());
        m.put("prioridad", a.getPrioridad());
        m.put("descripcion", a.getDescripcion());
        m.put("observaciones", a.getObservaciones());
        m.put("personalApoyo", a.getPersonalApoyo() != null ? a.getPersonalApoyo() : "");
        m.put("estado", a.getEstado());
        m.put("idSupervisor", a.getIdSupervisor());
        return m;
    }

    private Optional<Usuario> buscarUsuarioPorRpeOUsername(String valor) {
        List<Usuario> todos = usuarioRepository.findAll();
        return todos.stream()
                .filter(u -> valor.equalsIgnoreCase(u.getRpe()) || valor.equalsIgnoreCase(u.getUsername()))
                .findFirst();
    }
}
