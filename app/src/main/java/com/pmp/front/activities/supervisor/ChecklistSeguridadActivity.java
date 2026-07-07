package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChecklistSeguridadActivity extends Activity {

    private static final String PREFS_CHECKLISTS =
            "checklists_local";

    private static final String KEY_CHECKLISTS =
            "checklists";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_NOMBRE =
            "nombre_actual";

    private TextView btnVolver;
    private TextView btnGuardarAvance;
    private TextView btnCompletarChecklist;

    private TextView txtFolioChecklist;
    private TextView txtCircuitoChecklist;
    private TextView txtResponsableChecklist;
    private TextView txtProgresoChecklist;
    private TextView txtEstadoChecklist;
    private TextView txtFechaChecklist;
    private TextView txtSupervisorChecklist;

    private EditText etObservacionesChecklist;

    private RadioGroup rgAreaDelimitada;
    private RadioGroup rgEquipoProteccion;
    private RadioGroup rgCorteVisible;
    private RadioGroup rgDeteccionPotencial;
    private RadioGroup rgCeroMetales;
    private RadioGroup rgActividadesSalvanVidas;
    private RadioGroup rgLlenadoRim;

    private String folio;
    private String circuito;
    private String responsable;

    private boolean checklistCompletado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_checklist_seguridad
        );

        inicializarVistas();

        if (!cargarDatosSupervision()) {
            return;
        }

        mostrarDatosSupervision();
        configurarEventos();
        cargarChecklistGuardado();
        actualizarProgreso();
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnGuardarAvance =
                findViewById(R.id.btnGuardarAvance);

        btnCompletarChecklist =
                findViewById(R.id.btnCompletarChecklist);

        txtFolioChecklist =
                findViewById(R.id.txtFolioChecklist);

        txtCircuitoChecklist =
                findViewById(R.id.txtCircuitoChecklist);

        txtResponsableChecklist =
                findViewById(R.id.txtResponsableChecklist);

        txtProgresoChecklist =
                findViewById(R.id.txtProgresoChecklist);

        txtEstadoChecklist =
                findViewById(R.id.txtEstadoChecklist);

        txtFechaChecklist =
                findViewById(R.id.txtFechaChecklist);

        txtSupervisorChecklist =
                findViewById(R.id.txtSupervisorChecklist);

        etObservacionesChecklist =
                findViewById(R.id.etObservacionesChecklist);

        rgAreaDelimitada =
                findViewById(R.id.rgAreaDelimitada);

        rgEquipoProteccion =
                findViewById(R.id.rgEquipoProteccion);

        rgCorteVisible =
                findViewById(R.id.rgCorteVisible);

        rgDeteccionPotencial =
                findViewById(R.id.rgDeteccionPotencial);

        rgCeroMetales =
                findViewById(R.id.rgCeroMetales);

        rgActividadesSalvanVidas =
                findViewById(R.id.rgActividadesSalvanVidas);

        rgLlenadoRim =
                findViewById(R.id.rgLlenadoRim);
    }

    private boolean cargarDatosSupervision() {
        folio =
                getIntent().getStringExtra(
                        "folio"
                );

        circuito =
                getIntent().getStringExtra(
                        "circuito"
                );

        responsable =
                getIntent().getStringExtra(
                        "responsable"
                );

        if (folio == null ||
                folio.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No se encontró el folio de la supervisión",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return false;
        }

        folio =
                folio.trim();

        if (circuito == null) {
            circuito = "";
        }

        if (responsable == null) {
            responsable = "";
        }

        return true;
    }

    private void mostrarDatosSupervision() {
        txtFolioChecklist.setText(
                "Folio: " +
                        folio
        );

        txtCircuitoChecklist.setText(
                "Circuito: " +
                        circuito
        );

        txtResponsableChecklist.setText(
                "Técnico responsable: " +
                        responsable
        );

        txtEstadoChecklist.setText(
                "Estado: Pendiente"
        );

        txtFechaChecklist.setText(
                "Fecha: Sin guardar"
        );

        txtSupervisorChecklist.setText(
                "Supervisor: " +
                        obtenerSupervisorActual()
        );
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        RadioGroup.OnCheckedChangeListener listener =
                (group, checkedId) ->
                        actualizarProgreso();

        rgAreaDelimitada.setOnCheckedChangeListener(
                listener
        );

        rgEquipoProteccion.setOnCheckedChangeListener(
                listener
        );

        rgCorteVisible.setOnCheckedChangeListener(
                listener
        );

        rgDeteccionPotencial.setOnCheckedChangeListener(
                listener
        );

        rgCeroMetales.setOnCheckedChangeListener(
                listener
        );

        rgActividadesSalvanVidas.setOnCheckedChangeListener(
                listener
        );

        rgLlenadoRim.setOnCheckedChangeListener(
                listener
        );

        btnGuardarAvance.setOnClickListener(
                v -> guardarChecklist(false)
        );

        btnCompletarChecklist.setOnClickListener(
                v -> confirmarCompletarChecklist()
        );
    }

    private void confirmarCompletarChecklist() {
        int respondidas =
                contarRespuestas();

        if (respondidas < 7) {
            new AlertDialog.Builder(this)
                    .setTitle(
                            "Checklist incompleto"
                    )
                    .setMessage(
                            "Debes responder los siete puntos " +
                                    "antes de completar el checklist.\n\n" +
                                    "Progreso actual: " +
                                    respondidas +
                                    " de 7."
                    )
                    .setPositiveButton(
                            "Entendido",
                            null
                    )
                    .show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Completar checklist"
                )
                .setMessage(
                        "¿Confirmas que la información es correcta?\n\n" +
                                "Al completarlo se habilitará el reporte " +
                                "para el Técnico responsable."
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Completar",
                        (dialog, which) ->
                                guardarChecklist(true)
                )
                .show();
    }

    private void guardarChecklist(
            boolean completar
    ) {
        if (checklistCompletado) {
            Toast.makeText(
                    this,
                    "El checklist ya está completado",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int respondidas =
                contarRespuestas();

        if (completar &&
                respondidas < 7) {

            return;
        }

        String respuestaArea =
                obtenerRespuesta(
                        rgAreaDelimitada
                );

        String respuestaEquipo =
                obtenerRespuesta(
                        rgEquipoProteccion
                );

        String respuestaCorte =
                obtenerRespuesta(
                        rgCorteVisible
                );

        String respuestaPotencial =
                obtenerRespuesta(
                        rgDeteccionPotencial
                );

        String respuestaMetales =
                obtenerRespuesta(
                        rgCeroMetales
                );

        String respuestaActividades =
                obtenerRespuesta(
                        rgActividadesSalvanVidas
                );

        String respuestaRim =
                obtenerRespuesta(
                        rgLlenadoRim
                );

        String observaciones =
                limpiar(
                        etObservacionesChecklist
                                .getText()
                                .toString()
                );

        String estado =
                completar
                        ? "Completado"
                        : "Pendiente";

        String progreso =
                respondidas +
                        "/7";

        String fecha =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        String supervisor =
                limpiar(
                        obtenerSupervisorActual()
                );

        /*
         * Formato del checklist:
         *
         * 0  folio
         * 1  área delimitada
         * 2  equipo de protección personal
         * 3  corte visible
         * 4  detección de corte de potencial
         * 5  cero metales
         * 6  actividades que salvan vidas
         * 7  llenado correcto de RIM
         * 8  observaciones
         * 9  progreso
         * 10 fecha
         * 11 supervisor
         * 12 estado
         */

        String registroChecklist =
                folio + "|" +
                        respuestaArea + "|" +
                        respuestaEquipo + "|" +
                        respuestaCorte + "|" +
                        respuestaPotencial + "|" +
                        respuestaMetales + "|" +
                        respuestaActividades + "|" +
                        respuestaRim + "|" +
                        observaciones + "|" +
                        progreso + "|" +
                        fecha + "|" +
                        supervisor + "|" +
                        estado;

        guardarOActualizarChecklist(
                registroChecklist
        );

        actualizarEstadosSupervision(
                estado,
                completar
                        ? "Disponible"
                        : "Bloqueado"
        );

        /*
         * Cuando el Supervisor completa el
         * checklist, se avisa al Técnico
         * responsable que el reporte ya está
         * disponible.
         */
        if (completar) {
            crearNotificacionTecnico();
        }

        txtEstadoChecklist.setText(
                "Estado: " +
                        estado
        );

        txtFechaChecklist.setText(
                "Fecha: " +
                        fecha
        );

        txtSupervisorChecklist.setText(
                "Supervisor: " +
                        supervisor
        );

        if (completar) {
            checklistCompletado = true;

            bloquearEdicion();

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Checklist completado"
                    )
                    .setMessage(
                            "El checklist de la supervisión " +
                                    folio +
                                    " fue completado correctamente.\n\n" +
                                    "El reporte técnico ahora está disponible."
                    )
                    .setCancelable(false)
                    .setPositiveButton(
                            "Aceptar",
                            (dialog, which) ->
                                    finish()
                    )
                    .show();

        } else {
            Toast.makeText(
                    this,
                    "Avance del checklist guardado",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void crearNotificacionTecnico() {
        String usuarioTecnico =
                obtenerUsuarioTecnicoResponsable();

        if (usuarioTecnico.isEmpty()) {
            return;
        }

        NotificacionesHelper.crear(
                this,
                usuarioTecnico,
                "tecnico",
                "Reporte técnico disponible",
                "El Supervisor completó el checklist de la supervisión " +
                        folio +
                        ". Ya puedes registrar el reporte de supervisión de cortes.",
                "CHECKLIST_COMPLETADO",
                folio
        );
    }

    private String obtenerUsuarioTecnicoResponsable() {
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

            return "";
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

            if (folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                return partes[7] == null
                        ? ""
                        : partes[7].trim();
            }
        }

        return "";
    }

    private void guardarOActualizarChecklist(
            String nuevoRegistro
    ) {
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

        StringBuilder actualizados =
                new StringBuilder();

        boolean encontrado =
                false;

        if (datos != null &&
                !datos.trim().isEmpty()) {

            String[] registros =
                    datos.split("\n");

            for (String registro : registros) {
                if (registro.trim().isEmpty()) {
                    continue;
                }

                String[] partes =
                        registro.split("\\|", -1);

                if (partes.length > 0 &&
                        folio.equalsIgnoreCase(
                                partes[0].trim()
                        )) {

                    agregarRegistro(
                            actualizados,
                            nuevoRegistro
                    );

                    encontrado =
                            true;

                } else {
                    agregarRegistro(
                            actualizados,
                            registro
                    );
                }
            }
        }

        if (!encontrado) {
            agregarRegistro(
                    actualizados,
                    nuevoRegistro
            );
        }

        preferences.edit()
                .putString(
                        KEY_CHECKLISTS,
                        actualizados.toString()
                )
                .apply();
    }

    private void actualizarEstadosSupervision(
            String estadoChecklist,
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

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length >= 14 &&
                    folio.equalsIgnoreCase(
                            partes[0].trim()
                    )) {

                partes[12] =
                        estadoChecklist;

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
    }

    private void cargarChecklistGuardado() {
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

            seleccionarRespuesta(
                    rgAreaDelimitada,
                    partes[1]
            );

            seleccionarRespuesta(
                    rgEquipoProteccion,
                    partes[2]
            );

            seleccionarRespuesta(
                    rgCorteVisible,
                    partes[3]
            );

            seleccionarRespuesta(
                    rgDeteccionPotencial,
                    partes[4]
            );

            seleccionarRespuesta(
                    rgCeroMetales,
                    partes[5]
            );

            seleccionarRespuesta(
                    rgActividadesSalvanVidas,
                    partes[6]
            );

            seleccionarRespuesta(
                    rgLlenadoRim,
                    partes[7]
            );

            etObservacionesChecklist.setText(
                    partes[8]
            );

            txtFechaChecklist.setText(
                    "Fecha: " +
                            partes[10]
            );

            txtSupervisorChecklist.setText(
                    "Supervisor: " +
                            partes[11]
            );

            txtEstadoChecklist.setText(
                    "Estado: " +
                            partes[12]
            );

            checklistCompletado =
                    "Completado".equalsIgnoreCase(
                            partes[12]
                    );

            if (checklistCompletado) {
                bloquearEdicion();
            }

            actualizarProgreso();
            return;
        }
    }

    private void seleccionarRespuesta(
            RadioGroup grupo,
            String respuesta
    ) {
        if (respuesta == null ||
                respuesta.trim().isEmpty()) {

            return;
        }

        for (int i = 0;
             i < grupo.getChildCount();
             i++) {

            View vista =
                    grupo.getChildAt(i);

            if (!(vista instanceof RadioButton)) {
                continue;
            }

            RadioButton radioButton =
                    (RadioButton) vista;

            if (respuesta.equalsIgnoreCase(
                    radioButton.getText()
                            .toString()
                            .trim()
            )) {
                radioButton.setChecked(
                        true
                );

                return;
            }
        }
    }

    private String obtenerRespuesta(
            RadioGroup grupo
    ) {
        int checkedId =
                grupo.getCheckedRadioButtonId();

        if (checkedId == -1) {
            return "";
        }

        RadioButton radioButton =
                findViewById(checkedId);

        if (radioButton == null) {
            return "";
        }

        return radioButton.getText()
                .toString()
                .trim();
    }

    private int contarRespuestas() {
        int respondidas =
                0;

        if (rgAreaDelimitada
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgEquipoProteccion
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgCorteVisible
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgDeteccionPotencial
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgCeroMetales
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgActividadesSalvanVidas
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        if (rgLlenadoRim
                .getCheckedRadioButtonId() != -1) {

            respondidas++;
        }

        return respondidas;
    }

    private void actualizarProgreso() {
        int respondidas =
                contarRespuestas();

        int porcentaje =
                (respondidas * 100) / 7;

        txtProgresoChecklist.setText(
                "Progreso: " +
                        respondidas +
                        " de 7 (" +
                        porcentaje +
                        "%)"
        );
    }

    private void bloquearEdicion() {
        establecerGrupoHabilitado(
                rgAreaDelimitada,
                false
        );

        establecerGrupoHabilitado(
                rgEquipoProteccion,
                false
        );

        establecerGrupoHabilitado(
                rgCorteVisible,
                false
        );

        establecerGrupoHabilitado(
                rgDeteccionPotencial,
                false
        );

        establecerGrupoHabilitado(
                rgCeroMetales,
                false
        );

        establecerGrupoHabilitado(
                rgActividadesSalvanVidas,
                false
        );

        establecerGrupoHabilitado(
                rgLlenadoRim,
                false
        );

        etObservacionesChecklist.setEnabled(
                false
        );

        btnGuardarAvance.setVisibility(
                View.GONE
        );

        btnCompletarChecklist.setText(
                "Checklist completado"
        );

        btnCompletarChecklist.setEnabled(
                false
        );

        btnCompletarChecklist.setAlpha(
                0.65f
        );
    }

    private void establecerGrupoHabilitado(
            RadioGroup grupo,
            boolean habilitado
    ) {
        grupo.setEnabled(
                habilitado
        );

        for (int i = 0;
             i < grupo.getChildCount();
             i++) {

            grupo.getChildAt(i)
                    .setEnabled(
                            habilitado
                    );
        }
    }

    private String obtenerSupervisorActual() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        String nombre =
                preferences.getString(
                        KEY_NOMBRE,
                        "Administrador"
                );

        if (nombre == null ||
                nombre.trim().isEmpty()) {

            return "Administrador";
        }

        return nombre.trim();
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
}