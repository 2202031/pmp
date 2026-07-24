package com.example.pmp_back.controller;

import com.example.pmp_back.model.CreacionReporte;
import com.example.pmp_back.model.Usuario;
import com.example.pmp_back.repository.CreacionReporteRepository;
import com.example.pmp_back.repository.UsuarioRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Reporte técnico (datos de facturación + 4 fotos de evidencia) que llena el
// Supervisor para una supervisión (folio). Tabla: creacion_reporte.
@RestController
@RequestMapping("/api/creacion-reporte")
@CrossOrigin(origins = "*")
public class CreacionReporteController {

    @Autowired
    private CreacionReporteRepository creacionReporteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Guarda/actualiza los datos del reporte (sin fotos) para un folio.
    @PostMapping("/guardar-datos")
    public ResponseEntity<?> guardarDatos(@RequestBody Map<String, Object> datos) {
        try {
            // ---- VALIDACIÓN antes de tocar la base de datos ----
            String folio = Validador.texto(datos, "folio");
            String anioNotificacion = Validador.texto(datos, "anioNotificacion");
            String kwh = Validador.texto(datos, "kwh");
            String importe = Validador.texto(datos, "importe");
            String rpu = Validador.texto(datos, "rpu");
            String numeroCorte = Validador.texto(datos, "numeroCorte");
            String tarifa = Validador.texto(datos, "tarifa");
            String statusServicio = Validador.texto(datos, "statusServicio");

            String error = Validador.validarFolio(folio);
            if (error == null) error = Validador.validarEntero(anioNotificacion, "anioNotificacion", false);
            if (error == null) error = Validador.validarDecimal(kwh, "kwh", false);
            if (error == null) error = Validador.validarDecimal(importe, "importe", false);
            if (error == null) error = Validador.opcional(rpu, "rpu", 100);
            if (error == null) error = Validador.opcional(numeroCorte, "numeroCorte", 100);
            if (error == null) error = Validador.opcional(tarifa, "tarifa", 100);
            if (error == null) error = Validador.opcional(statusServicio, "statusServicio", 100);

            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            // ---- Guardado (ya con valores seguros) ----
            CreacionReporte reporte = creacionReporteRepository.findByFolio(folio).orElse(new CreacionReporte());
            reporte.setFolio(folio);

            if (!anioNotificacion.isEmpty()) reporte.setAnioNotificacion(Validador.aEntero(anioNotificacion));
            if (!kwh.isEmpty()) reporte.setKwh(Validador.aFloat(kwh));
            if (!importe.isEmpty()) reporte.setImporte(Validador.aBigDecimal(importe));
            if (!rpu.isEmpty()) reporte.setRpu(rpu);
            if (!numeroCorte.isEmpty()) reporte.setNumeroCorte(numeroCorte);
            if (!tarifa.isEmpty()) reporte.setTarifa(tarifa);
            if (!statusServicio.isEmpty()) reporte.setStatusServicio(statusServicio);

            String username = Validador.texto(datos, "username");
            if (!username.isEmpty()) {
                Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
                usuarioOpt.ifPresent(u -> reporte.setIdUsuarios(u.getIdUsuarios()));
            }

            creacionReporteRepository.save(reporte);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Datos del reporte guardados correctamente");
            response.put("id_reporte", reporte.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al guardar el reporte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Sube una foto de evidencia (tipo: corte | fachada | medidor | selfi) en Base64.
    @PostMapping("/guardar-foto")
    public ResponseEntity<?> guardarFoto(@RequestBody Map<String, Object> datos) {
        try {
            String folio = Validador.texto(datos, "folio");
            String tipo = Validador.texto(datos, "tipo").toLowerCase();
            String base64 = Validador.texto(datos, "contenidoBase64");

            String error = Validador.validarFolio(folio);
            if (error == null) error = Validador.obligatorio(tipo, "tipo", 20);
            if (error == null && base64.isEmpty()) {
                error = "El campo 'contenidoBase64' es obligatorio (la foto viene vacía).";
            }
            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            byte[] contenido;
            try {
                contenido = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", "La foto no está codificada correctamente en Base64.");
                return ResponseEntity.badRequest().body(respuestaError);
            }

            CreacionReporte reporte = creacionReporteRepository.findByFolio(folio).orElse(new CreacionReporte());
            reporte.setFolio(folio);

            switch (tipo) {
                case "corte": reporte.setFotoCorte(contenido); break;
                case "fachada": reporte.setFotoFachada(contenido); break;
                case "medidor": reporte.setFotoMedidor(contenido); break;
                case "selfi": reporte.setFotoSelfi(contenido); break;
                default:
                    Map<String, String> tipoInvalido = new HashMap<>();
                    tipoInvalido.put("status", "error");
                    tipoInvalido.put("message", "Tipo de foto inválido. Usa: corte, fachada, medidor o selfi");
                    return ResponseEntity.badRequest().body(tipoInvalido);
            }

            creacionReporteRepository.save(reporte);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Foto guardada correctamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al guardar la foto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Datos del reporte (sin las fotos, para no pesar la respuesta). Indica cuáles fotos ya existen.
    @GetMapping("/{folio}")
    public ResponseEntity<?> obtenerPorFolio(@PathVariable String folio) {
        Optional<CreacionReporte> reporteOpt = creacionReporteRepository.findByFolio(folio);

        if (reporteOpt.isEmpty()) {
            return ResponseEntity.ok(new HashMap<>());
        }

        CreacionReporte r = reporteOpt.get();
        Map<String, Object> m = new HashMap<>();
        m.put("folio", r.getFolio());
        m.put("anioNotificacion", r.getAnioNotificacion());
        m.put("kwh", r.getKwh());
        m.put("importe", r.getImporte());
        m.put("rpu", r.getRpu());
        m.put("numeroCorte", r.getNumeroCorte());
        m.put("tarifa", r.getTarifa());
        m.put("statusServicio", r.getStatusServicio());
        m.put("tieneFotoCorte", r.getFotoCorte() != null);
        m.put("tieneFotoFachada", r.getFotoFachada() != null);
        m.put("tieneFotoMedidor", r.getFotoMedidor() != null);
        m.put("tieneFotoSelfi", r.getFotoSelfi() != null);

        return ResponseEntity.ok(m);
    }

    // Elimina (pone en NULL) una de las 4 fotos de evidencia de un folio.
    @DeleteMapping("/{folio}/foto/{tipo}")
    public ResponseEntity<?> eliminarFoto(@PathVariable String folio, @PathVariable String tipo) {
        Optional<CreacionReporte> reporteOpt = creacionReporteRepository.findByFolio(folio);
        if (reporteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "No existe reporte para ese folio"));
        }

        CreacionReporte r = reporteOpt.get();
        switch (tipo.toLowerCase()) {
            case "corte": r.setFotoCorte(null); break;
            case "fachada": r.setFotoFachada(null); break;
            case "medidor": r.setFotoMedidor(null); break;
            case "selfi": r.setFotoSelfi(null); break;
            default: return ResponseEntity.badRequest().build();
        }
        creacionReporteRepository.save(r);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Foto eliminada");
        return ResponseEntity.ok(response);
    }

    // Devuelve la imagen cruda de una de las 4 fotos de evidencia.
    @GetMapping("/{folio}/foto/{tipo}")
    public ResponseEntity<byte[]> obtenerFoto(@PathVariable String folio, @PathVariable String tipo) {
        Optional<CreacionReporte> reporteOpt = creacionReporteRepository.findByFolio(folio);
        if (reporteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CreacionReporte r = reporteOpt.get();
        byte[] contenido;

        switch (tipo.toLowerCase()) {
            case "corte": contenido = r.getFotoCorte(); break;
            case "fachada": contenido = r.getFotoFachada(); break;
            case "medidor": contenido = r.getFotoMedidor(); break;
            case "selfi": contenido = r.getFotoSelfi(); break;
            default: return ResponseEntity.badRequest().build();
        }

        if (contenido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(contenido);
    }
}
