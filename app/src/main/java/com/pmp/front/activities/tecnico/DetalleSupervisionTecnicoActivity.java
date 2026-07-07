package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

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

            if (cargarSupervision()) {
                cargarUltimaRevision();
            }
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
                !"tecnico".equalsIgnoreCase(rol)) {

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

            mostrarErrorSupervision();
            return false;
        }

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
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

            if (!usuarioActual.equalsIgnoreCase(
                    partes[7].trim()
            )) {
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Acceso no permitido"
                        )
                        .setMessage(
                                "Esta supervisión no está " +
                                        "asignada a tu usuario."
                        )
                        .setCancelable(false)
                        .setPositiveButton(
                                "Cerrar",
                                (dialog, which) ->
                                        finish()
                        )
                        .show();

                return false;
            }

            supervisionActual = partes;

            mostrarDatosSupervision();

            return true;
        }

        mostrarErrorSupervision();

        return false;
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

        if ("Finalizada".equalsIgnoreCase(
                estadoGeneral
        )) {
            btnIniciarSupervision.setText(
                    "Supervisión finalizada"
            );

            btnIniciarSupervision.setEnabled(
                    false
            );

            btnIniciarSupervision.setAlpha(
                    0.55f
            );

            return;
        }

        if (
                "Pendiente de revisión".equalsIgnoreCase(
                        estadoGeneral
                ) ||
                        "Enviado".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            btnIniciarSupervision.setText(
                    "Enviada a revisión"
            );

            btnIniciarSupervision.setEnabled(
                    false
            );

            btnIniciarSupervision.setAlpha(
                    0.55f
            );

            return;
        }

        if (
                "Con observaciones".equalsIgnoreCase(
                        estadoGeneral
                ) ||
                        "Con observaciones".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            btnIniciarSupervision.setText(
                    "Corregir supervisión"
            );

            return;
        }

        if (
                "En proceso".equalsIgnoreCase(
                        estadoGeneral
                ) ||
                        "En proceso".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            btnIniciarSupervision.setText(
                    "Continuar supervisión"
            );

            return;
        }

        btnIniciarSupervision.setText(
                "Iniciar supervisión"
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

        if (!"Completado".equalsIgnoreCase(
                estadoChecklist
        )) {
            mostrarBloqueo();
            return;
        }

        if (
                "Asignada".equalsIgnoreCase(
                        estadoGeneral
                ) &&
                        (
                                "Disponible".equalsIgnoreCase(
                                        estadoReporte
                                ) ||
                                        "Bloqueado".equalsIgnoreCase(
                                                estadoReporte
                                        )
                        )
        ) {
            confirmarInicioSupervision();
            return;
        }

        if (
                "En proceso".equalsIgnoreCase(
                        estadoGeneral
                ) ||
                        "En proceso".equalsIgnoreCase(
                                estadoReporte
                        ) ||
                        "Con observaciones".equalsIgnoreCase(
                                estadoGeneral
                        ) ||
                        "Con observaciones".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            abrirReporteTecnico();
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

        boolean encontrada = false;

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (
                    partes.length >= 15 &&
                            folio.equalsIgnoreCase(
                                    partes[0].trim()
                            )
            ) {
                partes[11] =
                        "En proceso";

                partes[13] =
                        "En proceso";

                agregarRegistro(
                        actualizados,
                        unirPartes(partes)
                );

                encontrada = true;
            } else {
                agregarRegistro(
                        actualizados,
                        registro
                );
            }
        }

        if (!encontrada) {
            Toast.makeText(
                    this,
                    "No se pudo actualizar la supervisión",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        preferences.edit()
                .putString(
                        KEY_SUPERVISIONES,
                        actualizados.toString()
                )
                .apply();

        cargarSupervision();

        new AlertDialog.Builder(this)
                .setTitle(
                        "Supervisión iniciada"
                )
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
                            "El Supervisor todavía no ha " +
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

            mostrarChecklistNoEncontrado();
            return;
        }

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
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

            String mensaje =
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

                            "Fecha: " +
                            partes[10] +
                            "\n" +

                            "Supervisor: " +
                            partes[11] +
                            "\n" +

                            "Estado: " +
                            partes[12];

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Checklist de seguridad"
                    )
                    .setMessage(mensaje)
                    .setPositiveButton(
                            "Cerrar",
                            null
                    )
                    .show();

            return;
        }

        mostrarChecklistNoEncontrado();
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
                        "El Supervisor debe completar el " +
                                "checklist antes de iniciar " +
                                "la supervisión."
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