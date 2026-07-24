package com.pmp.front.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.R;
import com.pmp.front.activities.supervisor.RevisionSupervisionSupervisorActivity;
import com.pmp.front.activities.tecnico.DetalleSupervisionTecnicoActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificacionesActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_USUARIO = "usuario_actual";
    private static final String KEY_ROL = "rol_actual";

    private TextView btnVolver;
    private TextView btnMarcarTodas;
    private TextView txtTituloNotificaciones;
    private TextView txtResumenNotificaciones;
    private LinearLayout emptyStateNotificaciones;
    private LinearLayout containerNotificaciones;

    private String usuarioActual = "";
    private String rolActual = "";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static class RemoteNotif {
        int id;
        String titulo;
        String mensaje;
        String fecha;
        String folio;
        boolean leida;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);
        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarTitulo();
        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!usuarioActual.isEmpty() && !rolActual.isEmpty()) {
            cargarNotificacionesDesdeServidor();
        }
    }

    private void inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver);
        btnMarcarTodas = findViewById(R.id.btnMarcarTodas);
        txtTituloNotificaciones = findViewById(R.id.txtTituloNotificaciones);
        txtResumenNotificaciones = findViewById(R.id.txtResumenNotificaciones);
        emptyStateNotificaciones = findViewById(R.id.emptyStateNotificaciones);
        containerNotificaciones = findViewById(R.id.containerNotificaciones);
    }

    private boolean cargarSesion() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        usuarioActual = valorSeguro(preferences.getString(KEY_USUARIO, ""));
        rolActual = valorSeguro(preferences.getString(KEY_ROL, ""));
        return !usuarioActual.isEmpty();
    }

    private void configurarTitulo() {
        // CORREGIDO: Soporte visual adecuado para administradores y supervisores
        if ("administrador".equalsIgnoreCase(rolActual)) {
            txtTituloNotificaciones.setText("Notificaciones de la Red");
        } else if ("supervisor".equalsIgnoreCase(rolActual)) {
            txtTituloNotificaciones.setText("Panel de Supervisión");
        } else {
            txtTituloNotificaciones.setText("Mis notificaciones");
        }
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v -> finish());
        btnMarcarTodas.setOnClickListener(v -> confirmarMarcarTodas());
    }

    private void cargarNotificacionesDesdeServidor() {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Config.BASE_URL + "/api/notificaciones?username=" + usuarioActual + "&rol=" + rolActual);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder responseStr = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line.trim());
                    }

                    JSONArray array = new JSONArray(responseStr.toString());
                    List<RemoteNotif> lista = new ArrayList<>();
                    int noLeidas = 0;

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        RemoteNotif notif = new RemoteNotif();
                        notif.id = obj.getInt("id");
                        notif.titulo = obj.getString("titulo");
                        notif.mensaje = obj.getString("mensaje");
                        notif.fecha = obj.getString("fechaFormateada");
                        notif.folio = obj.optString("folio", "");
                        notif.leida = obj.getBoolean("leida");

                        lista.add(notif);
                        if (!notif.leida) noLeidas++;
                    }

                    final int countNoLeidas = noLeidas;
                    mainHandler.post(() -> actualizarUI(lista, countNoLeidas));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void actualizarUI(List<RemoteNotif> notificaciones, int noLeidas) {
        containerNotificaciones.removeAllViews();

        txtResumenNotificaciones.setText(notificaciones.size() +
                (notificaciones.size() == 1 ? " notificación" : " notificaciones") + " · " + noLeidas + " sin leer");

        btnMarcarTodas.setEnabled(noLeidas > 0);
        btnMarcarTodas.setAlpha(noLeidas > 0 ? 1f : 0.55f);

        if (notificaciones.isEmpty()) {
            emptyStateNotificaciones.setVisibility(View.VISIBLE);
            containerNotificaciones.setVisibility(View.GONE);
            return;
        }

        emptyStateNotificaciones.setVisibility(View.GONE);
        containerNotificaciones.setVisibility(View.VISIBLE);

        for (RemoteNotif notificacion : notificaciones) {
            containerNotificaciones.addView(crearTarjetaNotificacion(notificacion));
        }
    }

    private View crearTarjetaNotificacion(RemoteNotif notificacion) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dp(18), dp(16), dp(18), dp(16));
        tarjeta.setElevation(dp(4));

        LinearLayout.LayoutParams tarjetaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tarjetaParams.setMargins(0, 0, 0, dp(14));
        tarjeta.setLayoutParams(tarjetaParams);
        tarjeta.setBackground(crearFondoTarjeta(notificacion.leida));

        LinearLayout encabezado = new LinearLayout(this);
        encabezado.setOrientation(LinearLayout.HORIZONTAL);
        encabezado.setGravity(Gravity.CENTER_VERTICAL);

        TextView indicador = new TextView(this);
        indicador.setText(notificacion.leida ? "✓" : "●");
        indicador.setTextSize(notificacion.leida ? 15 : 13);
        indicador.setTextColor(notificacion.leida ? Color.parseColor("#6B7280") : Color.parseColor("#E30613"));
        indicador.setGravity(Gravity.CENTER);
        indicador.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout informacion = new LinearLayout(this);
        informacion.setOrientation(LinearLayout.VERTICAL);
        informacion.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView txtTitulo = new TextView(this);
        txtTitulo.setText(valorVisible(notificacion.titulo, "Notificación"));
        txtTitulo.setTextColor(Color.parseColor("#111827"));
        txtTitulo.setTextSize(15);
        txtTitulo.setTypeface(Typeface.DEFAULT, notificacion.leida ? Typeface.NORMAL : Typeface.BOLD);

        TextView txtFecha = new TextView(this);
        txtFecha.setText(notificacion.fecha);
        txtFecha.setTextColor(Color.parseColor("#6B7280"));
        txtFecha.setTextSize(11);

        informacion.addView(txtTitulo);
        informacion.addView(txtFecha);
        encabezado.addView(indicador);
        encabezado.addView(informacion);
        tarjeta.addView(encabezado);

        TextView txtMensaje = new TextView(this);
        txtMensaje.setText(valorVisible(notificacion.mensaje, "Sin descripción"));
        txtMensaje.setTextColor(Color.parseColor("#374151"));
        txtMensaje.setTextSize(13);

        LinearLayout.LayoutParams mensajeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mensajeParams.setMargins(dp(34), dp(10), 0, 0);
        txtMensaje.setLayoutParams(mensajeParams);
        tarjeta.addView(txtMensaje);

        if (!notificacion.folio.isEmpty()) {
            TextView txtFolio = new TextView(this);
            txtFolio.setText("Folio relacionado: " + notificacion.folio);
            txtFolio.setTextColor(Color.parseColor("#006341"));
            txtFolio.setTextSize(12);
            txtFolio.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

            LinearLayout.LayoutParams folioParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            folioParams.setMargins(dp(34), dp(10), 0, 0);
            txtFolio.setLayoutParams(folioParams);
            tarjeta.addView(txtFolio);
        }

        LinearLayout acciones = new LinearLayout(this);
        acciones.setOrientation(LinearLayout.HORIZONTAL);
        acciones.setGravity(Gravity.END);
        LinearLayout.LayoutParams accionesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        accionesParams.setMargins(0, dp(14), 0, 0);
        acciones.setLayoutParams(accionesParams);

        TextView btnEliminar = crearBotonSecundario("Eliminar");
        btnEliminar.setTextColor(Color.parseColor("#B91C1C"));
        btnEliminar.setOnClickListener(v -> confirmarEliminar(notificacion));
        acciones.addView(btnEliminar);

        if (!notificacion.folio.isEmpty()) {
            TextView btnAbrir = crearBotonPrincipal("Abrir supervisión");
            btnAbrir.setOnClickListener(v -> ejecutarMarcadoYLanzar(notificacion));
            acciones.addView(btnAbrir);
        }

        tarjeta.addView(acciones);
        tarjeta.setClickable(true);
        tarjeta.setFocusable(true);
        tarjeta.setOnClickListener(v -> ejecutarMarcadoYLanzar(notificacion));

        return tarjeta;
    }

    private void ejecutarMarcadoYLanzar(RemoteNotif notificacion) {
        if (!notificacion.leida) {
            executorService.execute(() -> {
                try {
                    URL url = new URL(Config.BASE_URL + "/api/notificaciones/" + notificacion.id + "/leer");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        mainHandler.post(() -> abrirRelacionado(notificacion.folio));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            abrirRelacionado(notificacion.folio);
        }
    }

    private void abrirRelacionado(String folio) {
        if (folio == null || folio.trim().isEmpty()) {
            cargarNotificacionesDesdeServidor();
            return;
        }

        // Mapeo de roles (importante):
        //  - "administrador" -> pantallas de la carpeta /supervisor (revisión, valida y ve el PDF)
        //  - "supervisor"    -> pantallas de la carpeta /tecnico (detalle de SU supervisión)
        Intent intent;
        if ("administrador".equalsIgnoreCase(rolActual)) {
            intent = new Intent(this, RevisionSupervisionSupervisorActivity.class);
        } else {
            intent = new Intent(this, DetalleSupervisionTecnicoActivity.class);
        }

        intent.putExtra("folio", folio);
        startActivity(intent);
    }

    private void confirmarMarcarTodas() {
        new AlertDialog.Builder(this)
                .setTitle("Marcar como leídas")
                .setMessage("¿Deseas marcar todas tus notificaciones como leídas?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Marcar todas", (dialog, which) -> executorService.execute(() -> {
                    try {
                        URL url = new URL(Config.BASE_URL + "/api/notificaciones/leer-todas?username=" + usuarioActual + "&rol=" + rolActual);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PUT");
                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            mainHandler.post(() -> {
                                cargarNotificacionesDesdeServidor();
                                Toast.makeText(NotificacionesActivity.this, "Notificaciones actualizadas", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).show();
    }

    private void confirmarEliminar(RemoteNotif notificacion) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar notificación")
                .setMessage("¿Deseas eliminar esta notificación?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> executorService.execute(() -> {
                    try {
                        URL url = new URL(Config.BASE_URL + "/api/notificaciones/" + notificacion.id);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            mainHandler.post(this::cargarNotificacionesDesdeServidor);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).show();
    }

    private TextView crearBotonPrincipal(String texto) {
        TextView boton = new TextView(this);
        boton.setText(texto);
        boton.setGravity(Gravity.CENTER);
        boton.setTextColor(Color.WHITE);
        boton.setTextSize(11);
        boton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        boton.setBackgroundResource(R.drawable.bg_button_login);
        boton.setClickable(true);
        boton.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
        params.setMargins(dp(8), 0, 0, 0);
        boton.setPadding(dp(14), 0, dp(14), 0);
        boton.setLayoutParams(params);
        return boton;
    }

    private TextView crearBotonSecundario(String texto) {
        TextView boton = new TextView(this);
        boton.setText(texto);
        boton.setGravity(Gravity.CENTER);
        boton.setTextSize(11);
        boton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        boton.setBackgroundResource(R.drawable.bg_input_login);
        boton.setClickable(true);
        boton.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
        boton.setPadding(dp(14), 0, dp(14), 0);
        boton.setLayoutParams(params);
        return boton;
    }

    private GradientDrawable crearFondoTarjeta(boolean leida) {
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(dp(14));

        if (leida) {
            fondo.setColor(Color.WHITE);
            fondo.setStroke(dp(1), Color.parseColor("#DDE7E1"));
        } else {
            fondo.setColor(Color.parseColor("#ECFDF5"));
            fondo.setStroke(dp(2), Color.parseColor("#006341"));
        }
        return fondo;
    }

    private String valorVisible(String valor, String alternativo) {
        return (valor == null || valor.trim().isEmpty()) ? alternativo : valor.trim();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}