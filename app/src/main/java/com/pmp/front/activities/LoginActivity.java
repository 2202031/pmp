package com.pmp.front.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;
import com.pmp.front.activities.supervisor.DashboardSupervisorActivity;
import com.pmp.front.activities.tecnico.DashboardTecnicoActivity;

public class LoginActivity extends Activity {

    private static final String PREFS_PERSONAL =
            "personal_operativo";

    private static final String KEY_TECNICOS =
            "tecnicos";

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

    private static final String KEY_NOMBRE =
            "nombre_actual";

    private static final String KEY_ROL =
            "rol_actual";

    private EditText etUser;
    private EditText etPassword;
    private TextView btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> iniciarSesion());
    }

    private void iniciarSesion() {
        String usuario = etUser.getText()
                .toString()
                .trim();

        String password = etPassword.getText()
                .toString()
                .trim();

        if (usuario.isEmpty()) {
            etUser.setError("Ingresa tu usuario");
            etUser.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return;
        }

        if (usuario.equalsIgnoreCase("admin")
                && password.equals("1234")) {

            guardarSesion(
                    "admin",
                    "Administrador",
                    "supervisor"
            );

            Intent intent = new Intent(
                    LoginActivity.this,
                    DashboardSupervisorActivity.class
            );

            startActivity(intent);
            finish();
            return;
        }

        String[] tecnicoEncontrado =
                buscarTecnicoLocal(usuario, password);

        if (tecnicoEncontrado != null) {
            String nombreTecnico =
                    tecnicoEncontrado[0].trim();

            String usuarioTecnico =
                    tecnicoEncontrado[4].trim();

            guardarSesion(
                    usuarioTecnico,
                    nombreTecnico,
                    "tecnico"
            );

            Intent intent = new Intent(
                    LoginActivity.this,
                    DashboardTecnicoActivity.class
            );

            startActivity(intent);
            finish();
            return;
        }

        Toast.makeText(
                this,
                "Usuario o contraseña incorrectos",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String[] buscarTecnicoLocal(
            String usuario,
            String password
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PERSONAL,
                        MODE_PRIVATE
                );

        String datos = preferences.getString(
                KEY_TECNICOS,
                ""
        );

        if (datos == null || datos.trim().isEmpty()) {
            return null;
        }

        String[] registros = datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            /*
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

            String usuarioGuardado =
                    partes[4].trim();

            String passwordGuardado =
                    partes[5].trim();

            if (usuario.equalsIgnoreCase(usuarioGuardado)
                    && password.equals(passwordGuardado)) {

                return partes;
            }
        }

        return null;
    }

    private void guardarSesion(
            String usuario,
            String nombre,
            String rol
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        preferences.edit()
                .putString(KEY_USUARIO, usuario)
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_ROL, rol)
                .apply();
    }
}