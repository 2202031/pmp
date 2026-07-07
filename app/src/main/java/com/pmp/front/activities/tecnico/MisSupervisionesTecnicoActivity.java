package com.pmp.front.activities.tecnico;

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

import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.util.Locale;

public class MisSupervisionesTecnicoActivity extends Activity {

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

    private TextView btnVolver;
    private TextView txtCantidadSupervisiones;

    private EditText etBuscarSupervision;

    private LinearLayout containerSupervisiones;
    private LinearLayout emptyState;

    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_mis_supervisiones_tecnico
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (usuarioActual != null &&
                !usuarioActual.isEmpty()) {

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

        txtCantidadSupervisiones =
                findViewById(
                        R.id.txtCantidadSupervisiones
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

        String textoBusqueda =
                busqueda.toLowerCase(Locale.ROOT);

        int visibles = 0;

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            String usuarioResponsable =
                    partes[7].trim();

            if (!usuarioActual.equalsIgnoreCase(
                    usuarioResponsable
            )) {
                continue;
            }

            if (!coincideBusqueda(
                    partes,
                    textoBusqueda
            )) {
                continue;
            }

            containerSupervisiones.addView(
                    crearTarjetaSupervision(partes)
            );

            visibles++;
        }

        txtCantidadSupervisiones.setText(
                visibles +
                        (
                                visibles == 1
                                        ? " supervisión"
                                        : " supervisiones"
                        )
        );

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
                        partes[11] + " " +
                        partes[12] + " " +
                        partes[13];

        return contenido
                .toLowerCase(Locale.ROOT)
                .contains(busqueda);
    }

    private View crearTarjetaSupervision(
            String[] partes
    ) {
        String folio = partes[0];
        String fecha = partes[1];
        String circuito = partes[2];
        String lugar = partes[3];
        String prioridad = partes[4];
        String estado = partes[11];
        String checklist = partes[12];
        String estadoReporte = partes[13];

        boolean checklistCompletado =
                "Completado".equalsIgnoreCase(
                        checklist
                );

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

        icono.setText("▣");
        icono.setTextSize(21);

        icono.setTextColor(
                Color.parseColor("#006341")
        );

        icono.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        icono.setGravity(Gravity.CENTER);

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

        badgeEstado.setText(estado);
        badgeEstado.setTextSize(11);

        badgeEstado.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        badgeEstado.setTextColor(
                Color.parseColor("#006341")
        );

        badgeEstado.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );

        GradientDrawable fondoBadge =
                new GradientDrawable();

        fondoBadge.setColor(
                Color.parseColor("#D1FAE5")
        );

        fondoBadge.setCornerRadius(
                dp(18)
        );

        badgeEstado.setBackground(
                fondoBadge
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

        TextView txtPrioridad =
                crearTextoDetalle(
                        "Prioridad: " + prioridad
                );

        TextView txtChecklist =
                crearTextoDetalle(
                        "Checklist: " + checklist
                );

        TextView txtReporte =
                crearTextoDetalle(
                        "Reporte técnico: " +
                                estadoReporte
                );

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

        TextView btnVer =
                crearBoton(
                        "Ver información",
                        false
                );

        String textoAccion;

        if (!checklistCompletado) {
            textoAccion =
                    "Reporte bloqueado";
        } else if (
                "Con observaciones".equalsIgnoreCase(
                        estado
                ) ||
                        "Con observaciones".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            textoAccion = "Corregir";
        } else if (
                "En proceso".equalsIgnoreCase(
                        estado
                ) ||
                        "En proceso".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            textoAccion = "Continuar";
        } else if (
                "Pendiente de revisión".equalsIgnoreCase(
                        estado
                ) ||
                        "Enviado".equalsIgnoreCase(
                                estadoReporte
                        )
        ) {
            textoAccion = "En revisión";
        } else if (
                "Finalizada".equalsIgnoreCase(
                        estado
                )
        ) {
            textoAccion = "Finalizada";
        } else {
            textoAccion =
                    "Abrir supervisión";
        }

        TextView btnAccion =
                crearBoton(
                        textoAccion,
                        checklistCompletado
                );

        btnVer.setOnClickListener(
                v -> abrirDetalleSupervision(
                        folio
                )
        );

        btnAccion.setOnClickListener(v -> {
            if (checklistCompletado) {
                abrirDetalleSupervision(
                        folio
                );
            } else {
                mostrarBloqueoChecklist(
                        folio
                );
            }
        });

        acciones.addView(btnVer);
        acciones.addView(btnAccion);

        tarjeta.setOnClickListener(
                v -> abrirDetalleSupervision(
                        folio
                )
        );

        tarjeta.addView(encabezado);
        tarjeta.addView(txtLugar);
        tarjeta.addView(txtPrioridad);
        tarjeta.addView(txtChecklist);
        tarjeta.addView(txtReporte);
        tarjeta.addView(acciones);

        return tarjeta;
    }

    private void abrirDetalleSupervision(
            String folio
    ) {
        Intent intent = new Intent(
                MisSupervisionesTecnicoActivity.this,
                DetalleSupervisionTecnicoActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
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

        boton.setGravity(Gravity.CENTER);
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

    private void mostrarBloqueoChecklist(
            String folio
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Reporte bloqueado")
                .setMessage(
                        "La supervisión " +
                                folio +
                                " todavía tiene pendiente el checklist " +
                                "de seguridad e higiene.\n\n" +
                                "El Supervisor debe completarlo antes " +
                                "de que puedas iniciar el reporte."
                )
                .setPositiveButton(
                        "Entendido",
                        null
                )
                .show();
    }

    private void mostrarEstadoVacio() {
        txtCantidadSupervisiones.setText(
                "0 supervisiones"
        );

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