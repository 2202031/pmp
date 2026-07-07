package com.pmp.front.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MiPerfilActivity extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

    private static final String KEY_NOMBRE =
            "nombre_actual";

    private static final String KEY_ROL =
            "rol_actual";

    private static final String PREFS_PERSONAL =
            "personal_operativo";

    private static final String KEY_TECNICOS =
            "tecnicos";

    private static final String PREFS_PERFILES =
            "perfiles_usuario_local";

    private static final String PREFIJO_PERFIL =
            "perfil_";

    private TextView btnVolver;
    private TextView btnGuardarPerfil;

    private TextView txtTituloPerfil;
    private TextView txtSubtituloPerfil;
    private TextView txtModoPerfil;
    private TextView txtEstadoPerfil;

    private EditText etNombrePerfil;
    private EditText etUsuarioPerfil;
    private EditText etRolPerfil;
    private EditText etCorreoPerfil;
    private EditText etTelefonoPerfil;
    private EditText etAreaCargoPerfil;

    private String usuarioActual = "";
    private String nombreActual = "";
    private String rolActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_mi_perfil
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarEventos();
        cargarPerfil();
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(
                        R.id.btnVolver
                );

        btnGuardarPerfil =
                findViewById(
                        R.id.btnGuardarPerfil
                );

        txtTituloPerfil =
                findViewById(
                        R.id.txtTituloPerfil
                );

        txtSubtituloPerfil =
                findViewById(
                        R.id.txtSubtituloPerfil
                );

        txtModoPerfil =
                findViewById(
                        R.id.txtModoPerfil
                );

        txtEstadoPerfil =
                findViewById(
                        R.id.txtEstadoPerfil
                );

        etNombrePerfil =
                findViewById(
                        R.id.etNombrePerfil
                );

        etUsuarioPerfil =
                findViewById(
                        R.id.etUsuarioPerfil
                );

        etRolPerfil =
                findViewById(
                        R.id.etRolPerfil
                );

        etCorreoPerfil =
                findViewById(
                        R.id.etCorreoPerfil
                );

        etTelefonoPerfil =
                findViewById(
                        R.id.etTelefonoPerfil
                );

        etAreaCargoPerfil =
                findViewById(
                        R.id.etAreaCargoPerfil
                );
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        usuarioActual =
                valorSeguro(
                        preferences.getString(
                                KEY_USUARIO,
                                ""
                        )
                );

        nombreActual =
                valorSeguro(
                        preferences.getString(
                                KEY_NOMBRE,
                                ""
                        )
                );

        rolActual =
                valorSeguro(
                        preferences.getString(
                                KEY_ROL,
                                ""
                        )
                );

        boolean rolValido =
                "supervisor".equalsIgnoreCase(
                        rolActual
                ) ||
                        "tecnico".equalsIgnoreCase(
                                rolActual
                        );

        if (usuarioActual.isEmpty() ||
                !rolValido) {

            regresarLogin();
            return false;
        }

        if (nombreActual.isEmpty()) {
            nombreActual =
                    usuarioActual;
        }

        return true;
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnGuardarPerfil.setOnClickListener(
                v -> guardarPerfilSupervisor()
        );
    }

    private void cargarPerfil() {
        etUsuarioPerfil.setText(
                usuarioActual
        );

        etRolPerfil.setText(
                obtenerRolVisible()
        );

        bloquearCampo(
                etUsuarioPerfil
        );

        bloquearCampo(
                etRolPerfil
        );

        if ("tecnico".equalsIgnoreCase(
                rolActual
        )) {
            configurarPerfilTecnico();
        } else {
            configurarPerfilSupervisor();
        }
    }

    private void configurarPerfilTecnico() {
        txtTituloPerfil.setText(
                "Mi perfil"
        );

        txtSubtituloPerfil.setText(
                "Consulta de información del Técnico"
        );

        txtModoPerfil.setText(
                "Modo lectura"
        );

        txtEstadoPerfil.setText(
                "Tus datos son administrados por el Supervisor."
        );

        btnGuardarPerfil.setVisibility(
                View.GONE
        );

        cargarDatosTecnico();

        bloquearCampo(
                etNombrePerfil
        );

        bloquearCampo(
                etCorreoPerfil
        );

        bloquearCampo(
                etTelefonoPerfil
        );

        bloquearCampo(
                etAreaCargoPerfil
        );
    }

    private void configurarPerfilSupervisor() {
        txtTituloPerfil.setText(
                "Perfil del Supervisor"
        );

        txtSubtituloPerfil.setText(
                "Consulta y edición de datos básicos"
        );

        txtModoPerfil.setText(
                "Modo edición"
        );

        txtEstadoPerfil.setText(
                "Puedes actualizar tu nombre visible y datos de contacto."
        );

        btnGuardarPerfil.setVisibility(
                View.VISIBLE
        );

        cargarDatosSupervisor();

        habilitarCampo(
                etNombrePerfil
        );

        habilitarCampo(
                etCorreoPerfil
        );

        habilitarCampo(
                etTelefonoPerfil
        );

        habilitarCampo(
                etAreaCargoPerfil
        );
    }

    private void cargarDatosTecnico() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PERSONAL,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_TECNICOS,
                        ""
                );

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

                /*
                 * Formato esperado del personal:
                 *
                 * 0 nombre
                 * 1 correo
                 * 2 teléfono
                 * 3 zona o área
                 * 4 usuario
                 * 5 contraseña
                 */

                if (partes.length < 6) {
                    continue;
                }

                if (usuarioActual.equalsIgnoreCase(
                        partes[4].trim()
                )) {

                    etNombrePerfil.setText(
                            valorVisible(
                                    partes[0],
                                    nombreActual
                            )
                    );

                    etCorreoPerfil.setText(
                            valorVisible(
                                    partes[1],
                                    "Sin correo registrado"
                            )
                    );

                    etTelefonoPerfil.setText(
                            valorVisible(
                                    partes[2],
                                    "Sin teléfono registrado"
                            )
                    );

                    etAreaCargoPerfil.setText(
                            valorVisible(
                                    partes[3],
                                    "Sin área registrada"
                            )
                    );

                    return;
                }
            }
        }

        etNombrePerfil.setText(
                nombreActual
        );

        etCorreoPerfil.setText(
                "Sin correo registrado"
        );

        etTelefonoPerfil.setText(
                "Sin teléfono registrado"
        );

        etAreaCargoPerfil.setText(
                "Sin área registrada"
        );
    }

    private void cargarDatosSupervisor() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PERFILES,
                        MODE_PRIVATE
                );

        String registro =
                preferences.getString(
                        obtenerClavePerfil(),
                        ""
                );

        if (registro == null ||
                registro.trim().isEmpty()) {

            etNombrePerfil.setText(
                    nombreActual
            );

            etCorreoPerfil.setText("");
            etTelefonoPerfil.setText("");
            etAreaCargoPerfil.setText(
                    "Supervisor"
            );

            return;
        }

        String[] partes =
                registro.split("\\|", -1);

        if (partes.length < 7) {
            etNombrePerfil.setText(
                    nombreActual
            );

            etCorreoPerfil.setText("");
            etTelefonoPerfil.setText("");
            etAreaCargoPerfil.setText(
                    "Supervisor"
            );

            return;
        }

        etNombrePerfil.setText(
                valorVisible(
                        partes[2],
                        nombreActual
                )
        );

        etCorreoPerfil.setText(
                partes[3]
        );

        etTelefonoPerfil.setText(
                partes[4]
        );

        etAreaCargoPerfil.setText(
                valorVisible(
                        partes[5],
                        "Supervisor"
                )
        );
    }

    private void guardarPerfilSupervisor() {
        if (!"supervisor".equalsIgnoreCase(
                rolActual
        )) {
            return;
        }

        String nombre =
                limpiar(
                        etNombrePerfil
                                .getText()
                                .toString()
                );

        String correo =
                limpiar(
                        etCorreoPerfil
                                .getText()
                                .toString()
                );

        String telefono =
                limpiar(
                        etTelefonoPerfil
                                .getText()
                                .toString()
                );

        String cargo =
                limpiar(
                        etAreaCargoPerfil
                                .getText()
                                .toString()
                );

        if (nombre.isEmpty()) {
            etNombrePerfil.setError(
                    "Ingresa el nombre"
            );

            etNombrePerfil.requestFocus();
            return;
        }

        if (cargo.isEmpty()) {
            cargo =
                    "Supervisor";
        }

        String fechaActualizacion =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        /*
         * Formato del perfil:
         *
         * 0 usuario
         * 1 rol
         * 2 nombre
         * 3 correo
         * 4 teléfono
         * 5 cargo o área
         * 6 fecha de actualización
         */

        String registro =
                limpiar(usuarioActual) + "|" +
                        limpiar(rolActual) + "|" +
                        nombre + "|" +
                        correo + "|" +
                        telefono + "|" +
                        cargo + "|" +
                        fechaActualizacion;

        SharedPreferences perfiles =
                getSharedPreferences(
                        PREFS_PERFILES,
                        MODE_PRIVATE
                );

        perfiles.edit()
                .putString(
                        obtenerClavePerfil(),
                        registro
                )
                .apply();

        SharedPreferences sesion =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        sesion.edit()
                .putString(
                        KEY_NOMBRE,
                        nombre
                )
                .apply();

        nombreActual =
                nombre;

        Toast.makeText(
                this,
                "Perfil actualizado",
                Toast.LENGTH_SHORT
        ).show();

        new AlertDialog.Builder(this)
                .setTitle(
                        "Perfil guardado"
                )
                .setMessage(
                        "La información del Supervisor fue actualizada correctamente."
                )
                .setPositiveButton(
                        "Aceptar",
                        null
                )
                .show();
    }

    private String obtenerClavePerfil() {
        return PREFIJO_PERFIL +
                rolActual.toLowerCase(
                        Locale.ROOT
                ) +
                "_" +
                usuarioActual.toLowerCase(
                        Locale.ROOT
                );
    }

    private String obtenerRolVisible() {
        if ("supervisor".equalsIgnoreCase(
                rolActual
        )) {
            return "Supervisor";
        }

        return "Técnico";
    }

    private void bloquearCampo(
            EditText editText
    ) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setLongClickable(false);
    }

    private void habilitarCampo(
            EditText editText
    ) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setCursorVisible(true);
        editText.setLongClickable(true);
    }

    private void regresarLogin() {
        Intent intent =
                new Intent(
                        MiPerfilActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private String valorSeguro(
            String valor
    ) {
        return valor == null
                ? ""
                : valor.trim();
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