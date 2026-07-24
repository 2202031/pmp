package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.Config;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionSupervisionSupervisorActivity
        extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_NOMBRE =
            "nombre_actual";

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

    private static final String PREFS_EVIDENCIAS =
            "evidencias_local";

    private static final String PREFS_REVISIONES =
            "revisiones_local";

    private static final String KEY_REVISIONES =
            "revisiones";

    private static final String[] SUFIJOS_EVIDENCIAS = {
            "foto_corte",
            "foto_fachada",
            "foto_medidor",
            "foto_selfi"
    };

    private TextView btnVolver;

    private TextView txtFolioRevision;
    private TextView txtCircuitoFechaRevision;
    private TextView txtEstadoRevision;
    private TextView txtModoRevision;

    private TextView txtDatosSupervisionRevision;
    private TextView txtChecklistRevision;
    private TextView txtReporteRevision;
    private TextView txtHistorialRevision;

    private ImageView[] imagenesEvidencias;
    private TextView[] estadosEvidencias;

    private EditText etObservacionesRevision;

    private TextView btnDevolverRevision;
    private TextView btnValidarRevision;

    private String folio;
    private String supervisorActual;

    private String[] supervisionActual;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean revisionActiva = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_revision_supervision_supervisor
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        folio =
                getIntent().getStringExtra(
                        "folio"
                );

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

        folio =
                folio.trim();

        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (folio == null ||
                folio.trim().isEmpty()) {

            return;
        }

        cargarSupervisionAsync(() -> {
            cargarChecklist();
            cargarReporte();
            cargarEvidencias();
            cargarUltimaRevision();
        });
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(
                        R.id.btnVolver
                );

        txtFolioRevision =
                findViewById(
                        R.id.txtFolioRevision
                );

        txtCircuitoFechaRevision =
                findViewById(
                        R.id.txtCircuitoFechaRevision
                );

        txtEstadoRevision =
                findViewById(
                        R.id.txtEstadoRevision
                );

        txtModoRevision =
                findViewById(
                        R.id.txtModoRevision
                );

        txtDatosSupervisionRevision =
                findViewById(
                        R.id.txtDatosSupervisionRevision
                );

        txtChecklistRevision =
                findViewById(
                        R.id.txtChecklistRevision
                );

        txtReporteRevision =
                findViewById(
                        R.id.txtReporteRevision
                );

        txtHistorialRevision =
                findViewById(
                        R.id.txtHistorialRevision
                );

        etObservacionesRevision =
                findViewById(
                        R.id.etObservacionesRevision
                );

        btnDevolverRevision =
                findViewById(
                        R.id.btnDevolverRevision
                );

        btnValidarRevision =
                findViewById(
                        R.id.btnValidarRevision
                );

        imagenesEvidencias =
                new ImageView[]{
                        findViewById(
                                R.id.imgRevisionCorte
                        ),
                        findViewById(
                                R.id.imgRevisionFachada
                        ),
                        findViewById(
                                R.id.imgRevisionMedidor
                        ),
                        findViewById(
                                R.id.imgRevisionSelfi
                        )
                };

        estadosEvidencias =
                new TextView[]{
                        findViewById(
                                R.id.txtRevisionCorte
                        ),
                        findViewById(
                                R.id.txtRevisionFachada
                        ),
                        findViewById(
                                R.id.txtRevisionMedidor
                        ),
                        findViewById(
                                R.id.txtRevisionSelfi
                        )
                };
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        supervisorActual =
                preferences.getString(
                        KEY_NOMBRE,
                        "Administrador"
                );

        String rol =
                preferences.getString(
                        KEY_ROL,
                        ""
                );

        if (supervisorActual == null ||
                supervisorActual.trim().isEmpty()) {

            supervisorActual =
                    "Administrador";
        }

        if (!"administrador".equalsIgnoreCase(
                rol
        )) {
            Intent intent =
                    new Intent(
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
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnDevolverRevision.setOnClickListener(
                v -> {
                    if (sinRedAviso()) return;
                    validarDevolucion();
                }
        );

        btnValidarRevision.setOnClickListener(v -> {
            // "Abrir PDF" (cuando ya está finalizada) no requiere red obligatoria,
            // pero validar/enviar sí. Verificamos solo cuando se va a validar.
            if (supervisionActual != null &&
                    "Finalizada".equalsIgnoreCase(
                            supervisionActual[11]
                    )) {

                abrirModuloPdf();

            } else {
                if (sinRedAviso()) return;
                confirmarValidacion();
            }
        });
    }

    // Devuelve true (y avisa) si NO hay conexión de red. Bloquea la acción.
    private boolean sinRedAviso() {
        if (!com.pmp.front.RedHelper.hayConexion(this)) {
            Toast.makeText(this,
                    "Sin conexión a la red. Conéctate al WiFi para validar o devolver.",
                    Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void cargarSupervisionAsync(Runnable siEncontrada) {
        executorService.execute(() -> {
            String[] partesSinteticas = null;

            try {
                URL url = new URL(Config.BASE_URL + "/api/asignaciones/" + folio);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    JSONObject json = new JSONObject(sb.toString());

                    partesSinteticas = new String[15];
                    partesSinteticas[0] = json.optString("folio", "");
                    partesSinteticas[1] = json.optString("fecha", "");
                    partesSinteticas[2] = "";
                    partesSinteticas[3] = json.optString("lugar", "");
                    partesSinteticas[4] = json.optString("prioridad", "");
                    partesSinteticas[5] = json.optString("descripcion", "");
                    partesSinteticas[6] = json.optString("observaciones", "");
                    partesSinteticas[7] = json.optString("usernameSupervisor", "");
                    partesSinteticas[8] = json.optString("tecnico", "");
                    partesSinteticas[9] = "";
                    partesSinteticas[10] = ""; // personal de apoyo (no manejado en servidor)
                    partesSinteticas[11] = json.optString("estado", "");
                    partesSinteticas[12] = "";
                    partesSinteticas[13] = "Verificada".equalsIgnoreCase(json.optString("estado", "")) ? "Validado" : "Pendiente";
                    partesSinteticas[14] = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String[] finalPartes = partesSinteticas;
            mainHandler.post(() -> {
                if (finalPartes == null) {
                    mostrarError();
                    return;
                }

                supervisionActual = finalPartes;
                mostrarSupervision();
                configurarModoRevision();

                if (siEncontrada != null) {
                    siEncontrada.run();
                }
            });
        });
    }

    private void mostrarSupervision() {
        String apoyos =
                supervisionActual[10].trim();

        if (apoyos.isEmpty()) {
            apoyos =
                    "Sin personal de apoyo";
        }

        String observacionesIniciales =
                supervisionActual[6].trim();

        if (observacionesIniciales.isEmpty()) {
            observacionesIniciales =
                    "Sin observaciones iniciales";
        }

        txtFolioRevision.setText(
                supervisionActual[0]
        );

        txtCircuitoFechaRevision.setText(
                supervisionActual[2] +
                        " • " +
                        supervisionActual[1]
        );

        txtEstadoRevision.setText(
                supervisionActual[11]
        );

        String datos =
                "Lugar o referencia:\n" +
                        supervisionActual[3] +
                        "\n\n" +

                        "Prioridad:\n" +
                        supervisionActual[4] +
                        "\n\n" +

                        "Descripción:\n" +
                        supervisionActual[5] +
                        "\n\n" +

                        "Observaciones iniciales:\n" +
                        observacionesIniciales +
                        "\n\n" +

                        "Técnico responsable:\n" +
                        supervisionActual[8] +
                        "\n\n" +

                        "Personal de apoyo:\n" +
                        apoyos +
                        "\n\n" +

                        "Estado del checklist: " +
                        supervisionActual[12] +
                        "\n" +

                        "Estado del reporte: " +
                        supervisionActual[13];

        txtDatosSupervisionRevision.setText(
                datos
        );
    }

    private void configurarModoRevision() {
        String estadoGeneral =
                supervisionActual[11];

        String estadoReporte =
                supervisionActual[13];

        revisionActiva =
                "Pendiente de revisión"
                        .equalsIgnoreCase(
                                estadoGeneral
                        ) ||
                        "Enviado"
                                .equalsIgnoreCase(
                                        estadoReporte
                                );

        etObservacionesRevision.setEnabled(
                revisionActiva
        );

        if (revisionActiva) {
            btnDevolverRevision.setEnabled(
                    true
            );

            btnDevolverRevision.setAlpha(
                    1f
            );

            btnValidarRevision.setEnabled(
                    true
            );

            btnValidarRevision.setAlpha(
                    1f
            );

            txtModoRevision.setText(
                    "Revisa el checklist, el reporte y las " +
                            "cuatro evidencias antes de tomar una decisión."
            );

            btnDevolverRevision.setText(
                    "Devolver con observaciones"
            );

            btnValidarRevision.setText(
                    "Validar supervisión"
            );

        } else if ("Finalizada"
                .equalsIgnoreCase(
                        estadoGeneral
                )) {

            etObservacionesRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setAlpha(
                    0.55f
            );

            btnValidarRevision.setEnabled(
                    true
            );

            btnValidarRevision.setAlpha(
                    1f
            );

            txtModoRevision.setText(
                    "La supervisión está validada. Ya puedes " +
                            "generar el reporte final en PDF."
            );

            btnDevolverRevision.setText(
                    "Revisión cerrada"
            );

            btnValidarRevision.setText(
                    "Abrir reporte PDF"
            );

        } else if ("Con observaciones"
                .equalsIgnoreCase(
                        estadoGeneral
                )) {

            etObservacionesRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setAlpha(
                    0.55f
            );

            btnValidarRevision.setEnabled(
                    false
            );

            btnValidarRevision.setAlpha(
                    0.55f
            );

            txtModoRevision.setText(
                    "La supervisión fue devuelta al Técnico y " +
                            "está pendiente de corrección."
            );

            btnDevolverRevision.setText(
                    "Devuelta al Técnico"
            );

            btnValidarRevision.setText(
                    "Pendiente de reenvío"
            );

        } else {
            etObservacionesRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setEnabled(
                    false
            );

            btnDevolverRevision.setAlpha(
                    0.55f
            );

            btnValidarRevision.setEnabled(
                    false
            );

            btnValidarRevision.setAlpha(
                    0.55f
            );

            txtModoRevision.setText(
                    "La supervisión todavía no se encuentra " +
                            "disponible para revisión."
            );

            btnDevolverRevision.setText(
                    "Revisión no disponible"
            );

            btnValidarRevision.setText(
                    "Revisión no disponible"
            );
        }
    }

    private void cargarChecklist() {
        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/checklists/buscar/" + folio);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    String respuesta = sb.toString().trim();

                    mainHandler.post(() -> {
                        if (respuesta.isEmpty() || respuesta.equals("\"\"")) {
                            txtChecklistRevision.setText("No se encontró el checklist.");
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(respuesta);

                            String observaciones = json.optString("observaciones", "");
                            if (observaciones.trim().isEmpty()) observaciones = "Sin observaciones";

                            String contenido =
                                    "Área delimitada: " + respuestaVisible(json.optString("areaDelimitada", "")) + "\n\n" +
                                    "Equipo de protección personal: " + respuestaVisible(json.optString("equipoProteccion", "")) + "\n\n" +
                                    "Corte visible: " + respuestaVisible(json.optString("corteVisible", "")) + "\n\n" +
                                    "Cero metales: " + respuestaVisible(json.optString("ceroMetales", "")) + "\n\n" +
                                    "Actividades que salvan vidas: " + respuestaVisible(json.optString("actividadesSalvanVidas", "")) + "\n\n" +
                                    "Llenado correcto de RIM: " + respuestaVisible(json.optString("llenadoRim", "")) + "\n\n" +
                                    "Observaciones:\n" + observaciones + "\n\n" +
                                    "Estado: " + json.optString("estado", "Pendiente");

                            txtChecklistRevision.setText(contenido);
                        } catch (Exception e) {
                            txtChecklistRevision.setText("No se encontró el checklist.");
                        }
                    });
                } else {
                    mainHandler.post(() -> txtChecklistRevision.setText("No se encontró el checklist asociado."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> txtChecklistRevision.setText("No se encontró el checklist asociado."));
            }
        });
    }

    private void cargarReporte() {
        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/creacion-reporte/" + folio);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    JSONObject json = new JSONObject(sb.toString());

                    mainHandler.post(() -> {
                        if (json.length() == 0 || json.opt("folio") == null) {
                            txtReporteRevision.setText("No se encontró el reporte técnico.");
                            return;
                        }

                        String statusCombinado = json.optString("statusServicio", "");
                        String[] partesStatus = statusCombinado.split("\\|\\|", -1);

                        String contenido =
                                "Número y año de notificación:\n" + json.optString("anioNotificacion", "") + "\n\n" +
                                "KWh:\n" + json.optString("kwh", "") + "\n\n" +
                                "Importe:\n$ " + json.optString("importe", "") + "\n\n" +
                                "RPU de notificación:\n" + json.optString("rpu", "") + "\n\n" +
                                "Número de orden de corte:\n" + json.optString("numeroCorte", "") + "\n\n" +
                                "Tarifa:\n" + json.optString("tarifa", "") + "\n\n" +
                                "Estatus del servicio:\n" + (partesStatus.length > 0 ? partesStatus[0] : "") + "\n\n" +
                                "Datos del corte:\n" + (partesStatus.length > 1 ? partesStatus[1] : "");

                        txtReporteRevision.setText(contenido);
                    });
                } else {
                    mainHandler.post(() -> txtReporteRevision.setText("No se encontró el reporte asociado."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> txtReporteRevision.setText("No se encontró el reporte asociado."));
            }
        });
    }

    private void cargarEvidencias() {
        for (int i = 0; i < SUFIJOS_EVIDENCIAS.length; i++) {
            final int indice = i;
            String tipo = SUFIJOS_EVIDENCIAS[i].replace("foto_", "");

            executorService.execute(() -> {
                Bitmap bitmap = null;
                try {
                    URL url = new URL(Config.BASE_URL + "/api/creacion-reporte/" + folio + "/foto/" + tipo);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] datos = new byte[4096];
                        int leidos;
                        while ((leidos = is.read(datos)) != -1) {
                            buffer.write(datos, 0, leidos);
                        }
                        bitmap = BitmapFactory.decodeByteArray(buffer.toByteArray(), 0, buffer.size());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Bitmap finalBitmap = bitmap;
                mainHandler.post(() -> mostrarEvidencia(indice, finalBitmap));
            });
        }
    }

    private void mostrarEvidencia(
            int indice,
            Bitmap bitmap
    ) {
        ImageView imageView =
                imagenesEvidencias[indice];

        TextView estado =
                estadosEvidencias[indice];

        if (bitmap == null) {
            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_INSIDE
            );

            imageView.setImageResource(
                    android.R.drawable.ic_menu_camera
            );

            estado.setText(
                    "Evidencia no registrada"
            );

            return;
        }

        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        imageView.setImageBitmap(
                bitmap
        );

        estado.setText(
                "Evidencia registrada"
        );
    }

    private Bitmap cargarBitmapReducido(
            String ruta,
            int anchoRequerido,
            int altoRequerido
    ) throws IOException {

        BitmapFactory.Options opciones =
                new BitmapFactory.Options();

        opciones.inJustDecodeBounds =
                true;

        if (ruta.startsWith("content://") ||
                ruta.startsWith("file://")) {

            Uri uri =
                    Uri.parse(ruta);

            try (InputStream inputStream =
                         getContentResolver()
                                 .openInputStream(uri)) {

                if (inputStream == null) {
                    throw new IOException(
                            "No se pudo abrir la imagen"
                    );
                }

                BitmapFactory.decodeStream(
                        inputStream,
                        null,
                        opciones
                );
            }

        } else {
            File archivo =
                    new File(ruta);

            if (!archivo.exists()) {
                throw new IOException(
                        "El archivo no existe"
                );
            }

            BitmapFactory.decodeFile(
                    ruta,
                    opciones
            );
        }

        opciones.inSampleSize =
                calcularInSampleSize(
                        opciones,
                        anchoRequerido,
                        altoRequerido
                );

        opciones.inJustDecodeBounds =
                false;

        if (ruta.startsWith("content://") ||
                ruta.startsWith("file://")) {

            Uri uri =
                    Uri.parse(ruta);

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
        int altura =
                opciones.outHeight;

        int anchura =
                opciones.outWidth;

        int sampleSize =
                1;

        if (altura <= 0 ||
                anchura <= 0) {

            return sampleSize;
        }

        if (altura > altoRequerido ||
                anchura > anchoRequerido) {

            int mitadAltura =
                    altura / 2;

            int mitadAnchura =
                    anchura / 2;

            while (mitadAltura / sampleSize >=
                    altoRequerido &&
                    mitadAnchura / sampleSize >=
                            anchoRequerido) {

                sampleSize *=
                        2;
            }
        }

        return sampleSize;
    }

    private void cargarUltimaRevision() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_REVISIONES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_REVISIONES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            txtHistorialRevision.setText(
                    "Esta supervisión todavía no tiene " +
                            "revisiones anteriores."
            );

            return;
        }

        String[] registros =
                datos.split("\n");

        String[] ultimaRevision =
                null;

        for (String registro :
                registros) {

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
                ultimaRevision =
                        partes;
            }
        }

        if (ultimaRevision == null) {
            txtHistorialRevision.setText(
                    "Esta supervisión todavía no tiene " +
                            "revisiones anteriores."
            );

            return;
        }

        String observaciones =
                ultimaRevision[1].trim();

        if (observaciones.isEmpty()) {
            observaciones =
                    "Sin observaciones";
        }

        String contenido =
                "Resultado: " +
                        ultimaRevision[4] +
                        "\n" +

                        "Fecha: " +
                        ultimaRevision[2] +
                        "\n" +

                        "Supervisor: " +
                        ultimaRevision[3] +
                        "\n\n" +

                        "Observaciones:\n" +
                        observaciones;

        txtHistorialRevision.setText(
                contenido
        );

        if (!revisionActiva) {
            etObservacionesRevision.setText(
                    ultimaRevision[1]
            );
        }
    }

    private void validarDevolucion() {
        if (!revisionActiva) {
            return;
        }

        String observaciones =
                obtenerObservaciones();

        if (observaciones.isEmpty()) {
            etObservacionesRevision.setError(
                    "Escribe el motivo de la devolución. Es obligatorio para que el Supervisor sepa qué corregir."
            );

            etObservacionesRevision.requestFocus();

            return;
        }

        if (observaciones.length() < 10) {
            etObservacionesRevision.setError(
                    "Explica mejor el motivo (al menos 10 caracteres). Indica qué debe corregirse."
            );
            etObservacionesRevision.requestFocus();
            return;
        }

        if (observaciones.length() > 2000) {
            etObservacionesRevision.setError(
                    "Las observaciones son demasiado largas (máximo 2000 caracteres). Actualmente tiene " + observaciones.length() + "."
            );
            etObservacionesRevision.requestFocus();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Devolver al Técnico"
                )
                .setMessage(
                        "¿Deseas devolver esta supervisión " +
                                "con las observaciones registradas?\n\n" +
                                "El Técnico podrá modificar el reporte " +
                                "y reemplazar evidencias."
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Devolver",
                        (dialog, which) ->
                                devolverSupervision(
                                        observaciones
                                )
                )
                .show();
    }

    private void devolverSupervision(
            String observaciones
    ) {
        guardarRevision(
                observaciones,
                "Devuelta"
        );

        actualizarEstadosSupervision(
                "Con observaciones",
                "Con observaciones"
        );

        String usuarioTecnico =
                obtenerUsuarioTecnico();

        if (!usuarioTecnico.isEmpty()) {
            NotificacionesHelper.crear(
                    this,
                    usuarioTecnico,
                    "supervisor",
                    "Supervisión devuelta",
                    "La supervisión " +
                            folio +
                            " fue devuelta con observaciones. " +
                            "Revisa las indicaciones del Supervisor " +
                            "y corrige la información.",
                    "DEVOLUCION",
                    folio,
                    exito -> {}
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión devuelta"
                )
                .setMessage(
                        "La supervisión fue devuelta al Técnico.\n\n" +
                                "Podrá corregir la información y " +
                                "enviarla nuevamente."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Aceptar",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void confirmarValidacion() {
        if (!revisionActiva) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Validar supervisión"
                )
                .setMessage(
                        "¿Confirmas que el checklist, el reporte " +
                                "y las evidencias son correctos?\n\n" +
                                "La supervisión cambiará a Finalizada."
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Validar",
                        (dialog, which) ->
                                validarSupervision()
                )
                .show();
    }

    private void validarSupervision() {
        guardarRevision(
                obtenerObservaciones(),
                "Validada"
        );

        actualizarEstadosSupervision(
                "Finalizada",
                "Validado"
        );

        String usuarioTecnico =
                obtenerUsuarioTecnico();

        if (!usuarioTecnico.isEmpty()) {
            NotificacionesHelper.crear(
                    this,
                    usuarioTecnico,
                    "supervisor",
                    "Supervisión validada",
                    "La supervisión " +
                            folio +
                            " fue validada correctamente y " +
                            "cambió al estado Finalizada.",
                    "VALIDACION",
                    folio,
                    exito -> {}
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión validada"
                )
                .setMessage(
                        "La supervisión fue validada correctamente.\n\n" +
                                "Ya puedes generar el reporte final en PDF."
                )
                .setCancelable(false)
                .setNegativeButton(
                        "Cerrar",
                        (dialog, which) ->
                                finish()
                )
                .setPositiveButton(
                        "Abrir PDF",
                        (dialog, which) -> {
                            Intent intent =
                                    new Intent(
                                            RevisionSupervisionSupervisorActivity.this,
                                            ReportePdfSupervisorActivity.class
                                    );

                            intent.putExtra(
                                    "folio",
                                    folio
                            );

                            startActivity(
                                    intent
                            );

                            finish();
                        }
                )
                .show();
    }

    private void guardarRevision(
            String observaciones,
            String resultado
    ) {
        String fecha =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        /*
         * Formato del historial:
         *
         * 0 folio
         * 1 observaciones
         * 2 fecha
         * 3 supervisor
         * 4 resultado
         */

        String registro =
                limpiar(folio) +
                        "|" +

                        limpiar(observaciones) +
                        "|" +

                        fecha +
                        "|" +

                        limpiar(supervisorActual) +
                        "|" +

                        limpiar(resultado);

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_REVISIONES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_REVISIONES,
                        ""
                );

        String actualizados;

        if (datos == null ||
                datos.trim().isEmpty()) {

            actualizados =
                    registro;

        } else {
            actualizados =
                    datos.trim() +
                            "\n" +
                            registro;
        }

        preferences.edit()
                .putString(
                        KEY_REVISIONES,
                        actualizados
                )
                .apply();
    }

    private void actualizarEstadosSupervision(
            String estadoGeneral,
            String estadoReporte
    ) {
        if (supervisionActual != null &&
                supervisionActual.length >= 15) {

            supervisionActual[11] = estadoGeneral;
            supervisionActual[13] = estadoReporte;
        }

        executorService.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("estado", estadoGeneral);

                URL url = new URL(Config.BASE_URL + "/api/asignaciones/" + folio + "/estado");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(8000);

                try (java.io.OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String obtenerUsuarioTecnico() {
        if (supervisionActual == null ||
                supervisionActual.length < 15) {

            return "";
        }

        if (supervisionActual[7] == null) {
            return "";
        }

        return supervisionActual[7]
                .trim();
    }

    private void abrirModuloPdf() {
        if (supervisionActual == null) {
            return;
        }

        if (!"Finalizada".equalsIgnoreCase(
                supervisionActual[11]
        )) {
            Toast.makeText(
                    this,
                    "La supervisión todavía no está finalizada",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        RevisionSupervisionSupervisorActivity.this,
                        ReportePdfSupervisorActivity.class
                );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(
                intent
        );
    }

    private String obtenerObservaciones() {
        return etObservacionesRevision
                .getText()
                .toString()
                .trim();
    }

    private String respuestaVisible(
            String respuesta
    ) {
        if (respuesta == null ||
                respuesta.trim().isEmpty()) {

            return "Sin respuesta";
        }

        return respuesta.trim();
    }

    private void mostrarError() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión no encontrada"
                )
                .setMessage(
                        "No fue posible encontrar la información " +
                                "de esta supervisión."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Cerrar",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void agregarRegistro(
            StringBuilder builder,
            String registro
    ) {
        if (builder.length() > 0) {
            builder.append("\n");
        }

        builder.append(
                registro
        );
    }

    private String unirPartes(
            String[] partes
    ) {
        StringBuilder resultado =
                new StringBuilder();

        for (int i = 0;
             i < partes.length;
             i++) {

            if (i > 0) {
                resultado.append("|");
            }

            resultado.append(
                    partes[i]
            );
        }

        return resultado.toString();
    }

    private String limpiar(
            String texto
    ) {
        if (texto == null) {
            return "";
        }

        return texto
                .trim()
                .replace("|", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private int dp(
            int value
    ) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}