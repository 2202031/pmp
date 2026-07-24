package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pmp.front.Config;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class ReportePdfSupervisorActivity extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_ROL =
            "rol_actual";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_CHECKLISTS =
            "checklists_local";

    private static final String KEY_CHECKLISTS =
            "checklists";

    private static final String PREFS_REPORTES =
            "reportes_tecnicos_local";

    private static final String KEY_REPORTES =
            "reportes";

    private static final String PREFS_REVISIONES =
            "revisiones_local";

    private static final String KEY_REVISIONES =
            "revisiones";

    private static final String PREFS_EVIDENCIAS =
            "evidencias_local";

    private static final String PREFS_PDF =
            "reportes_pdf_local";

    private static final String PREFIJO_ESTADO_PDF =
            "estado_";

    private static final String PREFIJO_RUTA_PDF =
            "ruta_";

    private static final String PREFIJO_FECHA_PDF =
            "fecha_";

    private static final String[] SUFIJOS_EVIDENCIAS = {
            "foto_corte",
            "foto_fachada",
            "foto_medidor",
            "foto_selfi"
    };

    private static final String[] NOMBRES_EVIDENCIAS = {
            "Foto del corte",
            "Foto de fachada",
            "Foto del medidor asegurado",
            "Foto selfi"
    };

    private static final int ANCHO_PAGINA = 595;
    private static final int ALTO_PAGINA = 842;
    private static final int MARGEN = 40;
    private static final int ANCHO_CONTENIDO =
            ANCHO_PAGINA - (MARGEN * 2);

    private TextView btnVolver;

    private TextView txtFolioPdf;
    private TextView txtCircuitoPdf;
    private TextView txtEstadoSupervisionPdf;

    private TextView txtEstadoPdf;
    private TextView txtFechaPdf;
    private TextView txtResumenContenidoPdf;

    private TextView btnGenerarPdf;
    private TextView btnVerPdf;
    private TextView btnCompartirPdf;

    private String folio;

    private String[] supervisionActual;
    private String[] checklistActual;
    private String[] reporteActual;

    private final String[] rutasEvidenciaTemp = new String[4];

    private final ExecutorService executorServiceCarga = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<String[]> revisiones =
            new ArrayList<>();

    private PdfDocument documentoPdf;
    private PdfDocument.Page paginaPdf;
    private Canvas canvasPdf;

    private final Paint paintPdf =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private int numeroPagina;
    private int posicionY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_reporte_pdf_supervisor
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        folio = getIntent().getStringExtra("folio");

        if (folio == null ||
                folio.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No se encontró el folio de la supervisión",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (folio != null &&
                !folio.trim().isEmpty()) {

            cargarInformacion();
        }
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        txtFolioPdf =
                findViewById(R.id.txtFolioPdf);

        txtCircuitoPdf =
                findViewById(R.id.txtCircuitoPdf);

        txtEstadoSupervisionPdf =
                findViewById(
                        R.id.txtEstadoSupervisionPdf
                );

        txtEstadoPdf =
                findViewById(R.id.txtEstadoPdf);

        txtFechaPdf =
                findViewById(R.id.txtFechaPdf);

        txtResumenContenidoPdf =
                findViewById(
                        R.id.txtResumenContenidoPdf
                );

        btnGenerarPdf =
                findViewById(R.id.btnGenerarPdf);

        btnVerPdf =
                findViewById(R.id.btnVerPdf);

        btnCompartirPdf =
                findViewById(R.id.btnCompartirPdf);
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        String rol = preferences.getString(
                KEY_ROL,
                ""
        );

        if (!"administrador".equalsIgnoreCase(rol)) {
            Intent intent = new Intent(
                    this,
                    LoginActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return false;
        }

        return true;
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v ->
                finish()
        );

        btnGenerarPdf.setOnClickListener(v -> {
            if (!com.pmp.front.RedHelper.hayConexion(this)) {
                Toast.makeText(this,
                        "Sin conexión a la red. Conéctate al WiFi para generar y guardar el PDF.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            confirmarGeneracion();
        });

        btnVerPdf.setOnClickListener(v ->
                abrirPdf()
        );

        btnCompartirPdf.setOnClickListener(v ->
                compartirPdf()
        );
    }

    private void cargarInformacion() {
        executorServiceCarga.execute(() -> {
            String[] nuevaSupervision = obtenerAsignacionDelServidor();
            String[] nuevoChecklist = (nuevaSupervision != null) ? obtenerChecklistDelServidor() : null;
            String[] nuevoReporte = (nuevaSupervision != null) ? obtenerReporteDelServidor() : null;

            if (nuevaSupervision != null) {
                descargarEvidenciasATemporal();
            }

            mainHandler.post(() -> {
                supervisionActual = nuevaSupervision;
                checklistActual = nuevoChecklist;
                reporteActual = nuevoReporte;

                cargarRevisiones();

                if (supervisionActual == null) {
                    mostrarSupervisionNoEncontrada();
                    return;
                }

                mostrarResumen();
                actualizarEstadoPdf();
            });
        });
    }

    // Trae la asignación del servidor y arma un arreglo de 15 campos compatible
    // con el resto de la pantalla (que ya sabe leer supervisionActual[índice]).
    private String[] obtenerAsignacionDelServidor() {
        try {
            URL url = new URL(Config.BASE_URL + "/api/asignaciones/" + folio);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            JSONObject json = leerRespuesta(connection);

            String[] partes = new String[15];
            partes[0] = json.optString("folio", "");
            partes[1] = json.optString("fecha", "");
            partes[2] = ""; // circuito (no manejado en servidor)
            partes[3] = json.optString("lugar", "");
            partes[4] = json.optString("prioridad", "");
            partes[5] = json.optString("descripcion", "");
            partes[6] = json.optString("observaciones", "");
            partes[7] = json.optString("usernameSupervisor", "");
            partes[8] = json.optString("tecnico", "");
            partes[9] = "";
            partes[10] = ""; // personal de apoyo (no manejado en servidor)
            partes[11] = json.optString("estado", "");
            partes[12] = "";
            partes[13] = "Verificada".equalsIgnoreCase(json.optString("estado", "")) ? "Validado" : "Pendiente";
            partes[14] = json.optString("horaProgramada", "");

            return partes;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Trae el checklist del servidor y arma un arreglo de 13 campos compatible.
    private String[] obtenerChecklistDelServidor() {
        try {
            URL url = new URL(Config.BASE_URL + "/api/checklists/buscar/" + folio);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            String respuesta = leerTexto(connection).trim();
            if (respuesta.isEmpty() || respuesta.equals("\"\"")) {
                return null;
            }

            JSONObject json = new JSONObject(respuesta);

            String[] partes = new String[13];
            partes[0] = folio;
            partes[1] = json.optString("areaDelimitada", "");
            partes[2] = json.optString("equipoProteccion", "");
            partes[3] = json.optString("corteVisible", "");
            partes[4] = ""; // detección de corte de potencial (no manejado por separado en el servidor)
            partes[5] = json.optString("ceroMetales", "");
            partes[6] = json.optString("actividadesSalvanVidas", "");
            partes[7] = json.optString("llenadoRim", "");
            partes[8] = json.optString("observaciones", "");
            partes[9] = json.has("progreso") ? String.valueOf(json.optDouble("progreso", 0)) : "";
            partes[10] = "";
            partes[11] = "";
            partes[12] = json.optString("estado", "Pendiente");

            return partes;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Trae el reporte técnico del servidor y arma un arreglo de 12 campos compatible.
    private String[] obtenerReporteDelServidor() {
        try {
            URL url = new URL(Config.BASE_URL + "/api/creacion-reporte/" + folio);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            JSONObject json = leerRespuesta(connection);
            if (json.length() == 0 || json.opt("folio") == null) {
                return null;
            }

            String statusCombinado = json.optString("statusServicio", "");
            String[] partesStatus = statusCombinado.split("\\|\\|", -1);

            String[] partes = new String[12];
            partes[0] = folio;
            partes[1] = json.optString("anioNotificacion", "");
            partes[2] = json.optString("kwh", "");
            partes[3] = json.optString("importe", "");
            partes[4] = json.optString("rpu", "");
            partes[5] = json.optString("numeroCorte", "");
            partes[6] = json.optString("tarifa", "");
            partes[7] = partesStatus.length > 0 ? partesStatus[0] : "";
            partes[8] = partesStatus.length > 1 ? partesStatus[1] : "";
            partes[9] = "";
            partes[10] = "";
            partes[11] = "";

            return partes;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Descarga las 4 fotos de evidencia (si existen) a la caché de la app,
    // para que crearDocumentoPdf() las pueda leer como archivos locales.
    private void descargarEvidenciasATemporal() {
        for (int i = 0; i < SUFIJOS_EVIDENCIAS.length; i++) {
            String tipo = SUFIJOS_EVIDENCIAS[i].replace("foto_", "");
            try {
                URL url = new URL(Config.BASE_URL + "/api/creacion-reporte/" + folio + "/foto/" + tipo);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    File temp = new File(getCacheDir(), "evidencia_" + folio + "_" + tipo + ".jpg");

                    try (InputStream is = connection.getInputStream();
                         FileOutputStream fos = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[4096];
                        int leidos;
                        while ((leidos = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, leidos);
                        }
                    }

                    rutasEvidenciaTemp[i] = temp.getAbsolutePath();
                } else {
                    rutasEvidenciaTemp[i] = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                rutasEvidenciaTemp[i] = null;
            }
        }
    }

    private JSONObject leerRespuesta(HttpURLConnection connection) throws Exception {
        return new JSONObject(leerTexto(connection));
    }

    private String leerTexto(HttpURLConnection connection) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private void cargarRevisiones() {
        revisiones.clear();

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_REVISIONES,
                        MODE_PRIVATE
                );

        String datos = preferences.getString(
                KEY_REVISIONES,
                ""
        );

        if (datos == null ||
                datos.trim().isEmpty()) {

            return;
        }

        String[] registros = datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 5) {
                continue;
            }

            if (folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                revisiones.add(partes);
            }
        }
    }

    private void mostrarResumen() {
        txtFolioPdf.setText(
                supervisionActual[0]
        );

        txtCircuitoPdf.setText(
                supervisionActual[2] +
                        " • " +
                        supervisionActual[1]
        );

        txtEstadoSupervisionPdf.setText(
                supervisionActual[11]
        );

        int cantidadEvidencias =
                contarEvidencias();

        String checklistDisponible =
                checklistActual != null
                        ? "Disponible"
                        : "No encontrado";

        String reporteDisponible =
                reporteActual != null
                        ? "Disponible"
                        : "No encontrado";

        String validacionDisponible =
                obtenerUltimaValidacion() != null
                        ? "Disponible"
                        : "No encontrada";

        String resumen =
                "Checklist: " +
                        checklistDisponible +
                        "\n" +

                        "Reporte técnico: " +
                        reporteDisponible +
                        "\n" +

                        "Evidencias: " +
                        cantidadEvidencias +
                        " de 4" +
                        "\n" +

                        "Validación: " +
                        validacionDisponible;

        txtResumenContenidoPdf.setText(resumen);
    }

    private void actualizarEstadoPdf() {
        File archivo = obtenerArchivoPdfExistente();

        boolean generado =
                archivo != null &&
                        archivo.exists();

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PDF,
                        MODE_PRIVATE
                );

        String fecha = preferences.getString(
                PREFIJO_FECHA_PDF + folio,
                ""
        );

        if (generado) {
            txtEstadoPdf.setText("Generado");

            txtFechaPdf.setText(
                    fecha.isEmpty()
                            ? "Documento disponible"
                            : "Generado: " + fecha
            );

            btnGenerarPdf.setText(
                    "Regenerar PDF"
            );
        } else {
            txtEstadoPdf.setText(
                    "No generado"
            );

            txtFechaPdf.setText(
                    "El documento todavía no ha sido generado."
            );

            btnGenerarPdf.setText(
                    "Generar PDF"
            );
        }

        boolean disponibleParaGenerar =
                estaDisponibleParaGenerar();

        btnGenerarPdf.setEnabled(
                disponibleParaGenerar
        );

        btnGenerarPdf.setAlpha(
                disponibleParaGenerar
                        ? 1f
                        : 0.55f
        );

        btnVerPdf.setEnabled(generado);
        btnVerPdf.setAlpha(
                generado ? 1f : 0.55f
        );

        btnCompartirPdf.setEnabled(generado);
        btnCompartirPdf.setAlpha(
                generado ? 1f : 0.55f
        );
    }

    private boolean estaDisponibleParaGenerar() {
        return supervisionActual != null &&
                checklistActual != null &&
                reporteActual != null &&

                estadoPermitePdf(supervisionActual[11]) &&

                contarEvidencias() == 4;
    }

    // El PDF puede generarse cuando el Administrador ya validó la supervisión.
    // Según el flujo, ese estado puede llamarse "Verificada" (checklist listo)
    // o "Finalizada" (reporte ya validado por el Administrador).
    private boolean estadoPermitePdf(String estado) {
        return "Verificada".equalsIgnoreCase(estado) ||
                "Finalizada".equalsIgnoreCase(estado) ||
                "Validada".equalsIgnoreCase(estado);
    }

    private List<String> obtenerFaltantes() {
        List<String> faltantes =
                new ArrayList<>();

        if (supervisionActual == null) {
            faltantes.add(
                    "Datos de la supervisión"
            );

            return faltantes;
        }

        if (!estadoPermitePdf(supervisionActual[11])) {
            faltantes.add(
                    "El checklist debe estar completado y verificado (estado actual: " +
                            (supervisionActual[11] == null || supervisionActual[11].isEmpty() ? "Asignada" : supervisionActual[11]) +
                            ")"
            );
        }

        if (checklistActual == null) {
            faltantes.add(
                    "Información del checklist"
            );
        }

        if (reporteActual == null) {
            faltantes.add(
                    "Información del reporte técnico"
            );
        }

        int evidencias = contarEvidencias();

        if (evidencias < 4) {
            faltantes.add(
                    "Las cuatro evidencias fotográficas " +
                            "(" + evidencias + " de 4)"
            );
        }

        return faltantes;
    }

    private void confirmarGeneracion() {
        List<String> faltantes =
                obtenerFaltantes();

        if (!faltantes.isEmpty()) {
            StringBuilder mensaje =
                    new StringBuilder();

            mensaje.append(
                    "No se puede generar el PDF porque falta:\n\n"
            );

            for (String faltante : faltantes) {
                mensaje.append("• ")
                        .append(faltante)
                        .append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "PDF no disponible"
                    )
                    .setMessage(
                            mensaje.toString()
                    )
                    .setPositiveButton(
                            "Entendido",
                            null
                    )
                    .show();

            return;
        }

        boolean yaExiste =
                obtenerArchivoPdfExistente() != null;

        String mensaje = yaExiste
                ? "Ya existe un PDF para esta supervisión.\n\n" +
                "¿Deseas reemplazarlo por una versión actualizada?"
                : "Se generará un documento que integra el checklist, " +
                "el reporte, las evidencias y la validación.\n\n" +
                "¿Deseas continuar?";

        new AlertDialog.Builder(this)
                .setTitle(
                        yaExiste
                                ? "Regenerar PDF"
                                : "Generar PDF"
                )
                .setMessage(mensaje)
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Generar",
                        (dialog, which) ->
                                generarPdf()
                )
                .show();
    }

    private void generarPdf() {
        try {
            File archivo = crearDocumentoPdf();

            guardarEstadoPdf(archivo);
            subirReporteAlServidor(archivo);

            actualizarEstadoPdf();

            new AlertDialog.Builder(this)
                    .setTitle(
                            "PDF generado"
                    )
                    .setMessage(
                            "El reporte final fue generado correctamente.\n\n" +
                                    "Ahora puedes abrirlo o compartirlo."
                    )
                    .setNegativeButton(
                            "Cerrar",
                            null
                    )
                    .setPositiveButton(
                            "Ver PDF",
                            (dialog, which) ->
                                    abrirPdf()
                    )
                    .show();

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "No se pudo generar el PDF: " +
                            exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private final ExecutorService executorServiceSubidaPdf = Executors.newSingleThreadExecutor();

    // Sube el PDF ya generado al servidor (tabla reportes_pdf) para que el
    // Administrador pueda verlo/descargarlo desde cualquier dispositivo.
    private void subirReporteAlServidor(File archivo) {
        executorServiceSubidaPdf.execute(() -> {
            try {
                SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
                String usernameSupervisor = preferences.getString("usuario_actual", "");

                byte[] bytes;
                try (FileInputStream fis = new FileInputStream(archivo)) {
                    bytes = new byte[(int) archivo.length()];
                    int leidos = 0;
                    while (leidos < bytes.length) {
                        int n = fis.read(bytes, leidos, bytes.length - leidos);
                        if (n == -1) break;
                        leidos += n;
                    }
                }

                String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);

                JSONObject json = new JSONObject();
                json.put("folio", folio);
                json.put("nombreArchivo", archivo.getName());
                json.put("usernameSupervisor", usernameSupervisor);
                json.put("contenidoBase64", base64);

                URL url = new URL(Config.BASE_URL + "/api/reportes/subir");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode(); // dispara la petición; no bloqueamos la UI con el resultado

            } catch (Exception e) {
                e.printStackTrace();
                // Si falla la subida, el PDF sigue disponible localmente en el dispositivo;
                // no interrumpimos el flujo del supervisor por un problema de red.
            }
        });
    }

    private File crearDocumentoPdf()
            throws IOException {

        File directorio = new File(
                getFilesDir(),
                "reportes"
        );

        if (!directorio.exists() &&
                !directorio.mkdirs()) {

            throw new IOException(
                    "No se pudo crear la carpeta de reportes"
            );
        }

        File archivo = new File(
                directorio,
                obtenerNombreArchivoPdf()
        );

        documentoPdf = new PdfDocument();
        paginaPdf = null;
        canvasPdf = null;
        numeroPagina = 0;

        try {
            iniciarNuevaPagina();

            dibujarTituloDocumento(
                    "REPORTE FINAL DE SUPERVISIÓN DE CORTES"
            );

            dibujarCampo(
                    "Folio",
                    supervisionActual[0]
            );

            dibujarCampo(
                    "Estado final",
                    supervisionActual[11]
            );

            dibujarCampo(
                    "Fecha de generación",
                    obtenerFechaActual()
            );

            dibujarSeccion(
                    "1. Datos generales de la supervisión"
            );

            dibujarCampo(
                    "Fecha programada",
                    supervisionActual[1]
            );

            dibujarCampo(
                    "Circuito",
                    supervisionActual[2]
            );

            dibujarCampo(
                    "Lugar o referencia",
                    supervisionActual[3]
            );

            dibujarCampo(
                    "Prioridad",
                    supervisionActual[4]
            );

            dibujarCampo(
                    "Descripción",
                    supervisionActual[5]
            );

            dibujarCampo(
                    "Observaciones iniciales",
                    valorVisible(
                            supervisionActual[6],
                            "Sin observaciones iniciales"
                    )
            );

            dibujarCampo(
                    "Técnico responsable",
                    supervisionActual[8]
            );

            dibujarCampo(
                    "Personal de apoyo",
                    valorVisible(
                            supervisionActual[10],
                            "Sin personal de apoyo"
                    )
            );

            dibujarCampo(
                    "Fecha de registro",
                    supervisionActual[14]
            );

            dibujarSeccion(
                    "2. Checklist de seguridad e higiene"
            );

            dibujarCampo(
                    "Área delimitada",
                    checklistActual[1]
            );

            dibujarCampo(
                    "Equipo de protección personal",
                    checklistActual[2]
            );

            dibujarCampo(
                    "Corte visible",
                    checklistActual[3]
            );

            dibujarCampo(
                    "Detección de corte de potencial",
                    checklistActual[4]
            );

            dibujarCampo(
                    "Cero metales",
                    checklistActual[5]
            );

            dibujarCampo(
                    "Actividades que salvan vidas",
                    checklistActual[6]
            );

            dibujarCampo(
                    "Llenado correcto de RIM",
                    checklistActual[7]
            );

            dibujarCampo(
                    "Observaciones del checklist",
                    valorVisible(
                            checklistActual[8],
                            "Sin observaciones"
                    )
            );

            dibujarSeccion(
                    "3. Reporte de supervisión de cortes"
            );

            dibujarCampo(
                    "Número y año de notificación",
                    reporteActual[1]
            );

            dibujarCampo(
                    "KWh",
                    reporteActual[2]
            );

            dibujarCampo(
                    "Importe",
                    "$ " + reporteActual[3]
            );

            dibujarCampo(
                    "RPU de notificación",
                    reporteActual[4]
            );

            dibujarCampo(
                    "Número de orden de corte",
                    reporteActual[5]
            );

            dibujarCampo(
                    "Tarifa",
                    reporteActual[6]
            );

            dibujarCampo(
                    "Estatus del servicio",
                    reporteActual[7]
            );

            dibujarCampo(
                    "Datos del corte",
                    reporteActual[8]
            );

            dibujarSeccion(
                    "4. Revisión y validación"
            );

            if (revisiones.isEmpty()) {
                dibujarCampo(
                        "Historial",
                        "No se encontraron revisiones registradas."
                );
            } else {
                for (int i = 0;
                     i < revisiones.size();
                     i++) {

                    String[] revision =
                            revisiones.get(i);

                    dibujarSubtitulo(
                            "Revisión " + (i + 1)
                    );

                    dibujarCampo(
                            "Resultado",
                            revision[4]
                    );

                    dibujarCampo(
                            "Fecha",
                            revision[2]
                    );

                    dibujarCampo(
                            "Supervisor",
                            revision[3]
                    );

                    dibujarCampo(
                            "Observaciones",
                            valorVisible(
                                    revision[1],
                                    "Sin observaciones"
                            )
                    );
                }
            }

            dibujarSeccion(
                    "5. Evidencias fotográficas"
            );

            for (int i = 0;
                 i < SUFIJOS_EVIDENCIAS.length;
                 i++) {

                dibujarEvidencia(
                        NOMBRES_EVIDENCIAS[i],
                        obtenerRutaEvidencia(i)
                );
            }

            dibujarSeccion(
                    "6. Cierre del documento"
            );

            String[] validacion =
                    obtenerUltimaValidacion();

            if (validacion != null) {
                dibujarCampo(
                        "Resultado final",
                        validacion[4]
                );

                dibujarCampo(
                        "Supervisor que validó",
                        validacion[3]
                );

                dibujarCampo(
                        "Fecha de validación",
                        validacion[2]
                );

                dibujarCampo(
                        "Observaciones finales",
                        valorVisible(
                                validacion[1],
                                "Sin observaciones finales"
                        )
                );
            } else {
                dibujarCampo(
                        "Resultado final",
                        "Supervisión validada"
                );
            }

            dibujarTextoFinal(
                    "Documento generado por el sistema de " +
                            "Supervisiones de Cortes."
            );

            finalizarPaginaActual();

            try (FileOutputStream outputStream =
                         new FileOutputStream(archivo)) {

                documentoPdf.writeTo(outputStream);
            }

            return archivo;

        } finally {
            if (paginaPdf != null) {
                finalizarPaginaActual();
            }

            if (documentoPdf != null) {
                documentoPdf.close();
            }

            documentoPdf = null;
            paginaPdf = null;
            canvasPdf = null;
        }
    }

    private void iniciarNuevaPagina() {
        if (paginaPdf != null) {
            finalizarPaginaActual();
        }

        numeroPagina++;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(
                        ANCHO_PAGINA,
                        ALTO_PAGINA,
                        numeroPagina
                ).create();

        paginaPdf =
                documentoPdf.startPage(pageInfo);

        canvasPdf =
                paginaPdf.getCanvas();

        paintPdf.setStyle(Paint.Style.FILL);
        paintPdf.setColor(
                Color.rgb(0, 99, 65)
        );

        canvasPdf.drawRect(
                0,
                0,
                ANCHO_PAGINA,
                70,
                paintPdf
        );

        paintPdf.setColor(Color.WHITE);
        paintPdf.setTextSize(15);
        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );

        canvasPdf.drawText(
                "CFE | Supervisiones",
                MARGEN,
                31,
                paintPdf
        );

        paintPdf.setTextSize(9);
        paintPdf.setTypeface(
                Typeface.DEFAULT
        );

        canvasPdf.drawText(
                "Reporte final de supervisión de cortes",
                MARGEN,
                49,
                paintPdf
        );

        paintPdf.setTextAlign(
                Paint.Align.RIGHT
        );

        canvasPdf.drawText(
                folio,
                ANCHO_PAGINA - MARGEN,
                40,
                paintPdf
        );

        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );

        posicionY = 95;
    }

    private void finalizarPaginaActual() {
        if (paginaPdf == null) {
            return;
        }

        paintPdf.setColor(
                Color.rgb(107, 114, 128)
        );

        paintPdf.setTextSize(8);
        paintPdf.setTypeface(
                Typeface.DEFAULT
        );

        paintPdf.setTextAlign(
                Paint.Align.CENTER
        );

        canvasPdf.drawText(
                "Página " + numeroPagina,
                ANCHO_PAGINA / 2f,
                ALTO_PAGINA - 22,
                paintPdf
        );

        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );

        documentoPdf.finishPage(paginaPdf);

        paginaPdf = null;
        canvasPdf = null;
    }

    private void asegurarEspacio(
            int espacioNecesario
    ) {
        int limiteInferior =
                ALTO_PAGINA - 55;

        if (posicionY + espacioNecesario >
                limiteInferior) {

            iniciarNuevaPagina();
        }
    }

    private void dibujarTituloDocumento(
            String titulo
    ) {
        asegurarEspacio(75);

        paintPdf.setColor(
                Color.rgb(0, 59, 36)
        );

        paintPdf.setTextSize(17);

        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        paintPdf.setTextAlign(
                Paint.Align.CENTER
        );

        List<String> lineas =
                dividirTexto(
                        titulo,
                        paintPdf,
                        ANCHO_CONTENIDO
                );

        for (String linea : lineas) {
            canvasPdf.drawText(
                    linea,
                    ANCHO_PAGINA / 2f,
                    posicionY,
                    paintPdf
            );

            posicionY += 22;
        }

        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );

        paintPdf.setColor(
                Color.rgb(227, 6, 19)
        );

        canvasPdf.drawRect(
                MARGEN,
                posicionY + 3,
                ANCHO_PAGINA - MARGEN,
                posicionY + 7,
                paintPdf
        );

        posicionY += 25;
    }

    private void dibujarSeccion(
            String titulo
    ) {
        asegurarEspacio(45);

        paintPdf.setStyle(Paint.Style.FILL);

        paintPdf.setColor(
                Color.rgb(220, 239, 230)
        );

        RectF fondo = new RectF(
                MARGEN,
                posicionY,
                ANCHO_PAGINA - MARGEN,
                posicionY + 30
        );

        canvasPdf.drawRoundRect(
                fondo,
                6,
                6,
                paintPdf
        );

        paintPdf.setColor(
                Color.rgb(0, 99, 65)
        );

        paintPdf.setTextSize(12);

        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        canvasPdf.drawText(
                titulo,
                MARGEN + 10,
                posicionY + 20,
                paintPdf
        );

        posicionY += 43;
    }

    private void dibujarSubtitulo(
            String titulo
    ) {
        asegurarEspacio(30);

        paintPdf.setColor(
                Color.rgb(31, 41, 55)
        );

        paintPdf.setTextSize(11);

        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        canvasPdf.drawText(
                titulo,
                MARGEN,
                posicionY,
                paintPdf
        );

        posicionY += 18;
    }

    private void dibujarCampo(
            String etiqueta,
            String valor
    ) {
        asegurarEspacio(38);

        paintPdf.setColor(
                Color.rgb(75, 85, 99)
        );

        paintPdf.setTextSize(9);

        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        canvasPdf.drawText(
                etiqueta,
                MARGEN,
                posicionY,
                paintPdf
        );

        posicionY += 14;

        dibujarTextoEnvuelto(
                valorVisible(
                        valor,
                        "No disponible"
                ),
                11,
                Color.rgb(17, 24, 39),
                Typeface.DEFAULT,
                15
        );

        posicionY += 7;
    }

    private void dibujarTextoEnvuelto(
            String texto,
            float tamano,
            int color,
            Typeface typeface,
            int altoLinea
    ) {
        paintPdf.setColor(color);
        paintPdf.setTextSize(tamano);
        paintPdf.setTypeface(typeface);
        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );

        List<String> lineas =
                dividirTexto(
                        texto,
                        paintPdf,
                        ANCHO_CONTENIDO
                );

        for (String linea : lineas) {
            asegurarEspacio(
                    altoLinea + 4
            );

            canvasPdf.drawText(
                    linea,
                    MARGEN,
                    posicionY,
                    paintPdf
            );

            posicionY += altoLinea;
        }
    }

    private List<String> dividirTexto(
            String texto,
            Paint paint,
            float anchoMaximo
    ) {
        List<String> resultado =
                new ArrayList<>();

        if (texto == null) {
            resultado.add("");
            return resultado;
        }

        String[] parrafos =
                texto.split("\\n", -1);

        for (String parrafo : parrafos) {
            String limpio = parrafo.trim();

            if (limpio.isEmpty()) {
                resultado.add("");
                continue;
            }

            String[] palabras =
                    limpio.split("\\s+");

            StringBuilder linea =
                    new StringBuilder();

            for (String palabra : palabras) {
                String candidato =
                        linea.length() == 0
                                ? palabra
                                : linea + " " + palabra;

                if (paint.measureText(candidato) <=
                        anchoMaximo) {

                    linea.setLength(0);
                    linea.append(candidato);

                } else {
                    if (linea.length() > 0) {
                        resultado.add(
                                linea.toString()
                        );
                    }

                    linea.setLength(0);
                    linea.append(palabra);
                }
            }

            if (linea.length() > 0) {
                resultado.add(
                        linea.toString()
                );
            }
        }

        if (resultado.isEmpty()) {
            resultado.add("");
        }

        return resultado;
    }

    private void dibujarEvidencia(
            String titulo,
            String ruta
    ) {
        asegurarEspacio(320);

        dibujarSubtitulo(titulo);

        int alturaMaxima = 250;

        Bitmap bitmap = null;

        try {
            bitmap = cargarBitmapReducido(
                    ruta,
                    ANCHO_CONTENIDO,
                    alturaMaxima
            );
        } catch (Exception ignored) {
        }

        if (bitmap == null) {
            paintPdf.setStyle(
                    Paint.Style.STROKE
            );

            paintPdf.setStrokeWidth(1);

            paintPdf.setColor(
                    Color.rgb(156, 163, 175)
            );

            RectF marco = new RectF(
                    MARGEN,
                    posicionY,
                    ANCHO_PAGINA - MARGEN,
                    posicionY + 120
            );

            canvasPdf.drawRect(
                    marco,
                    paintPdf
            );

            paintPdf.setStyle(
                    Paint.Style.FILL
            );

            paintPdf.setTextSize(11);

            paintPdf.setColor(
                    Color.rgb(107, 114, 128)
            );

            paintPdf.setTextAlign(
                    Paint.Align.CENTER
            );

            canvasPdf.drawText(
                    "Evidencia no disponible",
                    ANCHO_PAGINA / 2f,
                    posicionY + 64,
                    paintPdf
            );

            paintPdf.setTextAlign(
                    Paint.Align.LEFT
            );

            posicionY += 140;
            return;
        }

        float escala = Math.min(
                ANCHO_CONTENIDO /
                        (float) bitmap.getWidth(),

                alturaMaxima /
                        (float) bitmap.getHeight()
        );

        int anchoDestino =
                Math.round(
                        bitmap.getWidth() *
                                escala
                );

        int altoDestino =
                Math.round(
                        bitmap.getHeight() *
                                escala
                );

        float izquierda =
                MARGEN +
                        (
                                ANCHO_CONTENIDO -
                                        anchoDestino
                        ) / 2f;

        RectF destino = new RectF(
                izquierda,
                posicionY,
                izquierda + anchoDestino,
                posicionY + altoDestino
        );

        paintPdf.setStyle(Paint.Style.FILL);
        paintPdf.setColor(Color.WHITE);

        canvasPdf.drawRect(
                destino,
                paintPdf
        );

        canvasPdf.drawBitmap(
                bitmap,
                null,
                destino,
                paintPdf
        );

        paintPdf.setStyle(Paint.Style.STROKE);
        paintPdf.setStrokeWidth(1);

        paintPdf.setColor(
                Color.rgb(209, 213, 219)
        );

        canvasPdf.drawRect(
                destino,
                paintPdf
        );

        paintPdf.setStyle(Paint.Style.FILL);

        posicionY += altoDestino + 24;

        bitmap.recycle();
    }

    private void dibujarTextoFinal(
            String texto
    ) {
        asegurarEspacio(60);

        posicionY += 10;

        paintPdf.setColor(
                Color.rgb(107, 114, 128)
        );

        paintPdf.setTextSize(9);

        paintPdf.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.ITALIC
                )
        );

        paintPdf.setTextAlign(
                Paint.Align.CENTER
        );

        List<String> lineas =
                dividirTexto(
                        texto,
                        paintPdf,
                        ANCHO_CONTENIDO
                );

        for (String linea : lineas) {
            canvasPdf.drawText(
                    linea,
                    ANCHO_PAGINA / 2f,
                    posicionY,
                    paintPdf
            );

            posicionY += 14;
        }

        paintPdf.setTextAlign(
                Paint.Align.LEFT
        );
    }

    private Bitmap cargarBitmapReducido(
            String ruta,
            int anchoRequerido,
            int altoRequerido
    ) throws IOException {

        if (ruta == null ||
                ruta.trim().isEmpty()) {

            return null;
        }

        BitmapFactory.Options opciones =
                new BitmapFactory.Options();

        opciones.inJustDecodeBounds = true;

        decodificarImagen(
                ruta,
                opciones
        );

        opciones.inSampleSize =
                calcularInSampleSize(
                        opciones,
                        anchoRequerido,
                        altoRequerido
                );

        opciones.inJustDecodeBounds = false;

        return decodificarImagen(
                ruta,
                opciones
        );
    }

    private Bitmap decodificarImagen(
            String ruta,
            BitmapFactory.Options opciones
    ) throws IOException {

        if (ruta.startsWith("content://") ||
                ruta.startsWith("file://")) {

            Uri uri = Uri.parse(ruta);

            try (InputStream inputStream =
                         getContentResolver()
                                 .openInputStream(uri)) {

                if (inputStream == null) {
                    throw new IOException(
                            "No se pudo abrir la imagen"
                    );
                }

                return BitmapFactory.decodeStream(
                        inputStream,
                        null,
                        opciones
                );
            }
        }

        File archivo = new File(ruta);

        if (!archivo.exists()) {
            throw new IOException(
                    "La imagen no existe"
            );
        }

        return BitmapFactory.decodeFile(
                ruta,
                opciones
        );
    }

    private int calcularInSampleSize(
            BitmapFactory.Options opciones,
            int anchoRequerido,
            int altoRequerido
    ) {
        int altura = opciones.outHeight;
        int anchura = opciones.outWidth;

        int sampleSize = 1;

        if (altura <= 0 || anchura <= 0) {
            return sampleSize;
        }

        while (
                altura / sampleSize >
                        altoRequerido * 2 ||
                        anchura / sampleSize >
                                anchoRequerido * 2
        ) {
            sampleSize *= 2;
        }

        return sampleSize;
    }

    private String obtenerRutaEvidencia(
            int indice
    ) {
        String ruta = rutasEvidenciaTemp[indice];
        return ruta == null ? "" : ruta;
    }

    private int contarEvidencias() {
        int cantidad = 0;

        for (int i = 0;
             i < SUFIJOS_EVIDENCIAS.length;
             i++) {

            String ruta =
                    obtenerRutaEvidencia(i);

            if (!ruta.isEmpty()) {
                cantidad++;
            }
        }

        return cantidad;
    }

    private String[] obtenerUltimaValidacion() {
        String[] ultima = null;

        for (String[] revision : revisiones) {
            if (revision.length >= 5 &&
                    "Validada".equalsIgnoreCase(
                            revision[4]
                    )) {

                ultima = revision;
            }
        }

        return ultima;
    }

    private void guardarEstadoPdf(
            File archivo
    ) {
        String fecha =
                obtenerFechaActual();

        getSharedPreferences(
                PREFS_PDF,
                MODE_PRIVATE
        ).edit()
                .putString(
                        PREFIJO_ESTADO_PDF +
                                folio,
                        "Generado"
                )
                .putString(
                        PREFIJO_RUTA_PDF +
                                folio,
                        archivo.getAbsolutePath()
                )
                .putString(
                        PREFIJO_FECHA_PDF +
                                folio,
                        fecha
                )
                .apply();
    }

    private File obtenerArchivoPdfExistente() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PDF,
                        MODE_PRIVATE
                );

        String ruta = preferences.getString(
                PREFIJO_RUTA_PDF + folio,
                ""
        );

        if (ruta != null &&
                !ruta.trim().isEmpty()) {

            File archivo = new File(ruta);

            if (archivo.exists()) {
                return archivo;
            }
        }

        File archivoEsperado = new File(
                new File(
                        getFilesDir(),
                        "reportes"
                ),
                obtenerNombreArchivoPdf()
        );

        if (archivoEsperado.exists()) {
            return archivoEsperado;
        }

        return null;
    }

    private String obtenerNombreArchivoPdf() {
        String folioLimpio =
                folio.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        return "Reporte_Supervision_" +
                folioLimpio +
                ".pdf";
    }

    private void abrirPdf() {
        File archivo =
                obtenerArchivoPdfExistente();

        if (archivo == null ||
                !archivo.exists()) {

            Toast.makeText(
                    this,
                    "Primero debes generar el PDF",
                    Toast.LENGTH_SHORT
            ).show();

            actualizarEstadoPdf();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() +
                            ".fileprovider",
                    archivo
            );

            Intent intent = new Intent(
                    Intent.ACTION_VIEW
            );

            intent.setDataAndType(
                    uri,
                    "application/pdf"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Abrir reporte PDF"
                    )
            );

        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "No se encontró una aplicación para abrir PDF",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "No se pudo abrir el PDF",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void compartirPdf() {
        File archivo =
                obtenerArchivoPdfExistente();

        if (archivo == null ||
                !archivo.exists()) {

            Toast.makeText(
                    this,
                    "Primero debes generar el PDF",
                    Toast.LENGTH_SHORT
            ).show();

            actualizarEstadoPdf();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() +
                            ".fileprovider",
                    archivo
            );

            Intent intent = new Intent(
                    Intent.ACTION_SEND
            );

            intent.setType(
                    "application/pdf"
            );

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Reporte de supervisión " +
                            folio
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Reporte final de la supervisión " +
                            folio
            );

            intent.setClipData(
                    ClipData.newRawUri(
                            "Reporte PDF",
                            uri
                    )
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Compartir reporte PDF"
                    )
            );

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "No se pudo compartir el PDF",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String obtenerFechaActual() {
        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(
                Calendar.getInstance().getTime()
        );
    }

    private String valorVisible(
            String valor,
            String alternativo
    ) {
        if (valor == null ||
                valor.trim().isEmpty()) {

            return alternativo;
        }

        return valor.trim();
    }

    private void mostrarSupervisionNoEncontrada() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión no encontrada"
                )
                .setMessage(
                        "No fue posible encontrar la información " +
                                "de la supervisión."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Cerrar",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }
}