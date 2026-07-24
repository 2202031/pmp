package com.example.pmp_back.controller;

import com.example.pmp_back.model.Actividad;
import com.example.pmp_back.repository.ActividadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/actividades")
@CrossOrigin(origins = "*")
public class ActividadController {

    @Autowired
    private ActividadRepository actividadRepository;

    // Crea o actualiza una actividad (upsert por idActividad, generado por el front).
    @PostMapping("/guardar")
    public ResponseEntity<?> guardarActividad(@RequestBody Map<String, Object> datos) {
        try {
            String idActividad = datos.get("idActividad").toString().trim();

            Actividad actividad = actividadRepository.findByIdActividad(idActividad).orElse(new Actividad());
            actividad.setIdActividad(idActividad);
            actividad.setTitulo(datos.get("titulo") != null ? datos.get("titulo").toString() : "");
            actividad.setFecha(datos.get("fecha") != null ? datos.get("fecha").toString() : "");
            actividad.setHora(datos.get("hora") != null ? datos.get("hora").toString() : "");
            actividad.setDescripcion(datos.get("descripcion") != null ? datos.get("descripcion").toString() : "");
            actividad.setPrioridad(datos.get("prioridad") != null ? datos.get("prioridad").toString() : "");
            actividad.setFolio(datos.get("folio") != null ? datos.get("folio").toString() : "");
            actividad.setTecnico(datos.get("tecnico") != null ? datos.get("tecnico").toString() : "");
            actividad.setEstado(datos.get("estado") != null ? datos.get("estado").toString() : "");
            actividad.setFechaRegistro(datos.get("fechaRegistro") != null ? datos.get("fechaRegistro").toString() : "");

            actividadRepository.save(actividad);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Actividad guardada correctamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al guardar la actividad: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // Lista todas las actividades. El filtrado por supervisor/folio ya lo hace
    // la app del lado del cliente (igual que hacía con el almacenamiento local).
    @GetMapping
    public ResponseEntity<List<Actividad>> listarActividades() {
        return ResponseEntity.ok(actividadRepository.findAll());
    }

    @DeleteMapping("/{idActividad}")
    public ResponseEntity<?> eliminarActividad(@PathVariable String idActividad) {
        Optional<Actividad> actividadOpt = actividadRepository.findByIdActividad(idActividad);
        if (actividadOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "No existe esa actividad"));
        }

        actividadRepository.delete(actividadOpt.get());

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Actividad eliminada correctamente");
        return ResponseEntity.ok(response);
    }
}
