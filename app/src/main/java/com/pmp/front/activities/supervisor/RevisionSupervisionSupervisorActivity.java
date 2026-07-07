package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

        if (cargarSupervision()) {
            cargarChecklist();
            cargarReporte();
            cargarEvidencias();
            cargarUltimaRevision();
        }
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

        if (!"supervisor".equalsIgnoreCase(
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
                v -> validarDevolucion()
        );

        btnValidarRevision.setOnClickListener(v -> {
            if (supervisionActual != null &&
                    "Finalizada".equalsIgnoreCase(
                            supervisionActual[11]
                    )) {

                abrirModuloPdf();

            } else {
                confirmarValidacion();
            }
        });
    }

    private boolean cargarSupervision() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            mostrarError();
            return false;
        }

        String[] registros =
                datos.split("\n");

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (!folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            supervisionActual =
                    partes;

            mostrarSupervision();
            configurarModoRevision();

            return true;
        }

        mostrarError();

        return false;
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
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_CHECKLISTS,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_CHECKLISTS,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            txtChecklistRevision.setText(
                    "No se encontró el checklist."
            );

            return;
        }

        String[] registros =
                datos.split("\n");

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 13) {
                continue;
            }

            if (!folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            String observaciones =
                    partes[8].trim();

            if (observaciones.isEmpty()) {
                observaciones =
                        "Sin observaciones";
            }

            String contenido =
                    "Área delimitada: " +
                            respuestaVisible(
                                    partes[1]
                            ) +
                            "\n\n" +

                            "Equipo de protección personal: " +
                            respuestaVisible(
                                    partes[2]
                            ) +
                            "\n\n" +

                            "Corte visible: " +
                            respuestaVisible(
                                    partes[3]
                            ) +
                            "\n\n" +

                            "Detección de corte de potencial: " +
                            respuestaVisible(
                                    partes[4]
                            ) +
                            "\n\n" +

                            "Cero metales: " +
                            respuestaVisible(
                                    partes[5]
                            ) +
                            "\n\n" +

                            "Actividades que salvan vidas: " +
                            respuestaVisible(
                                    partes[6]
                            ) +
                            "\n\n" +

                            "Llenado correcto de RIM: " +
                            respuestaVisible(
                                    partes[7]
                            ) +
                            "\n\n" +

                            "Observaciones:\n" +
                            observaciones +
                            "\n\n" +

                            "Progreso: " +
                            partes[9] +
                            "\n" +

                            "Fecha: " +
                            partes[10] +
                            "\n" +

                            "Supervisor: " +
                            partes[11] +
                            "\n" +

                            "Estado: " +
                            partes[12];

            txtChecklistRevision.setText(
                    contenido
            );

            return;
        }

        txtChecklistRevision.setText(
                "No se encontró el checklist asociado."
        );
    }

    private void cargarReporte() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_REPORTES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_REPORTES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            txtReporteRevision.setText(
                    "No se encontró el reporte técnico."
            );

            return;
        }

        String[] registros =
                datos.split("\n");

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 12) {
                continue;
            }

            if (!folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            String contenido =
                    "Número y año de notificación:\n" +
                            partes[1] +
                            "\n\n" +

                            "KWh:\n" +
                            partes[2] +
                            "\n\n" +

                            "Importe:\n$ " +
                            partes[3] +
                            "\n\n" +

                            "RPU de notificación:\n" +
                            partes[4] +
                            "\n\n" +

                            "Número de orden de corte:\n" +
                            partes[5] +
                            "\n\n" +

                            "Tarifa:\n" +
                            partes[6] +
                            "\n\n" +

                            "Estatus del servicio:\n" +
                            partes[7] +
                            "\n\n" +

                            "Datos del corte:\n" +
                            partes[8] +
                            "\n\n" +

                            "Último guardado: " +
                            partes[9] +
                            "\n" +

                            "Usuario Técnico: " +
                            partes[10] +
                            "\n" +

                            "Estado: " +
                            partes[11];

            txtReporteRevision.setText(
                    contenido
            );

            return;
        }

        txtReporteRevision.setText(
                "No se encontró el reporte asociado."
        );
    }

    private void cargarEvidencias() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_EVIDENCIAS,
                        MODE_PRIVATE
                );

        for (int i = 0;
             i < SUFIJOS_EVIDENCIAS.length;
             i++) {

            String clave =
                    folio +
                            "_" +
                            SUFIJOS_EVIDENCIAS[i];

            String ruta =
                    preferences.getString(
                            clave,
                            ""
                    );

            mostrarEvidencia(
                    i,
                    ruta == null
                            ? ""
                            : ruta.trim()
            );
        }
    }

    private void mostrarEvidencia(
            int indice,
            String ruta
    ) {
        ImageView imageView =
                imagenesEvidencias[indice];

        TextView estado =
                estadosEvidencias[indice];

        if (ruta.isEmpty()) {
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

        try {
            Bitmap bitmap =
                    cargarBitmapReducido(
                            ruta,
                            dp(360),
                            dp(220)
                    );

            if (bitmap == null) {
                throw new IOException(
                        "No se pudo decodificar la imagen"
                );
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

        } catch (Exception exception) {
            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_INSIDE
            );

            imageView.setImageResource(
                    android.R.drawable.ic_delete
            );

            estado.setText(
                    "No se pudo cargar la evidencia"
            );
        }
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
                    "Escribe el motivo de la devolución"
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
                    "tecnico",
                    "Supervisión devuelta",
                    "La supervisión " +
                            folio +
                            " fue devuelta con observaciones. " +
                            "Revisa las indicaciones del Supervisor " +
                            "y corrige la información.",
                    "DEVOLUCION",
                    folio
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
                    "tecnico",
                    "Supervisión validada",
                    "La supervisión " +
                            folio +
                            " fue validada correctamente y " +
                            "cambió al estado Finalizada.",
                    "VALIDACION",
                    folio
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
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            return;
        }

        String[] registros =
                datos.split("\n");

        StringBuilder actualizados =
                new StringBuilder();

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length >= 15 &&
                    folio.equalsIgnoreCase(
                            partes[0].trim()
                    )) {

                partes[11] =
                        estadoGeneral;

                partes[13] =
                        estadoReporte;

                agregarRegistro(
                        actualizados,
                        unirPartes(partes)
                );

            } else {
                agregarRegistro(
                        actualizados,
                        registro
                );
            }
        }

        preferences.edit()
                .putString(
                        KEY_SUPERVISIONES,
                        actualizados.toString()
                )
                .apply();

        if (supervisionActual != null &&
                supervisionActual.length >= 15) {

            supervisionActual[11] =
                    estadoGeneral;

            supervisionActual[13] =
                    estadoReporte;
        }
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