package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonalOperativoActivity extends Activity {

    private LinearLayout contenedorPersonal;
    private TextView txtContador, emptyState, btnVolver, btnAgregarTecnico;
    private EditText etBuscarTecnico;

    // Hilos para comunicación asíncrona con el Servidor
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_operativo);

        contenedorPersonal = findViewById(R.id.contenedorPersonal);
        txtContador = findViewById(R.id.txtContador);
        emptyState = findViewById(R.id.emptyState);
        btnVolver = findViewById(R.id.btnVolver);
        btnAgregarTecnico = findViewById(R.id.btnAgregarTecnico);
        etBuscarTecnico = findViewById(R.id.etBuscarTecnico);

        btnVolver.setOnClickListener(v -> finish());

        btnAgregarTecnico.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrarTecnicoActivity.class))
        );

        etBuscarTecnico.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                obtenerTecnicosDesdeServidor(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        obtenerTecnicosDesdeServidor(etBuscarTecnico.getText().toString());
    }

    // =========================================================================
    // MODIFICADO: CARGA EN TIEMPO REAL DESDE EL BACKEND EN SPRING BOOT
    // =========================================================================
    private void obtenerTecnicosDesdeServidor(String filtro) {
        executorService.execute(() -> {
            try {
                // Consumimos el endpoint global de usuarios
                URL url = new URL(Config.BASE_URL + "/api/usuarios");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONArray jsonArray = new JSONArray(sb.toString());

                    mainHandler.post(() -> procesarYFiltrarTecnicos(jsonArray, filtro));
                } else {
                    mainHandler.post(() -> {
                        txtContador.setText("Error al conectar");
                        emptyState.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    txtContador.setText("Error de red");
                    emptyState.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void procesarYFiltrarTecnicos(JSONArray jsonArray, String filtro) {
        contenedorPersonal.removeAllViews();
        int encontrados = 0;

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                // IMPORTANTE: Discriminamos para listar únicamente al personal con rol Supervisor (id_rol = 2)
                int idRol = obj.optInt("id_rol", -1);
                if (idRol != 2) {
                    continue;
                }

                String nombre = obj.optString("nombre", "Sin Nombre");
                String correo = obj.optString("email", "Sin Correo");
                String telefono = obj.optString("telefono", "Sin Teléfono");
                String zona = obj.optString("zona", "Sin Zona");
                String username = obj.optString("username", "Sin Usuario");
                String idUsuario = obj.optString("id_usuario", "");

                String busqueda = (nombre + correo + telefono + zona + username).toLowerCase();

                if (!filtro.trim().isEmpty() && !busqueda.contains(filtro.toLowerCase())) {
                    continue;
                }

                encontrados++;
                contenedorPersonal.addView(
                        crearCardTecnico(idUsuario, nombre, correo, telefono, zona, username, obj.toString())
                );
            }

            txtContador.setText(encontrados + " técnicos encontrados");
            emptyState.setVisibility(encontrados == 0 ? View.VISIBLE : View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar la lista", Toast.LENGTH_SHORT).show();
        }
    }

    private View crearCardTecnico(
            String idUsuario,
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario,
            String jsonRawString
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_green);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(5));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        LinearLayout filaSuperior = new LinearLayout(this);
        filaSuperior.setOrientation(LinearLayout.HORIZONTAL);
        filaSuperior.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        avatar.setText(iniciales(nombre));
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setTextSize(16);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setBackgroundColor(0xFF006341);

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatarParams.setMargins(0, 0, dp(12), 0);
        avatar.setLayoutParams(avatarParams);

        LinearLayout datos = new LinearLayout(this);
        datos.setOrientation(LinearLayout.VERTICAL);
        datos.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        datos.addView(texto(nombre, 17, 0xFF111827, true));
        datos.addView(texto(correo, 12, 0xFF4F6B5D, false));
        datos.addView(texto("Usuario: " + usuario, 12, 0xFF006341, true));
        datos.addView(texto("Técnico · " + zona, 12, 0xFF6B7280, false));
        datos.addView(texto(telefono, 12, 0xFF4F6B5D, false));

        TextView estado = texto("Activo", 11, 0xFF006341, true);
        estado.setGravity(Gravity.CENTER);
        estado.setBackgroundResource(R.drawable.bg_input_login);
        estado.setPadding(dp(10), dp(4), dp(10), dp(4));

        filaSuperior.addView(avatar);
        filaSuperior.addView(datos);
        filaSuperior.addView(estado);

        View linea = new View(this);
        linea.setBackgroundColor(0xFFC9D8D0);
        LinearLayout.LayoutParams lineaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        lineaParams.setMargins(0, dp(14), 0, dp(12));
        linea.setLayoutParams(lineaParams);

        LinearLayout acciones = new LinearLayout(this);
        acciones.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnVer = botonAccion("👁  Ver");
        TextView btnEditar = botonAccion("✎  Editar");
        TextView btnEliminar = botonAccion("🗑");

        btnVer.setOnClickListener(v ->
                mostrarDetalle(nombre, correo, telefono, zona, usuario)
        );

        btnEditar.setOnClickListener(v -> editarTecnico(nombre, correo, telefono, zona, usuario, jsonRawString));

        btnEliminar.setOnClickListener(v -> confirmarEliminar(idUsuario, nombre));

        acciones.addView(btnVer);
        acciones.addView(btnEditar);
        acciones.addView(btnEliminar);

        card.addView(filaSuperior);
        card.addView(linea);
        card.addView(acciones);

        return card;
    }

    private void editarTecnico(
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario,
            String jsonRawString
    ) {
        String password = "";
        try {
            JSONObject obj = new JSONObject(jsonRawString);
            password = obj.optString("contraseña", ""); // Recupera el password del payload completo
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, RegistrarTecnicoActivity.class);
        intent.putExtra("editMode", true);
        intent.putExtra("nombre", nombre);
        intent.putExtra("correo", correo);
        intent.putExtra("telefono", telefono);
        intent.putExtra("zona", zona);
        intent.putExtra("usuario", usuario); // Mapeado al username
        intent.putExtra("password", password);
        startActivity(intent);
    }

    private TextView botonAccion(String texto) {
        TextView boton = new TextView(this);
        boton.setText(texto);
        boton.setGravity(Gravity.CENTER);
        boton.setTextColor(0xFF006341);
        boton.setTextSize(13);
        boton.setTypeface(null, Typeface.BOLD);
        boton.setBackgroundResource(R.drawable.bg_input_login);
        boton.setPadding(dp(8), 0, dp(8), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(44),
                1
        );
        params.setMargins(dp(4), 0, dp(4), 0);
        boton.setLayoutParams(params);

        return boton;
    }

    private TextView texto(String contenido, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(contenido);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private void mostrarDetalle(
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario
    ) {
        new AlertDialog.Builder(this)
                .setTitle(nombre)
                .setMessage(
                        "Correo: " + correo +
                                "\nTeléfono: " + telefono +
                                "\nZona/Cuadrilla: " + zona +
                                "\nUsuario: " + usuario +
                                "\nEstado: Activo"
                )
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void confirmarEliminar(String idUsuario, String nombre) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar técnico")
                .setMessage("¿Deseas eliminar a " + nombre + " de la base de datos remota?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTecnicoDesdeServidor(idUsuario))
                .show();
    }

    // =========================================================================
    // MODIFICADO: ELIMINACIÓN ASÍNCRONA VÍA HTTP DELETE EN EL SERVIDOR
    // =========================================================================
    private void eliminarTecnicoDesdeServidor(String idUsuario) {
        if (idUsuario == null || idUsuario.isEmpty()) {
            Toast.makeText(this, "ID inválido para eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/usuarios/" + idUsuario);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");

                int responseCode = connection.getResponseCode();

                // Si el servidor rechaza el borrado, leemos su mensaje para
                // explicarle al usuario POR QUÉ no se pudo (por ejemplo, que la
                // cuenta tiene supervisiones relacionadas).
                String mensajeServidor = "";
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                    try {
                        java.io.InputStream errStream = connection.getErrorStream();
                        if (errStream != null) {
                            java.io.BufferedReader br = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(errStream, "utf-8"));
                            StringBuilder sb = new StringBuilder();
                            String linea;
                            while ((linea = br.readLine()) != null) sb.append(linea.trim());
                            if (sb.length() > 0) {
                                mensajeServidor = new org.json.JSONObject(sb.toString())
                                        .optString("message", "");
                            }
                        }
                    } catch (Exception ignorado) {
                    }
                }

                final String mensajeFinal = mensajeServidor;

                mainHandler.post(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        Toast.makeText(this, "Técnico eliminado con éxito", Toast.LENGTH_SHORT).show();
                        obtenerTecnicosDesdeServidor(etBuscarTecnico.getText().toString());
                    } else {
                        Toast.makeText(this,
                                mensajeFinal.isEmpty() ? "Error al eliminar en el servidor" : mensajeFinal,
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Error de red al intentar eliminar", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String iniciales(String nombre) {
        String[] partes = nombre.trim().split(" ");

        if (partes.length == 0 || partes[0].isEmpty()) {
            return "T";
        }

        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }

        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}