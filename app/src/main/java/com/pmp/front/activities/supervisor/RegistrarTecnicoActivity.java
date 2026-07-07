package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;

public class RegistrarTecnicoActivity extends Activity {

    private EditText etNombreTecnico, etCorreoTecnico, etTelefonoTecnico, etZonaTecnico;
    private EditText etUsuarioTecnico, etPasswordTecnico, etConfirmarPasswordTecnico;
    private TextView btnGuardarTecnico, btnVolver;

    private static final String PREFS = "personal_operativo";
    private static final String KEY_TECNICOS = "tecnicos";

    private int editIndex = -1;

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

        editIndex = getIntent().getIntExtra("editIndex", -1);

        if (editIndex >= 0) {
            etNombreTecnico.setText(getIntent().getStringExtra("nombre"));
            etCorreoTecnico.setText(getIntent().getStringExtra("correo"));
            etTelefonoTecnico.setText(getIntent().getStringExtra("telefono"));
            etZonaTecnico.setText(getIntent().getStringExtra("zona"));
            etUsuarioTecnico.setText(getIntent().getStringExtra("usuario"));
            etPasswordTecnico.setText(getIntent().getStringExtra("password"));
            etConfirmarPasswordTecnico.setText(getIntent().getStringExtra("password"));
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

        String nuevoRegistro = nombre + "|" + correo + "|" + telefono + "|" + zona + "|" + usuario + "|" + password;

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String dataActual = prefs.getString(KEY_TECNICOS, "");

        if (editIndex >= 0) {
            String[] registros = dataActual.split("\n");
            StringBuilder actualizado = new StringBuilder();

            for (int i = 0; i < registros.length; i++) {
                if (i > 0) actualizado.append("\n");

                if (i == editIndex) {
                    actualizado.append(nuevoRegistro);
                } else {
                    actualizado.append(registros[i]);
                }
            }

            prefs.edit().putString(KEY_TECNICOS, actualizado.toString()).apply();
            Toast.makeText(this, "Técnico actualizado", Toast.LENGTH_SHORT).show();

        } else {
            String dataNueva = dataActual.isEmpty()
                    ? nuevoRegistro
                    : dataActual + "\n" + nuevoRegistro;

            prefs.edit().putString(KEY_TECNICOS, dataNueva).apply();
            Toast.makeText(this, "Técnico registrado", Toast.LENGTH_SHORT).show();
        }

        finish();
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
        if (nombre.isEmpty()) {
            etNombreTecnico.setError("Ingresa el nombre");
            return false;
        }

        if (nombre.length() < 3) {
            etNombreTecnico.setError("El nombre es demasiado corto");
            return false;
        }

        if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")) {
            etNombreTecnico.setError("El nombre solo debe contener letras");
            return false;
        }

        if (correo.isEmpty()) {
            etCorreoTecnico.setError("Ingresa el correo");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoTecnico.setError("Correo no válido");
            return false;
        }

        if (telefono.isEmpty()) {
            etTelefonoTecnico.setError("Ingresa el teléfono");
            return false;
        }

        if (!telefono.matches("^[0-9]{10}$")) {
            etTelefonoTecnico.setError("El teléfono debe tener 10 dígitos");
            return false;
        }

        if (zona.isEmpty()) {
            etZonaTecnico.setError("Ingresa la zona o cuadrilla");
            return false;
        }

        if (zona.length() < 3) {
            etZonaTecnico.setError("Zona demasiado corta");
            return false;
        }

        if (usuario.isEmpty()) {
            etUsuarioTecnico.setError("Ingresa un usuario");
            return false;
        }

        if (usuario.length() < 4) {
            etUsuarioTecnico.setError("El usuario debe tener mínimo 4 caracteres");
            return false;
        }

        if (!usuario.matches("^[a-zA-Z0-9._]+$")) {
            etUsuarioTecnico.setError("Usa solo letras, números, punto o guion bajo");
            return false;
        }

        if (usuarioDuplicado(usuario)) {
            etUsuarioTecnico.setError("Este usuario ya está registrado");
            return false;
        }

        if (password.isEmpty()) {
            etPasswordTecnico.setError("Ingresa una contraseña");
            return false;
        }

        if (password.length() < 6) {
            etPasswordTecnico.setError("La contraseña debe tener mínimo 6 caracteres");
            return false;
        }

        if (!password.equals(confirmarPassword)) {
            etConfirmarPasswordTecnico.setError("Las contraseñas no coinciden");
            return false;
        }

        return true;
    }

    private boolean usuarioDuplicado(String usuario) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String data = prefs.getString(KEY_TECNICOS, "");

        if (data.trim().isEmpty()) return false;

        String[] registros = data.split("\n");

        for (int i = 0; i < registros.length; i++) {
            if (i == editIndex) continue;

            String[] partes = registros[i].split("\\|");

            if (partes.length >= 5 && partes[4].equals(usuario)) {
                return true;
            }
        }

        return false;
    }

    private String limpiar(String texto) {
        return texto.trim().replace("|", "").replace("\n", " ");
    }
}