package com.pmp.front.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiPerfilActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_USUARIO = "usuario_actual"; // Almacena el correo/usuario con el que logueó
    private static final String KEY_NOMBRE = "nombre_actual";
    private static final String KEY_ROL = "rol_actual";

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

    // Hilos de fondo nativos para llamadas HTTP
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_perfil);

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarEventos();

        // Bloqueamos por defecto los campos que nunca cambian
        bloquearCampo(etUsuarioPerfil);
        bloquearCampo(etRolPerfil);
        bloquearCampo(etAreaCargoPerfil); // El rol/área se gestiona desde DB

        // Cargar los datos del perfil consumiendo el API de Spring Boot
        obtenerPerfilDesdeServidor();
    }

    private void inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver);
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil);
        txtTituloPerfil = findViewById(R.id.txtTituloPerfil);
        txtSubtituloPerfil = findViewById(R.id.txtSubtituloPerfil);
        txtModoPerfil = findViewById(R.id.txtModoPerfil);
        txtEstadoPerfil = findViewById(R.id.txtEstadoPerfil);
        etNombrePerfil = findViewById(R.id.etNombrePerfil);
        etUsuarioPerfil = findViewById(R.id.etUsuarioPerfil);
        etRolPerfil = findViewById(R.id.etRolPerfil);
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil);
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil);
        etAreaCargoPerfil = findViewById(R.id.etAreaCargoPerfil);
    }

    private boolean cargarSesion() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        usuarioActual = valorSeguro(preferences.getString(KEY_USUARIO, ""));
        nombreActual = valorSeguro(preferences.getString(KEY_NOMBRE, ""));
        rolActual = valorSeguro(preferences.getString(KEY_ROL, ""));

        boolean rolValido = "administrador".equalsIgnoreCase(rolActual) || "supervisor".equalsIgnoreCase(rolActual);

        if (usuarioActual.isEmpty() || !rolValido) {
            regresarLogin();
            return false;
        }
        return true;
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v -> finish());
        btnGuardarPerfil.setOnClickListener(v -> guardarPerfilEnServidor());
    }

    private void obtenerPerfilDesdeServidor() {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Pasamos el nombre de usuario como Query Parameter (?username=...)
                URL url = new URL(Config.BASE_URL + "/api/usuarios/perfil?username=" + usuarioActual);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(5000);

                int code = connection.getResponseCode();

                if (code == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder responseStr = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line.trim());
                    }

                    JSONObject perfilJson = new JSONObject(responseStr.toString());
                    String nombre = perfilJson.getString("nombre");
                    String email = perfilJson.getString("email");
                    String rpe = perfilJson.getString("rpe");
                    String telefono = perfilJson.getString("telefono");
                    int idRol = perfilJson.getInt("id_rol");

                    mainHandler.post(() -> {
                        // Asignar los datos obtenidos de la Base de Datos a los EditTexts
                        etNombrePerfil.setText(nombre);
                        etUsuarioPerfil.setText(rpe); // RPE del usuario
                        etCorreoPerfil.setText(email);
                        etTelefonoPerfil.setText(telefono);

                        if (idRol == 1) {
                            etRolPerfil.setText("Supervisor");
                            etAreaCargoPerfil.setText("Área de Supervisión");
                            configurarModoSupervisor();
                        } else {
                            etRolPerfil.setText("Técnico");
                            etAreaCargoPerfil.setText("Cuerpo Técnico Operativo");
                            configurarModoTecnico();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(MiPerfilActivity.this, "Error al cargar perfil desde el servidor", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void configurarModoTecnico() {
        txtTituloPerfil.setText("Mi perfil");
        txtSubtituloPerfil.setText("Consulta de información del Técnico");
        txtModoPerfil.setText("Modo lectura");
        txtEstadoPerfil.setText("Tus datos son administrados por el Supervisor.");
        btnGuardarPerfil.setVisibility(View.GONE);

        bloquearCampo(etNombrePerfil);
        bloquearCampo(etCorreoPerfil);
        bloquearCampo(etTelefonoPerfil);
    }

    private void configurarModoSupervisor() {
        txtTituloPerfil.setText("Perfil del Supervisor");
        txtSubtituloPerfil.setText("Consulta y edición de datos básicos");
        txtModoPerfil.setText("Modo edición");
        txtEstadoPerfil.setText("Puedes actualizar tu nombre visible y datos de contacto.");
        btnGuardarPerfil.setVisibility(View.VISIBLE);

        habilitarCampo(etNombrePerfil);
        habilitarCampo(etCorreoPerfil);
        habilitarCampo(etTelefonoPerfil);
    }

    private void guardarPerfilEnServidor() {
        String nombre = etNombrePerfil.getText().toString().trim();
        String correo = etCorreoPerfil.getText().toString().trim();
        String telefono = etTelefonoPerfil.getText().toString().trim();

        String error;

        // Nombre -> columna nombre VARCHAR(255) NOT NULL
        error = com.pmp.front.Validaciones.obligatorio(nombre, "el nombre completo", 255);
        if (error != null) {
            etNombrePerfil.setError(error);
            etNombrePerfil.requestFocus();
            return;
        }

        // Correo -> columna email VARCHAR(255)
        if (correo.isEmpty()) {
            etCorreoPerfil.setError("Escribe el correo electrónico. Ejemplo: juan.perez@cfe.mx");
            etCorreoPerfil.requestFocus();
            return;
        }
        error = com.pmp.front.Validaciones.email(correo);
        if (error != null) {
            etCorreoPerfil.setError(error);
            etCorreoPerfil.requestFocus();
            return;
        }

        // Teléfono -> columna telefono VARCHAR(255) (opcional)
        error = com.pmp.front.Validaciones.telefono(telefono);
        if (error != null) {
            etTelefonoPerfil.setError(error);
            etTelefonoPerfil.requestFocus();
            return;
        }

        if (!com.pmp.front.RedHelper.hayConexion(this)) {
            android.widget.Toast.makeText(this,
                    "Sin conexión a la red. Conéctate al WiFi para guardar tu perfil.",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        btnGuardarPerfil.setEnabled(false);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Config.BASE_URL + "/api/usuarios/perfil/actualizar");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("usernameOriginal", usuarioActual); // Llave para buscar en DB (username, ya no cambia con el correo)
                jsonParam.put("nombre", nombre);
                jsonParam.put("email", correo);
                jsonParam.put("telefono", telefono);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();

                mainHandler.post(() -> {
                    btnGuardarPerfil.setEnabled(true);
                    if (code == HttpURLConnection.HTTP_OK) {
                        // Actualizar SharedPreferences de la sesión local activa
                        // (el username no cambia aquí; solo se actualiza el nombre mostrado)
                        SharedPreferences sesion = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
                        sesion.edit()
                                .putString(KEY_NOMBRE, nombre)
                                .apply();

                        nombreActual = nombre;

                        Toast.makeText(MiPerfilActivity.this, "Perfil actualizado en base de datos", Toast.LENGTH_SHORT).show();

                        new AlertDialog.Builder(MiPerfilActivity.this)
                                .setTitle("Perfil guardado")
                                .setMessage("La información en MySQL fue actualizada correctamente.")
                                .setPositiveButton("Aceptar", null)
                                .show();
                    } else {
                        Toast.makeText(MiPerfilActivity.this, "Error al actualizar los datos en el servidor", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnGuardarPerfil.setEnabled(true);
                    Toast.makeText(MiPerfilActivity.this, "Error de red al intentar guardar", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void bloquearCampo(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setLongClickable(false);
    }

    private void habilitarCampo(EditText editText) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setCursorVisible(true);
        editText.setLongClickable(true);
    }

    private void regresarLogin() {
        Intent intent = new Intent(MiPerfilActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }
}