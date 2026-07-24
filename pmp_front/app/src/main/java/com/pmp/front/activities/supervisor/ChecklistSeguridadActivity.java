package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.Config;
import com.pmp.front.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChecklistSeguridadActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_NOMBRE = "nombre_actual";

    private TextView btnVolver, btnGuardarAvance, btnCompletarChecklist;
    private TextView txtFolioChecklist, txtCircuitoChecklist, txtResponsableChecklist;
    private TextView txtProgresoChecklist, txtEstadoChecklist, txtFechaChecklist, txtSupervisorChecklist;
    private EditText etObservacionesChecklist;

    private RadioGroup rgAreaDelimitada, rgEquipoProteccion, rgCorteVisible, rgDeteccionPotencial;
    private RadioGroup rgCeroMetales, rgActividadesSalvanVidas, rgLlenadoRim;

    private String folio;
    private String circuito;

    // IMPORTANTE: Este campo debe recibir de la pantalla anterior el "username" único del técnico responsable
    private String responsable;
    private boolean checklistCompletado = false;

    // Hilos para comunicación asíncrona con el Backend de Spring Boot
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_seguridad);

        inicializarVistas();

        if (!cargarDatosSupervision()) {
            return;
        }

        mostrarDatosSupervision();
        configurarEventos();

        // Obtiene el estado actual del checklist desde MySQL remoto
        cargarChecklistGuardado();
        actualizarProgreso();
    }

    private void inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver);
        btnGuardarAvance = findViewById(R.id.btnGuardarAvance);
        btnCompletarChecklist = findViewById(R.id.btnCompletarChecklist);
        txtFolioChecklist = findViewById(R.id.txtFolioChecklist);
        txtCircuitoChecklist = findViewById(R.id.txtCircuitoChecklist);
        txtResponsableChecklist = findViewById(R.id.txtResponsableChecklist);
        txtProgresoChecklist = findViewById(R.id.txtProgresoChecklist);
        txtEstadoChecklist = findViewById(R.id.txtEstadoChecklist);
        txtFechaChecklist = findViewById(R.id.txtFechaChecklist);
        txtSupervisorChecklist = findViewById(R.id.txtSupervisorChecklist);
        etObservacionesChecklist = findViewById(R.id.etObservacionesChecklist);
        rgAreaDelimitada = findViewById(R.id.rgAreaDelimitada);
        rgEquipoProteccion = findViewById(R.id.rgEquipoProteccion);
        rgCorteVisible = findViewById(R.id.rgCorteVisible);
        rgDeteccionPotencial = findViewById(R.id.rgDeteccionPotencial);
        rgCeroMetales = findViewById(R.id.rgCeroMetales);
        rgActividadesSalvanVidas = findViewById(R.id.rgActividadesSalvanVidas);
        rgLlenadoRim = findViewById(R.id.rgLlenadoRim);
    }

    private boolean cargarDatosSupervision() {
        folio = getIntent().getStringExtra("folio");
        circuito = getIntent().getStringExtra("circuito");

        // CORREGIDO: Aseguramos obtener el "username" enviado por la vista anterior
        responsable = getIntent().getStringExtra("responsable");

        if (folio == null || folio.trim().isEmpty()) {
            Toast.makeText(this, "No se encontró el folio de la supervisión", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        folio = folio.trim();
        if (circuito == null) circuito = "";
        if (responsable == null) responsable = "";
        return true;
    }

    private void mostrarDatosSupervision() {
        txtFolioChecklist.setText("Folio: " + folio);
        txtCircuitoChecklist.setText("Circuito: " + circuito);
        txtResponsableChecklist.setText("Técnico responsable: " + responsable);
        txtEstadoChecklist.setText("Estado: Pendiente");
        txtFechaChecklist.setText("Fecha: Sin guardar");
        txtSupervisorChecklist.setText("Supervisor: " + obtenerSupervisorActual());
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v -> finish());

        RadioGroup.OnCheckedChangeListener listener = (group, checkedId) -> actualizarProgreso();
        rgAreaDelimitada.setOnCheckedChangeListener(listener);
        rgEquipoProteccion.setOnCheckedChangeListener(listener);
        rgCorteVisible.setOnCheckedChangeListener(listener);
        rgDeteccionPotencial.setOnCheckedChangeListener(listener);
        rgCeroMetales.setOnCheckedChangeListener(listener);
        rgActividadesSalvanVidas.setOnCheckedChangeListener(listener);
        rgLlenadoRim.setOnCheckedChangeListener(listener);

        btnGuardarAvance.setOnClickListener(v -> {
            if (sinRedAviso()) return;
            guardarChecklist(false);
        });
        btnCompletarChecklist.setOnClickListener(v -> {
            if (sinRedAviso()) return;
            confirmarCompletarChecklist();
        });
    }

    private void confirmarCompletarChecklist() {
        int respondidas = contarRespuestas();
        if (respondidas < 7) {
            new AlertDialog.Builder(this)
                    .setTitle("Checklist incompleto")
                    .setMessage("Debes responder los siete puntos antes de completar el checklist.\n\nProgreso actual: " + respondidas + " de 7.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Completar checklist")
                .setMessage("¿Confirmas que la información es correcta?\n\nAl completarlo se habilitará el reporte para el Técnico responsable.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Completar", (dialog, which) -> guardarChecklist(true))
                .show();
    }

    private boolean sinRedAviso() {
        if (!com.pmp.front.RedHelper.hayConexion(this)) {
            Toast.makeText(this,
                    "Sin conexión a la red. Conéctate al WiFi para guardar el checklist.",
                    Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void guardarChecklist(boolean completar) {
        if (checklistCompletado) {
            Toast.makeText(this, "El checklist ya está completado", Toast.LENGTH_SHORT).show();
            return;
        }

        int respondidas = contarRespuestas();
        if (completar && respondidas < 7) return;

        String estado = completar ? "Completado" : "Pendiente";
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String supervisor = obtenerSupervisorActual();
        String observaciones = etObservacionesChecklist.getText().toString().trim();

        // Observaciones -> columna descripcion TEXT (opcional, limitamos a algo razonable)
        String errorObs = com.pmp.front.Validaciones.opcional(observaciones, "las observaciones", 2000);
        if (errorObs != null) {
            etObservacionesChecklist.setError(errorObs);
            etObservacionesChecklist.requestFocus();
            return;
        }

        btnGuardarAvance.setEnabled(false);
        btnCompletarChecklist.setEnabled(false);

        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/checklists/guardar");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("folio", folio);
                jsonParam.put("areaDelimitada", obtenerRespuesta(rgAreaDelimitada));
                jsonParam.put("equipoProteccion", obtenerRespuesta(rgEquipoProteccion));
                jsonParam.put("corteVisible", obtenerRespuesta(rgCorteVisible));
                jsonParam.put("deteccionPotencial", obtenerRespuesta(rgDeteccionPotencial));
                jsonParam.put("ceroMetales", obtenerRespuesta(rgCeroMetales));
                jsonParam.put("actividadesSalvanVidas", obtenerRespuesta(rgActividadesSalvanVidas));
                jsonParam.put("llenadoRim", obtenerRespuesta(rgLlenadoRim));
                jsonParam.put("observaciones", observaciones);
                jsonParam.put("estado", estado);
                jsonParam.put("fecha", fecha);
                jsonParam.put("supervisor", supervisor);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                mainHandler.post(() -> {
                    btnGuardarAvance.setEnabled(true);
                    btnCompletarChecklist.setEnabled(true);

                    if (code == HttpURLConnection.HTTP_OK) {
                        txtEstadoChecklist.setText("Estado: " + estado);
                        txtFechaChecklist.setText("Fecha: " + fecha);
                        txtSupervisorChecklist.setText("Supervisor: " + supervisor);

                        if (completar) {
                            checklistCompletado = true;
                            bloquearEdicion();

                            // "responsable" contiene el username del Supervisor asignado a esta supervisión.
                            NotificacionesHelper.crear(this, responsable, "supervisor", "Reporte técnico disponible",
                                    "El Administrador completó el checklist del folio " + folio + ". Ya puedes registrar el reporte.", "CHECKLIST_COMPLETADO", folio, exito -> {});

                            new AlertDialog.Builder(this)
                                    .setTitle("Checklist completado")
                                    .setMessage("El checklist del folio " + folio + " fue enviado exitosamente.\n\nEl reporte técnico ahora está disponible.")
                                    .setCancelable(false)
                                    .setPositiveButton("Aceptar", (dialog, which) -> finish())
                                    .show();
                        } else {
                            Toast.makeText(this, "Avance guardado en el servidor", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error del servidor al procesar el checklist", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnGuardarAvance.setEnabled(true);
                    btnCompletarChecklist.setEnabled(true);
                    Toast.makeText(this, "Error de red al conectar al servidor", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void cargarChecklistGuardado() {
        executorService.execute(() -> {
            try {
                // CORREGIDO: Protegemos el folio con URLEncoder por si contiene caracteres especiales o espacios accidentales
                String folioEscapado = URLEncoder.encode(folio, "UTF-8");
                URL url = new URL(Config.BASE_URL + "/api/checklists/buscar/" + folioEscapado);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    if (!sb.toString().isEmpty()) {
                        JSONObject json = new JSONObject(sb.toString());

                        mainHandler.post(() -> {
                            try {
                                seleccionarRespuesta(rgAreaDelimitada, json.optString("areaDelimitada", ""));
                                seleccionarRespuesta(rgEquipoProteccion, json.optString("equipoProteccion", ""));
                                seleccionarRespuesta(rgCorteVisible, json.optString("corteVisible", ""));
                                seleccionarRespuesta(rgDeteccionPotencial, json.optString("deteccionPotencial", ""));
                                seleccionarRespuesta(rgCeroMetales, json.optString("ceroMetales", ""));
                                seleccionarRespuesta(rgActividadesSalvanVidas, json.optString("actividadesSalvanVidas", ""));
                                seleccionarRespuesta(rgLlenadoRim, json.optString("llenadoRim", ""));

                                etObservacionesChecklist.setText(json.optString("observaciones", ""));
                                txtFechaChecklist.setText("Fecha: " + json.optString("fecha", "---"));
                                txtSupervisorChecklist.setText("Supervisor: " + json.optString("supervisor", "Administrador"));

                                String estadoGuardado = json.optString("estado", "Pendiente");
                                txtEstadoChecklist.setText("Estado: " + estadoGuardado);

                                checklistCompletado = "Completado".equalsIgnoreCase(estadoGuardado);
                                if (checklistCompletado) {
                                    bloquearEdicion();
                                }
                                actualizarProgreso();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void seleccionarRespuesta(RadioGroup grupo, String respuesta) {
        if (respuesta == null || respuesta.trim().isEmpty()) return;
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View vista = grupo.getChildAt(i);
            if (vista instanceof RadioButton) {
                RadioButton rb = (RadioButton) vista;
                if (respuesta.equalsIgnoreCase(rb.getText().toString().trim())) {
                    rb.setChecked(true);
                    return;
                }
            }
        }
    }

    private String obtenerRespuesta(RadioGroup grupo) {
        int checkedId = grupo.getCheckedRadioButtonId();
        if (checkedId == -1) return "";
        RadioButton rb = findViewById(checkedId);
        return rb == null ? "" : rb.getText().toString().trim();
    }

    private int contarRespuestas() {
        int respondidas = 0;
        if (rgAreaDelimitada.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgEquipoProteccion.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgCorteVisible.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgDeteccionPotencial.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgCeroMetales.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgActividadesSalvanVidas.getCheckedRadioButtonId() != -1) respondidas++;
        if (rgLlenadoRim.getCheckedRadioButtonId() != -1) respondidas++;
        return respondidas;
    }

    private void actualizarProgreso() {
        int respondidas = contarRespuestas();
        int porcentaje = (respondidas * 100) / 7;
        txtProgresoChecklist.setText("Progreso: " + respondidas + " de 7 (" + porcentaje + "%)");
    }

    private void bloquearEdicion() {
        establecerGrupoHabilitado(rgAreaDelimitada, false);
        establecerGrupoHabilitado(rgEquipoProteccion, false);
        establecerGrupoHabilitado(rgCorteVisible, false);
        establecerGrupoHabilitado(rgDeteccionPotencial, false);
        establecerGrupoHabilitado(rgCeroMetales, false);
        establecerGrupoHabilitado(rgActividadesSalvanVidas, false);
        establecerGrupoHabilitado(rgLlenadoRim, false);

        etObservacionesChecklist.setEnabled(false);
        btnGuardarAvance.setVisibility(View.GONE);
        btnCompletarChecklist.setText("Checklist completado");
        btnCompletarChecklist.setEnabled(false);
        btnCompletarChecklist.setAlpha(0.65f);
    }

    private void establecerGrupoHabilitado(RadioGroup grupo, boolean habilitado) {
        grupo.setEnabled(habilitado);
        for (int i = 0; i < grupo.getChildCount(); i++) {
            grupo.getChildAt(i).setEnabled(habilitado);
        }
    }

    private String obtenerSupervisorActual() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        return preferences.getString(KEY_NOMBRE, "Administrador").trim();
    }
}