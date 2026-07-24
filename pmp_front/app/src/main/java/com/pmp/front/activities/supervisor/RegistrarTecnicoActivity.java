package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistrarTecnicoActivity extends Activity {

    private EditText etNombreTecnico, etCorreoTecnico, etTelefonoTecnico, etZonaTecnico;
    private EditText etUsuarioTecnico, etPasswordTecnico, etConfirmarPasswordTecnico;
    private TextView btnGuardarTecnico, btnVolver;

    // Cambiado: Ahora manejamos un indicador booleano de edición y el ID único del usuario en la Base de Datos
    private boolean isEditMode = false;
    private String idUsuario = "";

    // Hilos para la comunicación asíncrona con el Backend de Spring Boot
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_tecnico);

        btnVolver = findViewById(R.id.btnVolver);
        btnGuardarTecnico = findViewById(R.id.btnGuardarTecnico);

        etNombreTecnico = findViewById(R.id.etNombreTecnico);
        etCorreoTecnico = findViewById(R.id.etCorreoTecnico);
        etTelefonoTecnico = findViewById(R.id.etTelefonoTecnico);
        etZonaTecnico = findViewById(R.id.etZonaTecnico);

        etUsuarioTecnico = findViewById(R.id.etUsuarioTecnico);
        etPasswordTecnico = findViewById(R.id.etPasswordTecnico);
        etConfirmarPasswordTecnico = findViewById(R.id.etConfirmarPasswordTecnico);

        // Obtenemos los parámetros enviados por PersonalOperativoActivity
        isEditMode = getIntent().getBooleanExtra("editMode", false);

        if (isEditMode) {
            idUsuario = getIntent().getStringExtra("usuario"); // Se usa el username o ID único como identificador
            etNombreTecnico.setText(getIntent().getStringExtra("nombre"));
            etCorreoTecnico.setText(getIntent().getStringExtra("correo"));
            etTelefonoTecnico.setText(getIntent().getStringExtra("telefono"));
            etZonaTecnico.setText(getIntent().getStringExtra("zona"));
            etUsuarioTecnico.setText(getIntent().getStringExtra("usuario"));
            etPasswordTecnico.setText(getIntent().getStringExtra("password"));
            etConfirmarPasswordTecnico.setText(getIntent().getStringExtra("password"));

            // Al editar, bloqueamos el campo de usuario ya que suele actuar como identificador único
            etUsuarioTecnico.setEnabled(false);
            btnGuardarTecnico.setText("Guardar cambios");
        }

        btnVolver.setOnClickListener(v -> finish());
        btnGuardarTecnico.setOnClickListener(v -> guardarTecnico());
    }

    private void guardarTecnico() {
        String nombre = limpiar(etNombreTecnico.getText().toString());
        String correo = limpiar(etCorreoTecnico.getText().toString());
        String telefono = limpiar(etTelefonoTecnico.getText().toString());
        String zona = limpiar(etZonaTecnico.getText().toString());
        String usuario = limpiar(etUsuarioTecnico.getText().toString());
        String password = limpiar(etPasswordTecnico.getText().toString());
        String confirmarPassword = limpiar(etConfirmarPasswordTecnico.getText().toString());

        if (!validarCampos(nombre, correo, telefono, zona, usuario, password, confirmarPassword)) {
            return;
        }

        btnGuardarTecnico.setEnabled(false);

        executorService.execute(() -> {
            try {
                URL url;
                String method;

                if (isEditMode) {
                    // Endpoint para que el Administrador actualice un Supervisor ya existente
                    url = new URL(Config.BASE_URL + "/api/usuarios/editar-supervisor");
                    method = "PUT";
                } else {
                    // Endpoint para registrar un nuevo usuario
                    url = new URL(Config.BASE_URL + "/api/usuarios/crear");
                    method = "POST";
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);

                // Construimos el JSON Payload que espera el backend
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("nombre", nombre);
                jsonParam.put("email", correo);
                jsonParam.put("telefono", telefono);
                jsonParam.put("zona", zona);
                jsonParam.put("username", usuario);
                jsonParam.put("password", password);
                jsonParam.put("idRol", 2); // Supervisor

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                mainHandler.post(() -> {
                    btnGuardarTecnico.setEnabled(true);
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        Toast.makeText(this, isEditMode ? "Técnico actualizado con éxito" : "Técnico registrado con éxito", Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                        etUsuarioTecnico.setError("Este usuario o correo ya se encuentra registrado");
                        Toast.makeText(this, "Conflicto: El usuario ya existe", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error del servidor al procesar la solicitud", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnGuardarTecnico.setEnabled(true);
                    Toast.makeText(this, "Error de red al conectar al servidor", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean validarCampos(
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario,
            String password,
            String confirmarPassword
    ) {
        String error;

        // Nombre -> columna nombre VARCHAR(255) NOT NULL
        error = com.pmp.front.Validaciones.obligatorio(nombre, "el nombre completo", 255);
        if (error != null) {
            etNombreTecnico.setError(error);
            etNombreTecnico.requestFocus();
            return false;
        }
        if (nombre.length() < 3) {
            etNombreTecnico.setError("El nombre es muy corto. Escribe al menos 3 caracteres.");
            etNombreTecnico.requestFocus();
            return false;
        }
        if (!nombre.matches("^[a-zA-Z\\u00e1\\u00e9\\u00ed\\u00f3\\u00fa\\u00c1\\u00c9\\u00cd\\u00d3\\u00da\\u00f1\\u00d1 ]+$")) {
            etNombreTecnico.setError("El nombre solo admite letras y espacios (sin numeros ni simbolos). Ejemplo: Juan Perez");
            etNombreTecnico.requestFocus();
            return false;
        }

        // Correo -> columna email VARCHAR(255) (opcional en BD, pero lo pedimos aqui)
        if (correo.isEmpty()) {
            etCorreoTecnico.setError("Escribe el correo electronico. Ejemplo: juan.perez@cfe.mx");
            etCorreoTecnico.requestFocus();
            return false;
        }
        error = com.pmp.front.Validaciones.email(correo);
        if (error != null) {
            etCorreoTecnico.setError(error);
            etCorreoTecnico.requestFocus();
            return false;
        }

        // Telefono -> columna telefono VARCHAR(255)
        if (telefono.isEmpty()) {
            etTelefonoTecnico.setError("Escribe el telefono a 10 digitos. Ejemplo: 5512345678");
            etTelefonoTecnico.requestFocus();
            return false;
        }
        error = com.pmp.front.Validaciones.telefono(telefono);
        if (error != null) {
            etTelefonoTecnico.setError(error);
            etTelefonoTecnico.requestFocus();
            return false;
        }

        // Zona -> columna zona VARCHAR(255)
        error = com.pmp.front.Validaciones.obligatorio(zona, "la zona o cuadrilla", 255);
        if (error != null) {
            etZonaTecnico.setError(error);
            etZonaTecnico.requestFocus();
            return false;
        }
        if (zona.length() < 3) {
            etZonaTecnico.setError("La zona es muy corta. Escribe al menos 3 caracteres.");
            etZonaTecnico.requestFocus();
            return false;
        }

        // Usuario -> columna username VARCHAR(255) NOT NULL UNIQUE
        error = com.pmp.front.Validaciones.username(usuario);
        if (error != null) {
            etUsuarioTecnico.setError(error);
            etUsuarioTecnico.requestFocus();
            return false;
        }
        if (usuario.length() < 4) {
            etUsuarioTecnico.setError("El usuario es muy corto. Escribe al menos 4 caracteres. Ejemplo: juan.perez");
            etUsuarioTecnico.requestFocus();
            return false;
        }

        // Contraseña -> columna contraseña VARCHAR(255) NOT NULL
        // Al EDITAR, dejarla vacía significa "no cambiar la contraseña actual".
        // Al CREAR, sí es obligatoria.
        if (password.isEmpty()) {
            if (!isEditMode) {
                etPasswordTecnico.setError("Escribe una contrasena. Este campo es obligatorio.");
                etPasswordTecnico.requestFocus();
                return false;
            }
            // En edición con contraseña vacía no hay nada más que validar.
            return true;
        }

        if (password.length() < 6) {
            etPasswordTecnico.setError("La contrasena es muy corta. Debe tener al menos 6 caracteres.");
            etPasswordTecnico.requestFocus();
            return false;
        }

        if (!password.equals(confirmarPassword)) {
            etConfirmarPasswordTecnico.setError("Las contrasenas no coinciden. Escribe exactamente la misma en ambos campos.");
            etConfirmarPasswordTecnico.requestFocus();
            return false;
        }

        return true;
    }

    private String limpiar(String texto) {
        return texto.trim().replace("|", "").replace("\n", " ");
    }
}