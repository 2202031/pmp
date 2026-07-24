package com.example.pmp_back.controller;

import com.example.pmp_back.model.ReportePdf;
import com.example.pmp_back.repository.ReportePdfRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Reportes PDF ya emitidos por los Supervisores. El Administrador los consulta
// y descarga desde el botón "Reportes PDF" de su dashboard.
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    @Autowired
    private ReportePdfRepository reportePdfRepository;

    @PostMapping("/subir")
    public ResponseEntity<?> subirReporte(@RequestBody Map<String, Object> datos) {
        try {
            String folio = Validador.texto(datos, "folio");
            String nombreArchivo = Validador.texto(datos, "nombreArchivo");
            String usernameSupervisor = Validador.texto(datos, "usernameSupervisor");
            String contenidoBase64 = Validador.texto(datos, "contenidoBase64");

            // ---- VALIDACIÓN según columnas de la tabla reportes_pdf ----
            String error = Validador.validarFolio(folio);
            if (error == null) error = Validador.opcional(nombreArchivo, "nombreArchivo", 200);
            if (error == null) error = Validador.opcional(usernameSupervisor, "usernameSupervisor", 50);
            if (error == null && contenidoBase64.isEmpty()) {
                error = "El campo 'contenidoBase64' es obligatorio (el PDF viene vacío).";
            }
            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            if (nombreArchivo.isEmpty()) {
                nombreArchivo = "reporte_" + folio + ".pdf";
            }

            byte[] contenido;
            try {
                contenido = Base64.getDecoder().decode(contenidoBase64);
            } catch (IllegalArgumentException e) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", "El PDF no está codificado correctamente en Base64.");
                return ResponseEntity.badRequest().body(respuestaError);
            }

            ReportePdf reporte = new ReportePdf();
            reporte.setFolio(folio);
            reporte.setNombreArchivo(nombreArchivo);
            reporte.setUsernameSupervisor(usernameSupervisor.isEmpty() ? null : usernameSupervisor);
            reporte.setContenido(contenido);

            reportePdfRepository.save(reporte);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Reporte PDF guardado correctamente");
            response.put("id_reporte", reporte.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al subir el reporte: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> listarReportes(@RequestParam(required = false) String folio) {
        List<ReportePdf> reportes = (folio != null && !folio.isEmpty())
                ? reportePdfRepository.findByFolio(folio)
                : reportePdfRepository.findAllByOrderByFechaGeneracionDesc();

        List<Map<String, Object>> lista = new ArrayList<>();
        for (ReportePdf r : reportes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("folio", r.getFolio());
            item.put("nombreArchivo", r.getNombreArchivo());
            item.put("usernameSupervisor", r.getUsernameSupervisor());
            item.put("fechaGeneracion", r.getFechaGeneracion());
            lista.add(item);
        }
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarReporte(@PathVariable Integer id) {
        Optional<ReportePdf> reporteOpt = reportePdfRepository.findById(id);

        if (reporteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ReportePdf reporte = reporteOpt.get();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reporte.getNombreArchivo() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reporte.getContenido());
    }
}
