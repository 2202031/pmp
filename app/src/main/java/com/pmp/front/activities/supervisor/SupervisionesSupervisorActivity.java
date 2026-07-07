package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.io.File;
import java.util.Locale;

public class SupervisionesSupervisorActivity
        extends Activity {

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

    private static final String PREFS_PDF =
            "reportes_pdf_local";

    private static final String PREFIJO_RUTA_PDF =
            "ruta_";

    private static final String PREFIJO_FECHA_PDF =
            "fecha_";

    private TextView btnVolver;
    private TextView btnNuevaAsignacion;

    private TextView txtTituloPantalla;
    private TextView txtSubtituloPantalla;
    private TextView txtDescripcionModulo;
    private TextView txtTituloListado;
    private TextView txtCantidadSupervisiones;

    private TextView txtEmptyTitulo;
    private TextView txtEmptyDescripcion;

    private EditText etBuscarSupervision;

    private LinearLayout containerSupervisiones;
    private LinearLayout emptyState;

    private boolean modoReportes = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_supervisiones_supervisor
        );

        String modo =
                getIntent().getStringExtra("modo");

        modoReportes =
                "reportes".equalsIgnoreCase(modo);

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarPantalla();
        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (etBuscarSupervision != null) {
            cargarSupervisiones(
                    etBuscarSupervision
                            .getText()
                            .toString()
                            .trim()
            );
        }
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnNuevaAsignacion =
                findViewById(
                        R.id.btnNuevaAsignacion
                );

        txtTituloPantalla =
                findViewById(
                        R.id.txtTituloPantalla
                );

        txtSubtituloPantalla =
                findViewById(
                        R.id.txtSubtituloPantalla
                );

        txtDescripcionModulo =
                findViewById(
                        R.id.txtDescripcionModulo
                );

        txtTituloListado =
                findViewById(
                        R.id.txtTituloListado
                );

        txtCantidadSupervisiones =
                findViewById(
                        R.id.txtCantidadSupervisiones
                );

        txtEmptyTitulo =
                findViewById(
                        R.id.txtEmptyTitulo
                );

        txtEmptyDescripcion =
                findViewById(
                        R.id.txtEmptyDescripcion
                );

        etBuscarSupervision =
                findViewById(
                        R.id.etBuscarSupervision
                );

        containerSupervisiones =
                findViewById(
                        R.id.containerSupervisiones
                );

        emptyState =
                findViewById(R.id.emptyState);
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        String rol =
                preferences.getString(
                        KEY_ROL,
                        ""
                );

        if (!"supervisor".equalsIgnoreCase(rol)) {
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

    private void configurarPantalla() {
        if (modoReportes) {
            txtTituloPantalla.setText(
                    "Reportes PDF"
            );

            txtSubtituloPantalla.setText(
                    "Documentos finales de supervisión"
            );

            txtDescripcionModulo.setText(
                    "Consulta las supervisiones finalizadas, " +
                            "genera sus reportes PDF y comparte " +
                            "los documentos disponibles."
            );

            txtTituloListado.setText(
                    "Reportes disponibles"
            );

            etBuscarSupervision.setHint(
                    "Buscar reporte por folio, circuito o Técnico"
            );

            btnNuevaAsignacion.setVisibility(
                    View.GONE
            );

            txtEmptyTitulo.setText(
                    "No hay reportes disponibles"
            );

            txtEmptyDescripcion.setText(
                    "Los reportes aparecerán cuando una " +
                            "supervisión haya sido validada y finalizada."
            );

        } else {
            txtTituloPantalla.setText(
                    "Supervisiones"
            );

            txtSubtituloPantalla.setText(
                    "Gestión y seguimiento"
            );

            txtDescripcionModulo.setText(
                    "Consulta las supervisiones, completa el checklist, " +
                            "revisa la información enviada por el Técnico " +
                            "y valida el cierre."
            );

            txtTituloListado.setText(
                    "Supervisiones registradas"
            );

            etBuscarSupervision.setHint(
                    "Buscar por folio, circuito, lugar o Técnico"
            );

            btnNuevaAsignacion.setVisibility(
                    View.VISIBLE
            );

            txtEmptyTitulo.setText(
                    "No hay supervisiones"
            );

            txtEmptyDescripcion.setText(
                    "Crea y asigna una supervisión para comenzar."
            );
        }
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnNuevaAsignacion.setOnClickListener(v -> {
            if (modoReportes) {
                return;
            }

            Intent intent = new Intent(
                    SupervisionesSupervisorActivity.this,
                    AsignarSupervisionActivity.class
            );

            startActivity(intent);
        });

        etBuscarSupervision.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        cargarSupervisiones(
                                s.toString().trim()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void cargarSupervisiones(
            String busqueda
    ) {
        containerSupervisiones.removeAllViews();

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

            mostrarEstadoVacio();
            return;
        }

        String[] registros =
                datos.split("\n");

        int visibles = 0;

        String textoBusqueda =
                busqueda.toLowerCase(
                        Locale.ROOT
                );

        for (int i = 0;
             i < registros.length;
             i++) {

            String registro =
                    registros[i];

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (modoReportes &&
                    !esReporteFinalizado(partes)) {

                continue;
            }

            if (!coincideBusqueda(
                    partes,
                    textoBusqueda
            )) {
                continue;
            }

            View tarjeta =
                    crearTarjetaSupervision(
                            partes,
                            i
                    );

            containerSupervisiones.addView(
                    tarjeta
            );

            visibles++;
        }

        if (modoReportes) {
            txtCantidadSupervisiones.setText(
                    visibles +
                            (
                                    visibles == 1
                                            ? " reporte"
                                            : " reportes"
                            )
            );
        } else {
            txtCantidadSupervisiones.setText(
                    visibles +
                            (
                                    visibles == 1
                                            ? " supervisión"
                                            : " supervisiones"
                            )
            );
        }

        if (visibles == 0) {
            emptyState.setVisibility(
                    View.VISIBLE
            );

            containerSupervisiones.setVisibility(
                    View.GONE
            );
        } else {
            emptyState.setVisibility(
                    View.GONE
            );

            containerSupervisiones.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private boolean esReporteFinalizado(
            String[] partes
    ) {
        return
                "Finalizada".equalsIgnoreCase(
                        partes[11]
                ) &&

                        "Completado".equalsIgnoreCase(
                                partes[12]
                        ) &&

                        "Validado".equalsIgnoreCase(
                                partes[13]
                        );
    }

    private boolean coincideBusqueda(
            String[] partes,
            String busqueda
    ) {
        if (busqueda.isEmpty()) {
            return true;
        }

        String contenido =
                partes[0] + " " +
                        partes[1] + " " +
                        partes[2] + " " +
                        partes[3] + " " +
                        partes[4] + " " +
                        partes[8] + " " +
                        partes[11] + " " +
                        partes[12] + " " +
                        partes[13] + " " +
                        obtenerEstadoPdf(partes[0]);

        return contenido
                .toLowerCase(Locale.ROOT)
                .contains(busqueda);
    }

    private View crearTarjetaSupervision(
            String[] partes,
            int indiceOriginal
    ) {
        String folio =
                partes[0];

        String fecha =
                partes[1];

        String circuito =
                partes[2];

        String lugar =
                partes[3];

        String prioridad =
                partes[4];

        String responsable =
                partes[8];

        String estado =
                partes[11];

        String checklist =
                partes[12];

        String estadoReporte =
                partes[13];

        String estadoPdf =
                obtenerEstadoPdf(folio);

        String fechaPdf =
                obtenerFechaPdf(folio);

        LinearLayout tarjeta =
                new LinearLayout(this);

        tarjeta.setOrientation(
                LinearLayout.VERTICAL
        );

        tarjeta.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        tarjeta.setBackgroundResource(
                R.drawable.bg_card_green
        );

        tarjeta.setElevation(dp(4));

        LinearLayout.LayoutParams tarjetaParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        tarjetaParams.setMargins(
                0,
                0,
                0,
                dp(14)
        );

        tarjeta.setLayoutParams(
                tarjetaParams
        );

        LinearLayout encabezado =
                new LinearLayout(this);

        encabezado.setOrientation(
                LinearLayout.HORIZONTAL
        );

        encabezado.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView icono =
                new TextView(this);

        icono.setText(
                modoReportes
                        ? "PDF"
                        : "▣"
        );

        icono.setTextSize(
                modoReportes
                        ? 12
                        : 21
        );

        icono.setTextColor(
                Color.parseColor("#006341")
        );

        icono.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        icono.setGravity(
                Gravity.CENTER
        );

        icono.setBackgroundResource(
                R.drawable.bg_input_login
        );

        icono.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(46),
                        dp(46)
                )
        );

        LinearLayout informacion =
                new LinearLayout(this);

        informacion.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams informacionParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        informacionParams.setMargins(
                dp(12),
                0,
                dp(8),
                0
        );

        informacion.setLayoutParams(
                informacionParams
        );

        TextView txtFolio =
                new TextView(this);

        txtFolio.setText(folio);
        txtFolio.setTextSize(16);

        txtFolio.setTextColor(
                Color.parseColor("#111827")
        );

        txtFolio.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        TextView txtCircuito =
                new TextView(this);

        txtCircuito.setText(
                circuito + " • " + fecha
        );

        txtCircuito.setTextSize(12);

        txtCircuito.setTextColor(
                Color.parseColor("#6B7280")
        );

        informacion.addView(txtFolio);
        informacion.addView(txtCircuito);

        TextView badgeEstado =
                new TextView(this);

        String textoBadge =
                modoReportes
                        ? estadoPdf
                        : estado;

        badgeEstado.setText(textoBadge);
        badgeEstado.setTextSize(11);

        badgeEstado.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        badgeEstado.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );

        GradientDrawable badgeFondo =
                new GradientDrawable();

        if (modoReportes &&
                !"Generado".equalsIgnoreCase(
                        estadoPdf
                )) {

            badgeEstado.setTextColor(
                    Color.parseColor("#92400E")
            );

            badgeFondo.setColor(
                    Color.parseColor("#FEF3C7")
            );
        } else {
            badgeEstado.setTextColor(
                    Color.parseColor("#006341")
            );

            badgeFondo.setColor(
                    Color.parseColor("#D1FAE5")
            );
        }

        badgeFondo.setCornerRadius(
                dp(18)
        );

        badgeEstado.setBackground(
                badgeFondo
        );

        encabezado.addView(icono);
        encabezado.addView(informacion);
        encabezado.addView(badgeEstado);

        TextView txtLugar =
                crearTextoDetalle(
                        "Lugar: " + lugar
                );

        LinearLayout.LayoutParams lugarParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        lugarParams.setMargins(
                0,
                dp(14),
                0,
                0
        );

        txtLugar.setLayoutParams(
                lugarParams
        );

        TextView txtResponsable =
                crearTextoDetalle(
                        "Técnico responsable: " +
                                responsable
                );

        TextView txtEstados =
                new TextView(this);

        if (modoReportes) {
            String detallePdf =
                    "Supervisión: " +
                            estado +
                            "   •   Reporte: " +
                            estadoReporte +
                            "\nPDF: " +
                            estadoPdf;

            if (!fechaPdf.isEmpty() &&
                    "Generado".equalsIgnoreCase(
                            estadoPdf
                    )) {

                detallePdf +=
                        "   •   " +
                                fechaPdf;
            }

            txtEstados.setText(
                    detallePdf
            );
        } else {
            txtEstados.setText(
                    "Prioridad: " +
                            prioridad +
                            "   •   Checklist: " +
                            checklist +
                            "\nReporte técnico: " +
                            estadoReporte
            );
        }

        txtEstados.setTextColor(
                Color.parseColor("#6B7280")
        );

        txtEstados.setTextSize(12);

        LinearLayout acciones =
                new LinearLayout(this);

        acciones.setOrientation(
                LinearLayout.HORIZONTAL
        );

        LinearLayout.LayoutParams accionesParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        accionesParams.setMargins(
                0,
                dp(16),
                0,
                0
        );

        acciones.setLayoutParams(
                accionesParams
        );

        if (modoReportes) {
            TextView btnReporte =
                    crearBoton(
                            "Generado".equalsIgnoreCase(
                                    estadoPdf
                            )
                                    ? "Abrir reporte"
                                    : "Generar reporte",
                            true
                    );

            TextView btnValidacion =
                    crearBoton(
                            "Ver validación",
                            false
                    );

            btnReporte.setOnClickListener(
                    v -> abrirReportePdf(
                            folio
                    )
            );

            btnValidacion.setOnClickListener(
                    v -> abrirRevision(
                            folio
                    )
            );

            acciones.addView(btnReporte);
            acciones.addView(btnValidacion);

            tarjeta.setOnClickListener(
                    v -> abrirReportePdf(
                            folio
                    )
            );

        } else {
            TextView btnVer =
                    crearBoton(
                            "Ver",
                            false
                    );

            String textoChecklist =
                    "Completado".equalsIgnoreCase(
                            checklist
                    )
                            ? "Ver checklist"
                            : "Checklist";

            TextView btnChecklist =
                    crearBoton(
                            textoChecklist,
                            true
                    );

            boolean tieneRevision =
                    tieneAccesoRevision(
                            estado,
                            estadoReporte
                    );

            String textoAccion;

            if ("Pendiente de revisión".equalsIgnoreCase(
                    estado
            ) ||
                    "Enviado".equalsIgnoreCase(
                            estadoReporte
                    )) {

                textoAccion =
                        "Revisar";

            } else if (
                    "Con observaciones".equalsIgnoreCase(
                            estado
                    ) ||
                            "Con observaciones".equalsIgnoreCase(
                                    estadoReporte
                            )
            ) {
                textoAccion =
                        "Ver revisión";

            } else if (
                    "Finalizada".equalsIgnoreCase(
                            estado
                    ) ||
                            "Validado".equalsIgnoreCase(
                                    estadoReporte
                            )
            ) {
                textoAccion =
                        "Ver validación";

            } else {
                textoAccion =
                        "Eliminar";
            }

            TextView btnAccion =
                    crearBoton(
                            textoAccion,
                            false
                    );

            btnVer.setOnClickListener(
                    v -> mostrarDetalle(
                            partes
                    )
            );

            btnChecklist.setOnClickListener(
                    v -> abrirChecklist(
                            folio,
                            circuito,
                            responsable
                    )
            );

            if (tieneRevision) {
                btnAccion.setOnClickListener(
                        v -> abrirRevision(
                                folio
                        )
                );
            } else {
                btnAccion.setOnClickListener(
                        v -> confirmarEliminacion(
                                folio,
                                indiceOriginal
                        )
                );
            }

            acciones.addView(btnVer);
            acciones.addView(btnChecklist);
            acciones.addView(btnAccion);

            tarjeta.setOnClickListener(
                    v -> mostrarDetalle(
                            partes
                    )
            );
        }

        tarjeta.addView(encabezado);
        tarjeta.addView(txtLugar);
        tarjeta.addView(txtResponsable);
        tarjeta.addView(txtEstados);
        tarjeta.addView(acciones);

        return tarjeta;
    }

    private TextView crearTextoDetalle(
            String texto
    ) {
        TextView textView =
                new TextView(this);

        textView.setText(texto);
        textView.setTextSize(13);

        textView.setTextColor(
                Color.parseColor("#374151")
        );

        return textView;
    }

    private TextView crearBoton(
            String texto,
            boolean principal
    ) {
        TextView boton =
                new TextView(this);

        boton.setText(texto);
        boton.setTextSize(12);

        boton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        boton.setGravity(
                Gravity.CENTER
        );

        boton.setClickable(true);
        boton.setFocusable(true);

        if (principal) {
            boton.setBackgroundResource(
                    R.drawable.bg_button_login
            );

            boton.setTextColor(
                    Color.WHITE
            );
        } else {
            boton.setBackgroundResource(
                    R.drawable.bg_input_login
            );

            boton.setTextColor(
                    Color.parseColor("#006341")
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1
                );

        params.setMargins(
                dp(3),
                0,
                dp(3),
                0
        );

        boton.setLayoutParams(params);

        return boton;
    }

    private boolean tieneAccesoRevision(
            String estadoGeneral,
            String estadoReporte
    ) {
        return
                "Pendiente de revisión".equalsIgnoreCase(
                        estadoGeneral
                ) ||

                        "Enviado".equalsIgnoreCase(
                                estadoReporte
                        ) ||

                        "Con observaciones".equalsIgnoreCase(
                                estadoGeneral
                        ) ||

                        "Con observaciones".equalsIgnoreCase(
                                estadoReporte
                        ) ||

                        "Finalizada".equalsIgnoreCase(
                                estadoGeneral
                        ) ||

                        "Validado".equalsIgnoreCase(
                                estadoReporte
                        );
    }

    private void abrirChecklist(
            String folio,
            String circuito,
            String responsable
    ) {
        Intent intent = new Intent(
                SupervisionesSupervisorActivity.this,
                ChecklistSeguridadActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        intent.putExtra(
                "circuito",
                circuito
        );

        intent.putExtra(
                "responsable",
                responsable
        );

        startActivity(intent);
    }

    private void abrirRevision(
            String folio
    ) {
        Intent intent = new Intent(
                SupervisionesSupervisorActivity.this,
                RevisionSupervisionSupervisorActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
    }

    private void abrirReportePdf(
            String folio
    ) {
        Intent intent = new Intent(
                SupervisionesSupervisorActivity.this,
                ReportePdfSupervisorActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
    }

    private void mostrarDetalle(
            String[] partes
    ) {
        String apoyos =
                partes[10].trim();

        if (apoyos.isEmpty()) {
            apoyos =
                    "Sin personal de apoyo";
        }

        String observaciones =
                partes[6].trim();

        if (observaciones.isEmpty()) {
            observaciones =
                    "Sin observaciones";
        }

        String mensaje =
                "Folio: " +
                        partes[0] +
                        "\n" +

                        "Fecha: " +
                        partes[1] +
                        "\n" +

                        "Circuito: " +
                        partes[2] +
                        "\n" +

                        "Lugar: " +
                        partes[3] +
                        "\n" +

                        "Prioridad: " +
                        partes[4] +
                        "\n\n" +

                        "Descripción:\n" +
                        partes[5] +
                        "\n\n" +

                        "Responsable:\n" +
                        partes[8] +
                        "\n\n" +

                        "Personal de apoyo:\n" +
                        apoyos +
                        "\n\n" +

                        "Observaciones:\n" +
                        observaciones +
                        "\n\n" +

                        "Estado: " +
                        partes[11] +
                        "\n" +

                        "Checklist: " +
                        partes[12] +
                        "\n" +

                        "Reporte técnico: " +
                        partes[13] +
                        "\n" +

                        "PDF: " +
                        obtenerEstadoPdf(
                                partes[0]
                        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Detalle de supervisión"
                )
                .setMessage(mensaje)
                .setPositiveButton(
                        "Cerrar",
                        null
                )
                .show();
    }

    private String obtenerEstadoPdf(
            String folio
    ) {
        File archivo =
                obtenerArchivoPdf(folio);

        return archivo != null &&
                archivo.exists()
                ? "Generado"
                : "No generado";
    }

    private String obtenerFechaPdf(
            String folio
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PDF,
                        MODE_PRIVATE
                );

        String fecha =
                preferences.getString(
                        PREFIJO_FECHA_PDF +
                                folio,
                        ""
                );

        return fecha == null
                ? ""
                : fecha.trim();
    }

    private File obtenerArchivoPdf(
            String folio
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PDF,
                        MODE_PRIVATE
                );

        String ruta =
                preferences.getString(
                        PREFIJO_RUTA_PDF +
                                folio,
                        ""
                );

        if (ruta != null &&
                !ruta.trim().isEmpty()) {

            File archivo =
                    new File(ruta);

            if (archivo.exists()) {
                return archivo;
            }
        }

        String folioLimpio =
                folio.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        File archivoEsperado =
                new File(
                        new File(
                                getFilesDir(),
                                "reportes"
                        ),
                        "Reporte_Supervision_" +
                                folioLimpio +
                                ".pdf"
                );

        if (archivoEsperado.exists()) {
            return archivoEsperado;
        }

        return null;
    }

    private void confirmarEliminacion(
            String folio,
            int indice
    ) {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Eliminar supervisión"
                )
                .setMessage(
                        "¿Deseas eliminar la supervisión " +
                                folio +
                                "?\n\n" +
                                "También se eliminará su checklist."
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Eliminar",
                        (dialog, which) ->
                                eliminarSupervision(
                                        folio,
                                        indice
                                )
                )
                .show();
    }

    private void eliminarSupervision(
            String folio,
            int indice
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

        for (int i = 0;
             i < registros.length;
             i++) {

            if (i == indice ||
                    registros[i].trim().isEmpty()) {

                continue;
            }

            if (actualizados.length() > 0) {
                actualizados.append("\n");
            }

            actualizados.append(
                    registros[i]
            );
        }

        preferences.edit()
                .putString(
                        KEY_SUPERVISIONES,
                        actualizados.toString()
                )
                .apply();

        eliminarChecklistAsociado(
                folio
        );

        Toast.makeText(
                this,
                "Supervisión eliminada",
                Toast.LENGTH_SHORT
        ).show();

        cargarSupervisiones(
                etBuscarSupervision
                        .getText()
                        .toString()
                        .trim()
        );
    }

    private void eliminarChecklistAsociado(
            String folio
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

            if (partes.length == 0) {
                continue;
            }

            if (folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            if (actualizados.length() > 0) {
                actualizados.append("\n");
            }

            actualizados.append(registro);
        }

        preferences.edit()
                .putString(
                        KEY_CHECKLISTS,
                        actualizados.toString()
                )
                .apply();
    }

    private void mostrarEstadoVacio() {
        if (modoReportes) {
            txtCantidadSupervisiones.setText(
                    "0 reportes"
            );
        } else {
            txtCantidadSupervisiones.setText(
                    "0 supervisiones"
            );
        }

        emptyState.setVisibility(
                View.VISIBLE
        );

        containerSupervisiones.setVisibility(
                View.GONE
        );
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}