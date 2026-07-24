package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.Config;
import com.pmp.front.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsignarSupervisionActivity extends Activity {

    private TextView btnVolver, btnGuardarAsignacion, btnQuitarHoraProgramada, txtPersonalApoyo;
    private EditText etFolio, etFecha, etHoraProgramada, etLugar, etDescripcion, etObservaciones;
    private Spinner spCircuito, spPrioridad, spTecnicoResponsable;

    private final List<String> nombresTecnicos = new ArrayList<>();

    // MODIFICADO: Ahora almacena estrictamente los 'username' únicos para que coincidan con el flujo global
    private final List<String> usuariosTecnicos = new ArrayList<>();
    private final Set<String> apoyosSeleccionados = new LinkedHashSet<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asignar_supervision);

        inicializarVistas();
        configurarCircuitos();
        configurarPrioridades();
        configurarEventos();
        establecerFechaActual();

        // Carga el personal dinámicamente
        cargarTecnicosDesdeServidor();

        etHoraProgramada.setText("");
    }

    private void inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver);
        btnGuardarAsignacion = findViewById(R.id.btnGuardarAsignacion);
        btnQuitarHoraProgramada = findViewById(R.id.btnQuitarHoraProgramada);
        txtPersonalApoyo = findViewById(R.id.txtPersonalApoyo);
        etFolio = findViewById(R.id.etFolio);
        etFecha = findViewById(R.id.etFecha);
        etHoraProgramada = findViewById(R.id.etHoraProgramada);
        etLugar = findViewById(R.id.etLugar);
        etDescripcion = findViewById(R.id.etDescripcion);
        etObservaciones = findViewById(R.id.etObservaciones);
        spCircuito = findViewById(R.id.spCircuito);
        spPrioridad = findViewById(R.id.spPrioridad);
        spTecnicoResponsable = findViewById(R.id.spTecnicoResponsable);
    }

    private String obtenerUsernameAdministrador() {
        android.content.SharedPreferences preferences = getSharedPreferences("sesion_usuario", MODE_PRIVATE);
        return preferences.getString("usuario_actual", "");
    }

    private void cargarTecnicosDesdeServidor() {
        executorService.execute(() -> {
            try {
                // CORREGIDO: Apunta al endpoint correcto de usuarios operativos
                // Solo los técnicos registrados (id_rol = 2). Antes se pedía
                // /api/usuarios, que devuelve TODOS e incluía al administrador.
                URL url = new URL(Config.BASE_URL + "/api/usuarios/supervisores");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONArray jsonArray = new JSONArray(sb.toString());
                    nombresTecnicos.clear();
                    usuariosTecnicos.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        nombresTecnicos.add(obj.getString("nombre"));

                        // CORREGIDO: Almacenamos el username para vincular de forma inequívoca al técnico
                        usuariosTecnicos.add(obj.getString("username"));
                    }

                    mainHandler.post(this::configurarSpinnerTecnicos);
                } else {
                    mainHandler.post(this::configurarSpinnerSinTecnicos);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(this::configurarSpinnerSinTecnicos);
            }
        });
    }

    private void guardarAsignacion(String folio, String fecha, String horaProgramada, String lugar, String descripcion, String observaciones) {
        String circuito = spCircuito.getSelectedItem().toString();
        String prioridad = spPrioridad.getSelectedItem().toString();
        String usuarioResponsable = obtenerUsuarioResponsable(); // Devuelve el username

        btnGuardarAsignacion.setEnabled(false);

        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/asignaciones/guardar");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("folio", folio);
                jsonParam.put("fecha", fecha);
                jsonParam.put("horaProgramada", horaProgramada);
                jsonParam.put("lugar", lugar);
                jsonParam.put("prioridad", prioridad);
                jsonParam.put("descripcion", descripcion);
                jsonParam.put("observaciones", observaciones);

                // Personal de apoyo: se envían los username separados por coma.
                jsonParam.put("personalApoyo", android.text.TextUtils.join(",", apoyosSeleccionados));

                // Enviamos el username limpio al backend
                jsonParam.put("usuarioResponsable", usuarioResponsable);
                jsonParam.put("administradorUsername", obtenerUsernameAdministrador());

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                mainHandler.post(() -> {
                    btnGuardarAsignacion.setEnabled(true);
                    if (code == HttpURLConnection.HTTP_OK) {

                        // Notificamos al Supervisor recién asignado (username) sobre la nueva supervisión.
                        NotificacionesHelper.crear(this, usuarioResponsable, "supervisor", "Nueva supervisión", "Se te asignó el folio " + folio, "ASIGNACION", folio, exito -> {});

                        new AlertDialog.Builder(this)
                                .setTitle("Asignación registrada")
                                .setMessage("La supervisión con folio " + folio + " se subió correctamente a MySQL.")
                                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                                .show();
                    } else {
                        Toast.makeText(this, "Error del servidor al guardar", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnGuardarAsignacion.setEnabled(true);
                    Toast.makeText(this, "Error de red al conectar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void configurarCircuitos() {
        String[] circuitos = {"Selecciona un circuito", "Circuito 1", "Circuito 2", "Circuito 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, circuitos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCircuito.setAdapter(adapter);
    }

    private void configurarPrioridades() {
        String[] prioridades = {"Selecciona una prioridad", "Baja", "Media", "Alta", "Urgente"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrioridad.setAdapter(adapter);
    }

    private void configurarSpinnerSinTecnicos() {
        List<String> opciones = new ArrayList<>();
        opciones.add("No hay personal operativo en el servidor");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        spTecnicoResponsable.setAdapter(adapter);
        spTecnicoResponsable.setEnabled(false);
    }

    private void configurarSpinnerTecnicos() {
        List<String> opciones = new ArrayList<>();
        opciones.add("Selecciona un técnico responsable");
        for (int i = 0; i < nombresTecnicos.size(); i++) {
            // Muestra estéticamente: "Nombre Técnico • (username)"
            opciones.add(nombresTecnicos.get(i) + " • (" + usuariosTecnicos.get(i) + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTecnicoResponsable.setAdapter(adapter);
        spTecnicoResponsable.setEnabled(true);
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v -> finish());
        etFecha.setOnClickListener(v -> mostrarSelectorFecha());
        etHoraProgramada.setOnClickListener(v -> mostrarSelectorHora());
        btnQuitarHoraProgramada.setOnClickListener(v -> etHoraProgramada.setText(""));
        txtPersonalApoyo.setOnClickListener(v -> mostrarSelectorApoyos());
        btnGuardarAsignacion.setOnClickListener(v -> {
            if (!com.pmp.front.RedHelper.hayConexion(this)) {
                Toast.makeText(this,
                        "Sin conexión a la red. Conéctate al WiFi para guardar la asignación.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            validarYGuardar();
        });
        spTecnicoResponsable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                quitarResponsableDeApoyos();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void establecerFechaActual() {
        etFecha.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime()));
    }

    private void mostrarSelectorFecha() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> etFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarSelectorHora() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> etHoraProgramada.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void mostrarSelectorApoyos() {
        if (nombresTecnicos.isEmpty() || spTecnicoResponsable.getSelectedItemPosition() <= 0) {
            Toast.makeText(this,
                    "Primero selecciona al supervisor responsable.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String resp = obtenerUsuarioResponsable();

        // Lista de candidatos: todos los técnicos menos el responsable asignado.
        final List<String> usuariosDisponibles = new ArrayList<>();
        final List<String> nombresDisponibles = new ArrayList<>();
        for (int i = 0; i < usuariosTecnicos.size(); i++) {
            if (!usuariosTecnicos.get(i).equalsIgnoreCase(resp)) {
                usuariosDisponibles.add(usuariosTecnicos.get(i));
                nombresDisponibles.add(nombresTecnicos.get(i) + " • (" + usuariosTecnicos.get(i) + ")");
            }
        }

        if (usuariosDisponibles.isEmpty()) {
            Toast.makeText(this,
                    "No hay más técnicos registrados para agregar como apoyo.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String[] opciones = nombresDisponibles.toArray(new String[0]);
        final boolean[] marcados = new boolean[opciones.length];

        // Se preseleccionan los que ya estaban elegidos.
        for (int i = 0; i < usuariosDisponibles.size(); i++) {
            marcados[i] = apoyosSeleccionados.contains(usuariosDisponibles.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Personal de apoyo")
                .setMultiChoiceItems(opciones, marcados,
                        (dialog, which, isChecked) -> marcados[which] = isChecked)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    // Aquí sí se guarda la selección (antes el listener era null
                    // y por eso no pasaba nada al aceptar).
                    apoyosSeleccionados.clear();
                    for (int i = 0; i < marcados.length; i++) {
                        if (marcados[i]) {
                            apoyosSeleccionados.add(usuariosDisponibles.get(i));
                        }
                    }
                    actualizarTextoApoyos();
                })
                .show();
    }

    /** Muestra en pantalla a quiénes se eligió como apoyo. */
    private void actualizarTextoApoyos() {
        if (apoyosSeleccionados.isEmpty()) {
            txtPersonalApoyo.setText("Personal de apoyo (opcional)");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String usuario : apoyosSeleccionados) {
            if (sb.length() > 0) sb.append(", ");
            // Mostramos el nombre completo si lo tenemos; si no, el username.
            int idx = usuariosTecnicos.indexOf(usuario);
            sb.append(idx >= 0 ? nombresTecnicos.get(idx) : usuario);
        }

        txtPersonalApoyo.setText(
                apoyosSeleccionados.size() + " de apoyo: " + sb);
    }

    private void quitarResponsableDeApoyos() {
        String resp = obtenerUsuarioResponsable();
        if (!resp.isEmpty() && apoyosSeleccionados.remove(resp)) {
            actualizarTextoApoyos();
        }
    }

    private String obtenerUsuarioResponsable() {
        int pos = spTecnicoResponsable.getSelectedItemPosition();
        return pos <= 0 ? "" : usuariosTecnicos.get(pos - 1);
    }

    private String obtenerNombreResponsable() {
        int pos = spTecnicoResponsable.getSelectedItemPosition();
        return pos <= 0 ? "" : nombresTecnicos.get(pos - 1);
    }

    private void validarYGuardar() {
        String folio = etFolio.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        String lugar = etLugar.getText().toString().trim();
        String desc = etDescripcion.getText().toString().trim();
        String hora = etHoraProgramada.getText().toString().trim();
        String observaciones = etObservaciones.getText().toString().trim();

        // Folio: obligatorio, máx 50, sin espacios (columna folio VARCHAR(50) UNIQUE)
        String error = com.pmp.front.Validaciones.folio(folio);
        if (error != null) {
            etFolio.setError(error);
            etFolio.requestFocus();
            return;
        }

        // Fecha: obligatoria, formato dd/mm/aaaa (columna DATE NOT NULL)
        error = com.pmp.front.Validaciones.fecha(fecha);
        if (error != null) {
            etFecha.setError(error);
            etFecha.requestFocus();
            return;
        }

        // Lugar / referencia: obligatorio, máx 200 (columna referencia VARCHAR(200) NOT NULL)
        error = com.pmp.front.Validaciones.obligatorio(lugar, "el lugar o referencia", 200);
        if (error != null) {
            etLugar.setError(error);
            etLugar.requestFocus();
            return;
        }

        // Descripción: obligatoria, máx 100 (columna descripcion VARCHAR(100))
        error = com.pmp.front.Validaciones.obligatorio(desc, "la descripción", 100);
        if (error != null) {
            etDescripcion.setError(error);
            etDescripcion.requestFocus();
            return;
        }

        // Hora programada: opcional, formato HH:mm (columna VARCHAR(10))
        error = com.pmp.front.Validaciones.hora(hora);
        if (error != null) {
            etHoraProgramada.setError(error);
            etHoraProgramada.requestFocus();
            return;
        }

        // Observaciones: opcional (columna TEXT), limitamos a algo razonable
        error = com.pmp.front.Validaciones.opcional(observaciones, "las observaciones", 2000);
        if (error != null) {
            etObservaciones.setError(error);
            etObservaciones.requestFocus();
            return;
        }

        // Supervisor responsable: debe elegirse del spinner
        if (spTecnicoResponsable.getSelectedItemPosition() <= 0) {
            Toast.makeText(this,
                    "Selecciona el supervisor responsable de la lista. Es obligatorio para poder asignar.",
                    Toast.LENGTH_LONG).show();
            spTecnicoResponsable.requestFocus();
            return;
        }

        guardarAsignacion(folio, fecha, hora, lugar, desc, observaciones);
    }
}