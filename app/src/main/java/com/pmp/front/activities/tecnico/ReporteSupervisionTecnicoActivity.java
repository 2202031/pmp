package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReporteSupervisionTecnicoActivity
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
    private TextView btnGuardarAvance;
    private TextView btnAbrirEvidencias;
    private TextView btnEnviarRevision;

    private TextView txtFolioReporte;
    private TextView txtCircuitoReporte;
    private TextView txtEstadoFormulario;
    private TextView txtUltimoGuardado;
    private TextView txtCantidadEvidencias;

    private LinearLayout panelObservacionesSupervisor;

    private TextView txtObservacionesSupervisor;

    private EditText etNumeroNotificacion;
    private EditText etKwh;
    private EditText etImporte;
    private EditText etRpu;
    private EditText etOrdenCorte;
    private EditText etTarifa;

    private RadioGroup rgStatusServicio;
    private RadioGroup rgDatosCorte;

    private String usuarioActual;
    private String folio;

    private String[] supervisionActual;

    private boolean modoSoloLectura =
            false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_reporte_supervision_tecnico
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

        if (usuarioActual == null ||
                usuarioActual.trim().isEmpty() ||
                folio == null) {

            return;
        }

        if (cargarSupervision()) {
            cargarReporteGuardado();
            cargarUltimaRevision();
            actualizarCantidadEvidencias();
        }
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnGuardarAvance =
                findViewById(
                        R.id.btnGuardarAvance
                );

        btnAbrirEvidencias =
                findViewById(
                        R.id.btnAbrirEvidencias
                );

        btnEnviarRevision =
                findViewById(
                        R.id.btnEnviarRevision
                );

        txtFolioReporte =
                findViewById(
                        R.id.txtFolioReporte
                );

        txtCircuitoReporte =
                findViewById(
                        R.id.txtCircuitoReporte
                );

        txtEstadoFormulario =
                findViewById(
                        R.id.txtEstadoFormulario
                );

        txtUltimoGuardado =
                findViewById(
                        R.id.txtUltimoGuardado
                );

        txtCantidadEvidencias =
                findViewById(
                        R.id.txtCantidadEvidencias
                );

        panelObservacionesSupervisor =
                findViewById(
                        R.id.panelObservacionesSupervisor
                );

        txtObservacionesSupervisor =
                findViewById(
                        R.id.txtObservacionesSupervisor
                );

        etNumeroNotificacion =
                findViewById(
                        R.id.etNumeroNotificacion
                );

        etKwh =
                findViewById(R.id.etKwh);

        etImporte =
                findViewById(R.id.etImporte);

        etRpu =
                findViewById(R.id.etRpu);

        etOrdenCorte =
                findViewById(
                        R.id.etOrdenCorte
                );

        etTarifa =
                findViewById(R.id.etTarifa);

        rgStatusServicio =
                findViewById(
                        R.id.rgStatusServicio
                );

        rgDatosCorte =
                findViewById(
                        R.id.rgDatosCorte
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

        usuarioActual =
                usuarioActual.trim();

        return true;
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnGuardarAvance.setOnClickListener(
                v -> guardarReporte(
                        obtenerEstadoParaGuardado(),
                        true
                )
        );

        btnAbrirEvidencias.setOnClickListener(
                v -> abrirEvidencias()
        );

        btnEnviarRevision.setOnClickListener(
                v -> validarYConfirmarEnvio()
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

            mostrarSupervisionNoEncontrada();
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

            supervisionActual =
                    partes;

            txtFolioReporte.setText(
                    supervisionActual[0]
            );

            txtCircuitoReporte.setText(
                    supervisionActual[2] +
                            " • " +
                            supervisionActual[1]
            );

            txtEstadoFormulario.setText(
                    "Reporte: " +
                            supervisionActual[13]
            );

            configurarModoFormulario();

            return true;
        }

        mostrarSupervisionNoEncontrada();

        return false;
    }

    private void configurarModoFormulario() {
        if (supervisionActual == null) {
            return;
        }

        String estadoGeneral =
                supervisionActual[11];

        String estadoReporte =
                supervisionActual[13];

        modoSoloLectura =
                "Pendiente de revisión".equalsIgnoreCase(
                        estadoGeneral
                ) ||
                        "Finalizada".equalsIgnoreCase(
                                estadoGeneral
                        ) ||
                        "Enviado".equalsIgnoreCase(
                                estadoReporte
                        ) ||
                        "Validado".equalsIgnoreCase(
                                estadoReporte
                        );

        etNumeroNotificacion.setEnabled(
                !modoSoloLectura
        );

        etKwh.setEnabled(
                !modoSoloLectura
        );

        etImporte.setEnabled(
                !modoSoloLectura
        );

        etRpu.setEnabled(
                !modoSoloLectura
        );

        etOrdenCorte.setEnabled(
                !modoSoloLectura
        );

        etTarifa.setEnabled(
                !modoSoloLectura
        );

        habilitarRadioGroup(
                rgStatusServicio,
                !modoSoloLectura
        );

        habilitarRadioGroup(
                rgDatosCorte,
                !modoSoloLectura
        );

        if (modoSoloLectura) {
            btnGuardarAvance.setVisibility(
                    View.GONE
            );

            btnAbrirEvidencias.setText(
                    "Ver evidencias fotográficas"
            );

            btnEnviarRevision.setEnabled(
                    false
            );

            btnEnviarRevision.setAlpha(
                    0.55f
            );

            if ("Finalizada".equalsIgnoreCase(
                    estadoGeneral
            )) {
                btnEnviarRevision.setText(
                        "Supervisión finalizada"
                );
            } else {
                btnEnviarRevision.setText(
                        "Información enviada a revisión"
                );
            }

        } else {
            btnGuardarAvance.setVisibility(
                    View.VISIBLE
            );

            btnAbrirEvidencias.setText(
                    "Capturar evidencias"
            );

            btnEnviarRevision.setEnabled(
                    true
            );

            btnEnviarRevision.setAlpha(
                    1f
            );

            if ("Con observaciones".equalsIgnoreCase(
                    estadoGeneral
            ) ||
                    "Con observaciones".equalsIgnoreCase(
                            estadoReporte
                    )) {

                btnEnviarRevision.setText(
                        "Reenviar a revisión"
                );

            } else {
                btnEnviarRevision.setText(
                        "Enviar a revisión"
                );
            }
        }
    }

    private void cargarReporteGuardado() {
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

            txtUltimoGuardado.setText(
                    "Sin avances guardados"
            );

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

            if (partes.length < 12) {
                continue;
            }

            if (!folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            etNumeroNotificacion.setText(
                    partes[1]
            );

            etKwh.setText(
                    partes[2]
            );

            etImporte.setText(
                    partes[3]
            );

            etRpu.setText(
                    partes[4]
            );

            etOrdenCorte.setText(
                    partes[5]
            );

            etTarifa.setText(
                    partes[6]
            );

            seleccionarRespuesta(
                    rgStatusServicio,
                    partes[7]
            );

            seleccionarRespuesta(
                    rgDatosCorte,
                    partes[8]
            );

            txtUltimoGuardado.setText(
                    "Último guardado: " +
                            partes[9]
            );

            return;
        }

        txtUltimoGuardado.setText(
                "Sin avances guardados"
        );
    }

    private void cargarUltimaRevision() {
        panelObservacionesSupervisor.setVisibility(
                View.GONE
        );

        txtObservacionesSupervisor.setText("");

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

        String[] ultimaRevision =
                null;

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
                ultimaRevision =
                        partes;
            }
        }

        if (ultimaRevision == null ||
                !"Devuelta".equalsIgnoreCase(
                        ultimaRevision[4]
                )) {

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

        txtObservacionesSupervisor.setText(
                contenido
        );

        panelObservacionesSupervisor.setVisibility(
                View.VISIBLE
        );
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
                    radioButton
                            .getText()
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

    private void abrirEvidencias() {
        if (!modoSoloLectura) {
            guardarReporte(
                    obtenerEstadoParaGuardado(),
                    false
            );
        }

        Intent intent =
                new Intent(
                        ReporteSupervisionTecnicoActivity.this,
                        EvidenciasSupervisionTecnicoActivity.class
                );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
    }

    private void validarYConfirmarEnvio() {
        List<String> faltantes =
                obtenerCamposFaltantes();

        if (!faltantes.isEmpty()) {
            StringBuilder mensaje =
                    new StringBuilder();

            mensaje.append(
                    "Completa la siguiente información:\n\n"
            );

            for (String faltante :
                    faltantes) {

                mensaje.append("• ")
                        .append(faltante)
                        .append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Información incompleta"
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

        String titulo =
                esCorreccion()
                        ? "Reenviar a revisión"
                        : "Enviar a revisión";

        String mensaje =
                esCorreccion()
                        ? "¿Confirmas que realizaste las correcciones solicitadas?\n\n" +
                        "Después del reenvío no podrás modificar la información hasta una nueva revisión."
                        : "¿Confirmas que el reporte y las cuatro evidencias están completos?\n\n" +
                        "Después del envío no podrás modificarlos hasta que el Supervisor los devuelva con observaciones.";

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Enviar",
                        (dialog, which) ->
                                enviarARevision()
                )
                .show();
    }

    private List<String> obtenerCamposFaltantes() {
        List<String> faltantes =
                new ArrayList<>();

        if (obtenerTexto(
                etNumeroNotificacion
        ).isEmpty()) {

            faltantes.add(
                    "Número y año de notificación"
            );
        }

        if (obtenerTexto(
                etKwh
        ).isEmpty()) {

            faltantes.add("KWh");
        }

        if (obtenerTexto(
                etImporte
        ).isEmpty()) {

            faltantes.add("Importe");
        }

        if (obtenerTexto(
                etRpu
        ).isEmpty()) {

            faltantes.add(
                    "RPU de notificación"
            );
        }

        if (obtenerTexto(
                etOrdenCorte
        ).isEmpty()) {

            faltantes.add(
                    "Número de orden de corte"
            );
        }

        if (obtenerTexto(
                etTarifa
        ).isEmpty()) {

            faltantes.add("Tarifa");
        }

        if (rgStatusServicio
                .getCheckedRadioButtonId() == -1) {

            faltantes.add(
                    "Estatus del servicio"
            );
        }

        if (rgDatosCorte
                .getCheckedRadioButtonId() == -1) {

            faltantes.add(
                    "Datos del corte"
            );
        }

        int evidencias =
                contarEvidencias();

        if (evidencias < 4) {
            faltantes.add(
                    "Las cuatro evidencias fotográficas (" +
                            evidencias +
                            " de 4)"
            );
        }

        return faltantes;
    }

    private boolean esCorreccion() {
        if (supervisionActual == null) {
            return false;
        }

        return "Con observaciones"
                .equalsIgnoreCase(
                        supervisionActual[11]
                ) ||
                "Con observaciones"
                        .equalsIgnoreCase(
                                supervisionActual[13]
                        );
    }

    private void enviarARevision() {
        boolean eraCorreccion =
                esCorreccion();

        guardarReporte(
                "Enviado",
                false
        );

        actualizarEstadosSupervision(
                "Pendiente de revisión",
                "Enviado"
        );

        String tituloNotificacion =
                eraCorreccion
                        ? "Supervisión reenviada"
                        : "Supervisión enviada a revisión";

        String mensajeNotificacion =
                eraCorreccion
                        ? "El Técnico realizó las correcciones y reenvió la supervisión " +
                        folio +
                        " para una nueva revisión."
                        : "El Técnico envió la supervisión " +
                        folio +
                        " para revisión.";

        String tipoNotificacion =
                eraCorreccion
                        ? "REENVIO_REVISION"
                        : "ENVIO_REVISION";

        /*
         * El asterisco indica que la
         * notificación es visible para
         * cualquier usuario Supervisor.
         */
        NotificacionesHelper.crear(
                this,
                "*",
                "supervisor",
                tituloNotificacion,
                mensajeNotificacion,
                tipoNotificacion,
                folio
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Enviado correctamente"
                )
                .setMessage(
                        "La supervisión " +
                                folio +
                                " fue enviada a revisión.\n\n" +
                                "El Supervisor podrá validar la información " +
                                "o devolverla nuevamente con observaciones."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Aceptar",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void guardarReporte(
            String estadoRegistro,
            boolean mostrarMensaje
    ) {
        String fechaGuardado =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        String nuevoRegistro =
                limpiar(folio) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etNumeroNotificacion
                                )
                        ) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etKwh
                                )
                        ) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etImporte
                                )
                        ) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etRpu
                                )
                        ) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etOrdenCorte
                                )
                        ) + "|" +

                        limpiar(
                                obtenerTexto(
                                        etTarifa
                                )
                        ) + "|" +

                        limpiar(
                                obtenerRespuesta(
                                        rgStatusServicio
                                )
                        ) + "|" +

                        limpiar(
                                obtenerRespuesta(
                                        rgDatosCorte
                                )
                        ) + "|" +

                        fechaGuardado + "|" +

                        limpiar(
                                usuarioActual
                        ) + "|" +

                        limpiar(
                                estadoRegistro
                        );

        guardarOActualizarReporte(
                nuevoRegistro
        );

        txtUltimoGuardado.setText(
                "Último guardado: " +
                        fechaGuardado
        );

        if (mostrarMensaje) {
            Toast.makeText(
                    this,
                    "Avance del reporte guardado",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void guardarOActualizarReporte(
            String nuevoRegistro
    ) {
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

        StringBuilder actualizados =
                new StringBuilder();

        boolean encontrado =
                false;

        if (datos != null &&
                !datos.trim().isEmpty()) {

            String[] registros =
                    datos.split("\n");

            for (String registro :
                    registros) {

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
                        KEY_REPORTES,
                        actualizados.toString()
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

    private void actualizarCantidadEvidencias() {
        int cantidad =
                contarEvidencias();

        txtCantidadEvidencias.setText(
                cantidad +
                        " de 4 evidencias registradas"
        );
    }

    private int contarEvidencias() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_EVIDENCIAS,
                        MODE_PRIVATE
                );

        int cantidad =
                0;

        for (String sufijo :
                SUFIJOS_EVIDENCIAS) {

            String ruta =
                    preferences.getString(
                            obtenerClaveEvidencia(
                                    sufijo
                            ),
                            ""
                    );

            if (ruta != null &&
                    !ruta.trim().isEmpty()) {

                cantidad++;
            }
        }

        return cantidad;
    }

    private String obtenerClaveEvidencia(
            String sufijo
    ) {
        return folio +
                "_" +
                sufijo;
    }

    private String obtenerEstadoParaGuardado() {
        if (esCorreccion()) {
            return "Con observaciones";
        }

        return "En proceso";
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
                findViewById(
                        checkedId
                );

        if (radioButton == null) {
            return "";
        }

        return radioButton
                .getText()
                .toString()
                .trim();
    }

    private String obtenerTexto(
            EditText editText
    ) {
        return editText
                .getText()
                .toString()
                .trim();
    }

    private void habilitarRadioGroup(
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