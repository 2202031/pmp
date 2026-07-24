package com.example.pmp_back.controller;

import com.example.pmp_back.model.Asignacion;
import com.example.pmp_back.model.Supervision;
import com.example.pmp_back.repository.AsignacionRepository;
import com.example.pmp_back.repository.SupervisionRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/checklists")
@CrossOrigin(origins = "*")
public class CheckListController {

    @Autowired
    private SupervisionRepository supervisionRepository;

    @Autowired
    private AsignacionRepository asignacionRepository;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardarChecklist(@RequestBody Map<String, Object> datos) {
        try {
            if (datos == null || datos.get("folio") == null) {
                Map<String, String> badRequest = new HashMap<>();
                badRequest.put("status", "error");
                badRequest.put("message", "El número de folio es obligatorio");
                return ResponseEntity.badRequest().body(badRequest);
            }

            String folio = datos.get("folio").toString().trim();

            String areaDelimitada = safeString(datos.get("areaDelimitada"));
            String equipoProteccion = safeString(datos.get("equipoProteccion"));
            String corteVisible = safeString(datos.get("corteVisible"));
            String deteccionPotencial = safeString(datos.get("deteccionPotencial"));
            String ceroMetales = safeString(datos.get("ceroMetales"));
            String actividadesSalvanVidas = safeString(datos.get("actividadesSalvanVidas"));
            String llenadoRim = safeString(datos.get("llenadoRim"));
            String observaciones = safeString(datos.get("observaciones"));
            String estado = safeString(datos.get("estado"));

            // ---- VALIDACIÓN según columnas de la tabla supervision ----
            // Las 7 respuestas son VARCHAR(100) y las observaciones TEXT.
            String error = Validador.validarFolio(folio);
            if (error == null) error = Validador.opcional(areaDelimitada, "areaDelimitada", 100);
            if (error == null) error = Validador.opcional(equipoProteccion, "equipoProteccion", 100);
            if (error == null) error = Validador.opcional(corteVisible, "corteVisible", 100);
            if (error == null) error = Validador.opcional(deteccionPotencial, "deteccionPotencial", 100);
            if (error == null) error = Validador.opcional(ceroMetales, "ceroMetales", 100);
            if (error == null) error = Validador.opcional(actividadesSalvanVidas, "actividadesSalvanVidas", 100);
            if (error == null) error = Validador.opcional(llenadoRim, "llenadoRim", 100);
            if (error == null) error = Validador.opcional(observaciones, "observaciones", 5000);

            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            int respondidas = 0;
            if (!areaDelimitada.isEmpty()) respondidas++;
            if (!equipoProteccion.isEmpty()) respondidas++;
            if (!corteVisible.isEmpty()) respondidas++;
            if (!deteccionPotencial.isEmpty()) respondidas++;
            if (!ceroMetales.isEmpty()) respondidas++;
            if (!actividadesSalvanVidas.isEmpty()) respondidas++;
            if (!llenadoRim.isEmpty()) respondidas++;

            float progreso = (respondidas * 100.0f) / 7.0f;

            Supervision supervision = supervisionRepository.findByFolio(folio).orElse(new Supervision());
            supervision.setFolio(folio);
            supervision.setProgreso(progreso);
            supervision.setAreaDelimitada(areaDelimitada);
            supervision.setEquipoSecurity(equipoProteccion);
            supervision.setCortePotencial(corteVisible);
            supervision.setDeteccionPotencial(deteccionPotencial);
            supervision.setMetalesCero(ceroMetales);
            supervision.setActividadesVida(actividadesSalvanVidas);
            supervision.setRimCorrecto(llenadoRim);
            supervision.setDescripcion(observaciones);

            supervisionRepository.save(supervision);

            if ("Completado".equalsIgnoreCase(estado)) {
                Optional<Asignacion> asignacionOpt = asignacionRepository.findByFolio(folio);
                if (asignacionOpt.isPresent()) {
                    Asignacion asignacion = asignacionOpt.get();
                    asignacion.setEstado("Verificada");
                    asignacionRepository.save(asignacion);
                }
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Checklist guardado correctamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/buscar/{folio}")
    public ResponseEntity<?> buscarChecklistPorFolio(@PathVariable String folio) {
        try {
            Optional<Supervision> supervisionOpt = supervisionRepository.findByFolio(folio);

            if (supervisionOpt.isEmpty()) {
                return ResponseEntity.ok("");
            }

            Supervision s = supervisionOpt.get();
            Map<String, Object> mapper = new HashMap<>();
            mapper.put("areaDelimitada", s.getAreaDelimitada() != null ? s.getAreaDelimitada() : "");
            mapper.put("equipoProteccion", s.getEquipoSecurity() != null ? s.getEquipoSecurity() : "");
            mapper.put("corteVisible", s.getCortePotencial() != null ? s.getCortePotencial() : "");
            mapper.put("deteccionPotencial", s.getDeteccionPotencial() != null ? s.getDeteccionPotencial() : "");
            mapper.put("ceroMetales", s.getMetalesCero() != null ? s.getMetalesCero() : "");
            mapper.put("actividadesSalvanVidas", s.getActividadesVida() != null ? s.getActividadesVida() : "");
            mapper.put("llenadoRim", s.getRimCorrecto() != null ? s.getRimCorrecto() : "");
            mapper.put("observaciones", s.getDescripcion() != null ? s.getDescripcion() : "");
            mapper.put("progreso", s.getProgreso());
            // El checklist se considera completado cuando las 7 preguntas están respondidas (progreso == 100).
            mapper.put("checklistCompletado", s.getProgreso() >= 99.9f);

            Optional<Asignacion> asignacionOpt = asignacionRepository.findByFolio(folio);
            mapper.put("estado", asignacionOpt.map(Asignacion::getEstado).orElse("Pendiente"));

            return ResponseEntity.ok(mapper);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String safeString(Object obj) {
        return (obj == null) ? "" : obj.toString().trim();
    }
}
