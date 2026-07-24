package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetalleSupervisionTecnicoActivity
        extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

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

    private static final String PREFS_REVISIONES =
            "revisiones_local";

    private static final String KEY_REVISIONES =
            "revisiones";

    private TextView btnVolver;
    private TextView btnVerChecklist;
    private TextView btnIniciarSupervision;

    private TextView txtFolio;
    private TextView txtCircuitoFecha;
    private TextView txtEstadoGeneral;
    private TextView txtLugar;
    private TextView txtPrioridad;
    private TextView txtDescripcion;
    private TextView txtObservaciones;
    private TextView txtResponsable;
    private TextView txtPersonalApoyo;
    private TextView txtEstadoChecklist;
    private TextView txtEstadoReporte;

    private LinearLayout panelObservacionesTecnico;

    private TextView txtObservacionesTecnico;

    private String usuarioActual;
    private String folio;

    private String[] supervisionActual;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_detalle_supervision_tecnico
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        folio =
                getIntent()
                        .getStringExtra("folio");

        if (folio == null ||
                folio.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No se encontró la supervisión",
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

        if (usuarioActual != null &&
                !usuarioActual.trim().isEmpty() &&
                folio != null) {

            cargarSupervisionAsync(this::cargarUltimaRevision);
        }
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnVerChecklist =
                findViewById(
                        R.id.btnVerChecklist
                );

        btnIniciarSupervision =
                findViewById(
                        R.id.btnIniciarSupervision
                );

        txtFolio =
                findViewById(R.id.txtFolio);

        txtCircuitoFecha =
                findViewById(
                        R.id.txtCircuitoFecha
                );

        txtEstadoGeneral =
                findViewById(
                        R.id.txtEstadoGeneral
                );

        txtLugar =
                findViewById(R.id.txtLugar);

        txtPrioridad =
                findViewById(R.id.txtPrioridad);

        txtDescripcion =
                findViewById(R.id.txtDescripcion);

        txtObservaciones =
                findViewById(
                        R.id.txtObservaciones
                );

        txtResponsable =
                findViewById(
                        R.id.txtResponsable
                );

        txtPersonalApoyo =
                findViewById(
                        R.id.txtPersonalApoyo
                );

        txtEstadoChecklist =
                findViewById(
                        R.id.txtEstadoChecklist
                );

        txtEstadoReporte =
                findViewById(
                        R.id.txtEstadoReporte
                );

        panelObservacionesTecnico =
                findViewById(
                        R.id.panelObservacionesTecnico
                );

        txtObservacionesTecnico =
                findViewById(
                        R.id.txtObservacionesTecnico
                );
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        usuarioActual =
                preferences.getString(
                        KEY_USUARIO,
                        ""
                );

        String rol =
                preferences.getString(
                        KEY_ROL,
                        ""
                );

        if (usuarioActual == null ||
                usuarioActual.trim().isEmpty() ||
                !"supervisor".equalsIgnoreCase(rol)) {

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
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnVerChecklist.setOnClickListener(
                v -> mostrarChecklist()
        );

        btnIniciarSupervision.setOnClickListener(
                v -> procesarAccionPrincipal()
        );
    }

    // Consulta el checklist del folio y devuelve "Completado" si el Administrador
    // ya lo terminó (progreso 100%), o "Pendiente" en cualquier otro caso.
    // Debe llamarse desde un hilo de fondo.
    private String consultarEstadoChecklist() {
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
                if (respuesta.isEmpty() || respuesta.equals("\"\"")) {
                    return "Pendiente";
                }

                JSONObject json = new JSONObject(respuesta);
                if (json.optBoolean("checklistCompletado", false)) {
                    return "Completado";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Pendiente";
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
                    String usernameSupervisor = json.optString("usernameSupervisor", "");

                    if (usuarioActual.equalsIgnoreCase(usernameSupervisor)) {
                        // Consultamos el checklist real para saber si el Administrador ya lo completó.
                        String estadoChecklist = consultarEstadoChecklist();

                        partesSinteticas = new String[15];
                        partesSinteticas[0] = json.optString("folio", "");
                        partesSinteticas[1] = json.optString("fecha", "");
                        partesSinteticas[2] = "";
                        partesSinteticas[3] = json.optString("lugar", "");
                        partesSinteticas[4] = json.optString("prioridad", "");
                        partesSinteticas[5] = json.optString("descripcion", "");
                        partesSinteticas[6] = json.optString("observaciones", "");
                        partesSinteticas[7] = usernameSupervisor;
                        partesSinteticas[8] = json.optString("tecnico", "");
                        partesSinteticas[9] = "";
                        partesSinteticas[10] = "";
                        partesSinteticas[11] = json.optString("estado", "");
                        partesSinteticas[12] = estadoChecklist; // "Completado" desbloquea el reporte al Supervisor
                        // El índice 13 refleja el estado del REPORTE, no del checklist.
                        // "Verificada" (checklist listo) NO significa terminado: el técnico apenas puede empezar.
                        // Solo "Finalizada" (validada por el admin al final) cierra el reporte.
                        String estadoAsignacion = json.optString("estado", "");
                        if ("Finalizada".equalsIgnoreCase(estadoAsignacion)) {
                            partesSinteticas[13] = "Validado";
                        } else if ("En proceso".equalsIgnoreCase(estadoAsignacion)) {
                            partesSinteticas[13] = "En proceso";
                        } else if ("Pendiente de revisión".equalsIgnoreCase(estadoAsignacion)) {
                            partesSinteticas[13] = "Enviado";
                        } else {
                            partesSinteticas[13] = "Pendiente";
                        }
                        partesSinteticas[14] = "";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String[] finalPartes = partesSinteticas;
            mainHandler.post(() -> {
                if (finalPartes == null) {
                    mostrarErrorSupervision();
                    return;
                }

                supervisionActual = finalPartes;
                mostrarDatosSupervision();

                if (siEncontrada != null) {
                    siEncontrada.run();
                }
            });
        });
    }

    private void mostrarDatosSupervision() {
        if (supervisionActual == null ||
                supervisionActual.length < 15) {

            return;
        }

        String apoyos =
                supervisionActual[10].trim();

        if (apoyos.isEmpty()) {
            apoyos =
                    "Sin personal de apoyo";
        }

        String observaciones =
                supervisionActual[6].trim();

        if (observaciones.isEmpty()) {
            observaciones =
                    "Sin observaciones iniciales";
        }

        txtFolio.setText(
                supervisionActual[0]
        );

        txtCircuitoFecha.setText(
                supervisionActual[2] +
                        " • " +
                        supervisionActual[1]
        );

        txtEstadoGeneral.setText(
                supervisionActual[11]
        );

        txtLugar.setText(
                supervisionActual[3]
        );

        txtPrioridad.setText(
                supervisionActual[4]
        );

        txtDescripcion.setText(
                supervisionActual[5]
        );

        txtObservaciones.setText(
                observaciones
        );

        txtResponsable.setText(
                supervisionActual[8]
        );

        txtPersonalApoyo.setText(
                apoyos
        );

        txtEstadoChecklist.setText(
                supervisionActual[12]
        );

        txtEstadoReporte.setText(
                supervisionActual[13]
        );

        configurarAccionPrincipal();
    }

    // Estados que indican que la supervisión ya se cerró y no debe reabrirse el reporte.
    private boolean esEstadoTerminado(String estado) {
        return "Finalizada".equalsIgnoreCase(estado) ||
                "Validada".equalsIgnoreCase(estado) ||
                "Validado".equalsIgnoreCase(estado);
    }

    private void configurarAccionPrincipal() {
        String estadoGeneral =
                supervisionActual[11];

        String estadoChecklist =
                supervisionActual[12];

        String estadoReporte =
                supervisionActual[13];

        boolean checklistCompletado =
                "Completado".equalsIgnoreCase(
                        estadoChecklist
                );

        btnIniciarSupervision.setEnabled(
                true
        );

        btnIniciarSupervision.setAlpha(
                1f
        );

        // Paso 0: si la supervisión ya terminó (validada/finalizada por el Administrador),
        // el reporte queda cerrado y el botón se bloquea. Evita que el Supervisor
        // vuelva a llenar un reporte ya cerrado.
        if (esEstadoTerminado(estadoGeneral) || esEstadoTerminado(estadoReporte)) {
            btnIniciarSupervision.setText(
                    "Supervisión finalizada"
            );
            btnIniciarSupervision.setEnabled(false);
            btnIniciarSupervision.setAlpha(0.55f);
            return;
        }

        // Paso 1: si el Administrador aún no completa el checklist, el reporte está bloqueado.
        if (!checklistCompletado) {
            btnIniciarSupervision.setText(
                    "Reporte bloqueado"
            );

            btnIniciarSupervision.setEnabled(
                    false
            );

            btnIniciarSupervision.setAlpha(
                    0.55f
            );

            return;
        }

        // Paso 2: el checklist ya está completo -> el Supervisor puede llenar el reporte.
        // El estado de la asignación viene como "Verificada" (checklist listo) o
        // "En proceso" (ya empezó a llenar el reporte).
        if ("En proceso".equalsIgnoreCase(estadoReporte) ||
                "En proceso".equalsIgnoreCase(estadoGeneral)) {
            btnIniciarSupervision.setText(
                    "Continuar reporte"
            );
            return;
        }

        if ("Enviado".equalsIgnoreCase(estadoReporte) ||
                "Pendiente de revisión".equalsIgnoreCase(estadoGeneral)) {
            btnIniciarSupervision.setText(
                    "Reporte enviado"
            );
            btnIniciarSupervision.setEnabled(false);
            btnIniciarSupervision.setAlpha(0.55f);
            return;
        }

        btnIniciarSupervision.setText(
                "Llenar reporte técnico"
        );
    }

    private void procesarAccionPrincipal() {
        if (supervisionActual == null) {
            return;
        }

        String estadoChecklist =
                supervisionActual[12];

        String estadoGeneral =
                supervisionActual[11];

        String estadoReporte =
                supervisionActual[13];

        // Si la supervisión ya está finalizada/validada, no se puede reabrir el reporte.
        if (esEstadoTerminado(estadoGeneral) || esEstadoTerminado(estadoReporte)) {
            Toast.makeText(this, "Esta supervisión ya fue finalizada.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"Completado".equalsIgnoreCase(
                estadoChecklist
        )) {
            mostrarBloqueo();
            return;
        }

        // El reporte ya fue enviado a revisión: no dejar re-abrir.
        if ("Enviado".equalsIgnoreCase(estadoReporte) ||
                "Pendiente de revisión".equalsIgnoreCase(estadoGeneral)) {
            Toast.makeText(this, "El reporte ya fue enviado a revisión.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Checklist completo: el Supervisor puede llenar/continuar el reporte técnico.
        // Si aún no lo ha empezado, marcamos la asignación como "En proceso" y abrimos.
        if ("En proceso".equalsIgnoreCase(estadoGeneral)) {
            abrirReporteTecnico();
        } else {
            confirmarInicioSupervision();
        }
    }

    private void confirmarInicioSupervision() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Iniciar supervisión"
                )
                .setMessage(
                        "¿Deseas iniciar la supervisión " +
                                folio +
                                "?\n\n" +
                                "El estado cambiará a En proceso."
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Iniciar",
                        (dialog, which) ->
                                iniciarSupervision()
                )
                .show();
    }

    private void iniciarSupervision() {
        executorService.execute(() -> {
            boolean ok = false;
            try {
                JSONObject json = new JSONObject();
                json.put("estado", "En proceso");

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

                ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean finalOk = ok;
            mainHandler.post(() -> {
                if (!finalOk) {
                    Toast.makeText(this, "No se pudo actualizar la supervisión", Toast.LENGTH_SHORT).show();
                    return;
                }

                cargarSupervisionAsync(null);

                new AlertDialog.Builder(this)
                        .setTitle("Supervisión iniciada")
                .setMessage(
                        "La supervisión ahora se encuentra " +
                                "En proceso.\n\n" +
                                "Ya puedes continuar con el " +
                                "reporte de supervisión de cortes."
                )
                .setPositiveButton(
                        "Continuar",
                        (dialog, which) ->
                                abrirReporteTecnico()
                )
                .show();
            });
        });
    }

    private void abrirReporteTecnico() {
        Intent intent = new Intent(
                DetalleSupervisionTecnicoActivity.this,
                ReporteSupervisionTecnicoActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
    }

    private void cargarUltimaRevision() {
        panelObservacionesTecnico.setVisibility(
                View.GONE
        );

        txtObservacionesTecnico.setText(
                ""
        );

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

            return;
        }

        String[] registros =
                datos.split("\n");

        String[] ultimaRevision = null;

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
                ultimaRevision = partes;
            }
        }

        if (
                ultimaRevision == null ||
                        !"Devuelta".equalsIgnoreCase(
                                ultimaRevision[4]
                        )
        ) {
            return;
        }

        String observaciones =
                ultimaRevision[1].trim();

        if (observaciones.isEmpty()) {
            observaciones =
                    "Sin observaciones registradas.";
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

        txtObservacionesTecnico.setText(
                contenido
        );

        panelObservacionesTecnico.setVisibility(
                View.VISIBLE
        );
    }

    private void mostrarChecklist() {
        if (supervisionActual == null) {
            return;
        }

        String estadoChecklist =
                supervisionActual[12];

        if (!"Completado".equalsIgnoreCase(
                estadoChecklist
        )) {
            new AlertDialog.Builder(this)
                    .setTitle(
                            "Checklist pendiente"
                    )
                    .setMessage(
                            "El Administrador todavía no ha " +
                                    "completado el checklist de " +
                                    "seguridad e higiene."
                    )
                    .setPositiveButton(
                            "Entendido",
                            null
                    )
                    .show();

            return;
        }

        // Consultamos el checklist real del servidor.
        executorService.execute(() -> {
            String mensaje = null;
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
                    if (!respuesta.isEmpty() && !respuesta.equals("\"\"")) {
                        JSONObject json = new JSONObject(respuesta);

                        String observaciones = json.optString("observaciones", "");
                        if (observaciones.trim().isEmpty()) observaciones = "Sin observaciones";

                        mensaje =
                                "Área delimitada: " + respuestaVisible(json.optString("areaDelimitada", "")) + "\n\n" +
                                "Equipo de protección personal: " + respuestaVisible(json.optString("equipoProteccion", "")) + "\n\n" +
                                "Corte visible: " + respuestaVisible(json.optString("corteVisible", "")) + "\n\n" +
                                "Detección de corte de potencial: " + respuestaVisible(json.optString("deteccionPotencial", "")) + "\n\n" +
                                "Cero metales: " + respuestaVisible(json.optString("ceroMetales", "")) + "\n\n" +
                                "Actividades que salvan vidas: " + respuestaVisible(json.optString("actividadesSalvanVidas", "")) + "\n\n" +
                                "Llenado correcto de RIM: " + respuestaVisible(json.optString("llenadoRim", "")) + "\n\n" +
                                "Observaciones:\n" + observaciones + "\n\n" +
                                "Estado: " + json.optString("estado", "Pendiente");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalMensaje = mensaje;
            mainHandler.post(() -> {
                if (finalMensaje == null) {
                    mostrarChecklistNoEncontrado();
                    return;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Checklist de seguridad")
                        .setMessage(finalMensaje)
                        .setPositiveButton("Cerrar", null)
                        .show();
            });
        });
    }

    private String respuestaVisible(
            String respuesta
    ) {
        if (
                respuesta == null ||
                        respuesta.trim().isEmpty()
        ) {
            return "Sin respuesta";
        }

        return respuesta.trim();
    }

    private void mostrarChecklistNoEncontrado() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Checklist no disponible"
                )
                .setMessage(
                        "No se encontraron los datos del " +
                                "checklist para esta supervisión."
                )
                .setPositiveButton(
                        "Cerrar",
                        null
                )
                .show();
    }

    private void mostrarBloqueo() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Reporte bloqueado"
                )
                .setMessage(
                        "El Administrador debe completar el " +
                                "checklist antes de que puedas " +
                                "llenar el reporte."
                )
                .setPositiveButton(
                        "Entendido",
                        null
                )
                .show();
    }

    private void mostrarErrorSupervision() {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión no encontrada"
                )
                .setMessage(
                        "No se pudo encontrar la información " +
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

        builder.append(registro);
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
}