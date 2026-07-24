package com.pmp.front.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;
import com.pmp.front.activities.supervisor.DashboardSupervisorActivity;
import com.pmp.front.activities.tecnico.DashboardTecnicoActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_USUARIO = "usuario_actual";
    private static final String KEY_NOMBRE = "nombre_actual";
    private static final String KEY_ROL = "rol_actual";

    private EditText etUser;
    private EditText etPassword;
    private TextView btnLogin;

    // Ejecutor para realizar la petición HTTP en un hilo de fondo (Obligatorio en Android)
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Aseguramos que la URL del servidor esté cargada desde la configuración.
        Config.init(this);

        // Preparamos el canal de notificaciones y pedimos permiso (Android 13+).
        com.pmp.front.BarraNotificacionHelper.crearCanal(this);
        pedirPermisoNotificaciones();

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> iniciarSesion());
    }

    private void pedirPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void iniciarSesion() {
        String usuario = etUser.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String errorUsuario = com.pmp.front.Validaciones.username(usuario);
        if (errorUsuario != null) {
            etUser.setError(errorUsuario);
            etUser.requestFocus();
            return;
        }

        String errorPassword = com.pmp.front.Validaciones.password(password);
        if (errorPassword != null) {
            etPassword.setError(errorPassword);
            etPassword.requestFocus();
            return;
        }

        if (!com.pmp.front.RedHelper.hayConexion(this)) {
            Toast.makeText(this,
                    "Sin conexión a la red. Conéctate al WiFi para iniciar sesión.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Deshabilitamos el botón momentáneamente para evitar múltiples clics
        btnLogin.setEnabled(false);

        // Llamamos al método que conecta con el servidor de Spring Boot
        conectarConBackend(usuario, password);
    }

    private void conectarConBackend(String username, String password) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // URL apuntando a tu computadora desde el emulador de Android (10.0.2.2)
                URL url = new URL(Config.BASE_URL + "/api/auth/login");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000); // 5 segundos max de espera

                // Crear el JSON con las credenciales mapeadas a "username" tal como lo espera el backend
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", username);
                jsonParam.put("password", password);

                // Enviar el JSON en el cuerpo de la petición
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();

                // Si las credenciales son válidas (HTTP 200 OK)
                if (code == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder responseStr = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line.trim());
                    }

                    // Convertir la respuesta del servidor en un objeto JSON
                    JSONObject jsonResponse = new JSONObject(responseStr.toString());

                    // Validamos de forma segura las claves dinámicas devueltas por Spring
                    String nombre = jsonResponse.optString("nombre", "Usuario");
                    String usernameSesion = jsonResponse.optString("username", username);

                    // El backend envía "administrador" o "supervisor" en la clave "rol"
                    String rolString = jsonResponse.optString("rol", "supervisor");

                    // Regresamos al hilo principal de la interfaz para guardar sesión y cambiar de ventana
                    mainHandler.post(() -> {
                        btnLogin.setEnabled(true);

                        // Guardamos la sesión en SharedPreferences
                        guardarSesion(usernameSesion, nombre, rolString);

                        Intent intent;
                        // Mapeo de roles:
                        //  - "administrador" -> pantallas de la carpeta /supervisor (asigna, verifica, ve reportes PDF)
                        //  - "supervisor"    -> pantallas de la carpeta /tecnico (rellena checklist y reporte)
                        if ("administrador".equalsIgnoreCase(rolString)) {
                            intent = new Intent(LoginActivity.this, DashboardSupervisorActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, DashboardTecnicoActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    });

                } else {
                    // Cualquier respuesta distinta de 200. Leemos el mensaje real
                    // que envía el servidor para no mostrar una causa equivocada.
                    String mensajeServidor = "";
                    try {
                        InputStream errStream = connection.getErrorStream();
                        if (errStream != null) {
                            BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream, "utf-8"));
                            StringBuilder errStr = new StringBuilder();
                            String errLine;
                            while ((errLine = errReader.readLine()) != null) {
                                errStr.append(errLine.trim());
                            }
                            if (errStr.length() > 0) {
                                JSONObject errJson = new JSONObject(errStr.toString());
                                mensajeServidor = errJson.optString("message", "");
                            }
                        }
                    } catch (Exception ignorado) {
                        // Si no se puede leer el detalle, se usa un mensaje genérico.
                    }

                    final String mensajeFinal = mensajeServidor.isEmpty()
                            ? ("No se pudo iniciar sesión (código " + code + ")")
                            : mensajeServidor;

                    mainHandler.post(() -> {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, mensajeFinal, Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Error de comunicación o lectura de datos", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void guardarSesion(String usuario, String nombre, String rol) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        preferences.edit()
                .putString(KEY_USUARIO, usuario)
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_ROL, rol)
                .apply();
    }
}