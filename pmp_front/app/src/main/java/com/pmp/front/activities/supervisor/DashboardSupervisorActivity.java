package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.Config;
import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;
import com.pmp.front.activities.NotificacionesActivity;
import com.pmp.front.activities.MiPerfilActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardSupervisorActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_USUARIO = "usuario_actual"; // Almacena el username único del login

    private static final String PREFS_PERSONAL = "personal_operativo";
    private static final String KEY_TECNICOS = "tecnicos";

    private static final String PREFS_SUPERVISIONES = "supervisiones_local";
    private static final String KEY_SUPERVISIONES = "supervisiones";

    private static final String PREFS_PDF = "reportes_pdf_local";
    private static final String PREFIJO_RUTA_PDF = "ruta_";

    private static final String PREFS_CALENDARIO = "calendario_local";
    private static final String KEY_ACTIVIDADES = "actividades";

    private View sidebar;
    private View sidebarOverlay;

    private ImageButton btnOpenSidebar, btnToggleSidebar;

    private TextView txtInicio, txtReportes, txtUbicacion, txtPerfil, txtPersonal, txtNotificaciones, txtMiPerfil;
    private TextView btnLogout, btnNuevaSupervision;

    private TextView txtCantidadSupervisiones, txtCantidadPersonal, txtCantidadReportesPdf, txtCantidadCalendario;
    private TextView txtResumenRecienteTitulo, txtResumenRecienteDetalle;

    private LinearLayout cardReportes, cardPersonal, cardCalendario, cardVistaPdf;

    // Hilos para consumir las alertas del backend en Spring Boot
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_supervisor);

        inicializarVistas();
        configurarClicks();
    }

    private String datosSupervisionesServidor = "";
    private String datosActividadesServidor = "";
    private int cantidadPersonalServidor = 0;
    private final java.util.Map<String, String> reportesPdfServidor = new java.util.HashMap<>();

    @Override
    protected void onResume() {
        super.onResume();
        executorService.execute(() -> {
            com.pmp.front.ServidorSyncHelper.ResultadoSincronizacion resultado =
                    com.pmp.front.ServidorSyncHelper.obtenerSupervisiones("administrador", null);
            datosSupervisionesServidor = resultado.registros;
            datosActividadesServidor = com.pmp.front.ServidorSyncHelper.obtenerActividades();
            cantidadPersonalServidor = contarSupervisoresDelServidor();
            reportesPdfServidor.clear();
            reportesPdfServidor.putAll(consultarReportesPdfDelServidor());
            mainHandler.post(this::actualizarDashboard);
        });
    }

    private int contarSupervisoresDelServidor() {
        try {
            URL url = new URL(Config.BASE_URL + "/api/usuarios/supervisores");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return new JSONArray(sb.toString()).length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private java.util.Map<String, String> consultarReportesPdfDelServidor() {
        java.util.Map<String, String> mapa = new java.util.HashMap<>();
        try {
            URL url = new URL(Config.BASE_URL + "/api/reportes");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONArray lista = new JSONArray(sb.toString());
                for (int i = 0; i < lista.length(); i++) {
                    JSONObject r = lista.getJSONObject(i);
                    mapa.put(r.optString("folio", ""), r.optString("fechaGeneracion", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapa;
    }

    private void inicializarVistas() {
        sidebar = findViewById(R.id.sidebar);
        sidebarOverlay = findViewById(R.id.sidebarOverlay);
        btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);
        txtInicio = findViewById(R.id.txtInicio);
        txtReportes = findViewById(R.id.txtReportes);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        txtPerfil = findViewById(R.id.txtPerfil);
        txtPersonal = findViewById(R.id.txtPersonal);
        txtNotificaciones = findViewById(R.id.txtNotificaciones);
        txtMiPerfil = findViewById(R.id.txtMiPerfil);
        btnLogout = findViewById(R.id.btnLogout);
        btnNuevaSupervision = findViewById(R.id.btnNuevaSupervision);
        txtCantidadSupervisiones = findViewById(R.id.txtCantidadSupervisiones);
        txtCantidadPersonal = findViewById(R.id.txtCantidadPersonal);
        txtCantidadReportesPdf = findViewById(R.id.txtCantidadReportesPdf);
        txtCantidadCalendario = findViewById(R.id.txtCantidadCalendario);
        txtResumenRecienteTitulo = findViewById(R.id.txtResumenRecienteTitulo);
        txtResumenRecienteDetalle = findViewById(R.id.txtResumenRecienteDetalle);
        cardReportes = findViewById(R.id.cardReportes);
        cardPersonal = findViewById(R.id.cardPersonal);
        cardCalendario = findViewById(R.id.cardCalendario);
        cardVistaPdf = findViewById(R.id.cardVistaPdf);
    }

    private void configurarClicks() {
        btnOpenSidebar.setOnClickListener(v -> openSidebar());
        btnToggleSidebar.setOnClickListener(v -> closeSidebar());
        sidebarOverlay.setOnClickListener(v -> closeSidebar());
        txtInicio.setOnClickListener(v -> closeSidebar());
        btnNuevaSupervision.setOnClickListener(v -> abrirAsignacionSupervision());

        txtReportes.setOnClickListener(v -> {
            closeSidebar();
            abrirListadoSupervisiones();
        });
        cardReportes.setOnClickListener(v -> abrirListadoSupervisiones());

        txtPersonal.setOnClickListener(v -> {
            closeSidebar();
            abrirPersonalOperativo();
        });
        cardPersonal.setOnClickListener(v -> abrirPersonalOperativo());

        txtPerfil.setOnClickListener(v -> {
            closeSidebar();
            abrirListadoReportesPdf();
        });
        cardVistaPdf.setOnClickListener(v -> abrirListadoReportesPdf());

        txtUbicacion.setOnClickListener(v -> {
            closeSidebar();
            abrirCalendario();
        });
        cardCalendario.setOnClickListener(v -> abrirCalendario());

        txtNotificaciones.setOnClickListener(v -> {
            closeSidebar();
            abrirNotificaciones();
        });

        txtMiPerfil.setOnClickListener(v -> {
            closeSidebar();
            abrirMiPerfil();
        });

        btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    private void abrirAsignacionSupervision() {
        startActivity(new Intent(DashboardSupervisorActivity.this, AsignarSupervisionActivity.class));
    }

    private void abrirListadoSupervisiones() {
        startActivity(new Intent(DashboardSupervisorActivity.this, SupervisionesSupervisorActivity.class));
    }

    private void abrirListadoReportesPdf() {
        startActivity(new Intent(DashboardSupervisorActivity.this, ReportesPdfListActivity.class));
    }

    private void abrirPersonalOperativo() {
        startActivity(new Intent(DashboardSupervisorActivity.this, PersonalOperativoActivity.class));
    }

    private void abrirCalendario() {
        startActivity(new Intent(DashboardSupervisorActivity.this, CalendarioSupervisorActivity.class));
    }

    private void abrirNotificaciones() {
        startActivity(new Intent(DashboardSupervisorActivity.this, NotificacionesActivity.class));
    }

    private void abrirMiPerfil() {
        startActivity(new Intent(DashboardSupervisorActivity.this, MiPerfilActivity.class));
    }

    private void cerrarSesion() {
        // Se borran los datos de sesión guardados en el dispositivo.
        // (Antes solo se cambiaba de pantalla y la sesión quedaba almacenada.)
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(DashboardSupervisorActivity.this, com.pmp.front.activities.ConfiguracionServidorActivity.class);
        intent.putExtra("forzar_config", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void actualizarDashboard() {
        actualizarCantidadPersonal();

        String supervisionesGuardadas = datosSupervisionesServidor;

        actualizarCantidadSupervisiones(supervisionesGuardadas);
        actualizarCantidadReportesPdf(supervisionesGuardadas);
        actualizarCantidadCalendario();
        mostrarSupervisionReciente(supervisionesGuardadas);

        // Consulta remota de notificaciones HTTP
        actualizarCantidadNotificaciones();
    }

    private void actualizarCantidadPersonal() {
        int cantidadPersonal = cantidadPersonalServidor;
        txtCantidadPersonal.setText(cantidadPersonal + (cantidadPersonal == 1 ? " registro" : " registros"));
    }

    private void actualizarCantidadSupervisiones(String datos) {
        int cantidadSupervisiones = contarRegistros(datos);
        txtCantidadSupervisiones.setText(cantidadSupervisiones + (cantidadSupervisiones == 1 ? " registrada" : " registradas"));
    }

    private void actualizarCantidadReportesPdf(String datos) {
        int disponibles = 0;
        int generados = 0;

        if (datos != null && !datos.trim().isEmpty()) {
            String[] registros = datos.split("\n");
            for (String registro : registros) {
                if (registro.trim().isEmpty()) continue;
                String[] partes = registro.split("\\|", -1);
                if (partes.length < 15) continue;

                if (esReporteDisponible(partes)) {
                    disponibles++;
                    if (existePdf(partes[0])) {
                        generados++;
                    }
                }
            }
        }
        txtCantidadReportesPdf.setText(disponibles + (disponibles == 1 ? " disponible" : " disponibles") + " · " + generados + (generados == 1 ? " generado" : " generados"));
    }

    private void actualizarCantidadCalendario() {
        String datos = datosActividadesServidor;
        int pendientes = 0;

        if (datos != null && !datos.trim().isEmpty()) {
            String[] registros = datos.split("\n");
            for (String registro : registros) {
                if (registro.trim().isEmpty()) continue;
                String[] partes = registro.split("\\|", -1);
                if (partes.length < 10) continue;
                if ("Pendiente".equalsIgnoreCase(partes[8].trim())) {
                    pendientes++;
                }
            }
        }
        txtCantidadCalendario.setText(pendientes + (pendientes == 1 ? " pendiente" : " pendientes"));
    }

    private void actualizarCantidadNotificaciones() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        String usuario = preferences.getString(KEY_USUARIO, "").trim();

        if (usuario.isEmpty()) {
            txtNotificaciones.setText("◌   Notificaciones      0");
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(Config.BASE_URL + "/api/notificaciones?username=" + usuario + "&rol=administrador");
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
                    int noLeidas = 0;

                    java.util.List<NotificacionesHelper.Notificacion> lista = new java.util.ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        if (!obj.optBoolean("leida", false)) {
                            noLeidas++;
                        }

                        NotificacionesHelper.Notificacion n = new NotificacionesHelper.Notificacion();
                        n.id = String.valueOf(obj.optInt("id", 0));
                        n.titulo = obj.optString("titulo", "");
                        n.mensaje = obj.optString("mensaje", "");
                        n.folio = obj.optString("folio", "");
                        n.leida = obj.optBoolean("leida", false);
                        lista.add(n);
                    }

                    final int finalNoLeidas = noLeidas;
                    mainHandler.post(() -> {
                        txtNotificaciones.setText("◌   Notificaciones      " + finalNoLeidas);
                        com.pmp.front.BarraNotificacionHelper.mostrarNuevasEnBarra(DashboardSupervisorActivity.this, lista);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> txtNotificaciones.setText("◌   Notificaciones      --"));
            }
        });
    }

    private boolean esReporteDisponible(String[] partes) {
        return "Finalizada".equalsIgnoreCase(partes[11].trim()) &&
                "Completado".equalsIgnoreCase(partes[12].trim()) &&
                "Validado".equalsIgnoreCase(partes[13].trim());
    }

    private boolean existePdf(String folio) {
        return folio != null && reportesPdfServidor.containsKey(folio);
    }

    private int contarRegistros(String datos) {
        if (datos == null || datos.trim().isEmpty()) return 0;
        int cantidad = 0;
        String[] registros = datos.split("\n");
        for (String r : registros) {
            if (!r.trim().isEmpty()) cantidad++;
        }
        return cantidad;
    }

    private void mostrarSupervisionReciente(String datos) {
        if (datos == null || datos.trim().isEmpty()) {
            txtResumenRecienteTitulo.setText("Sin supervisiones registradas");
            txtResumenRecienteDetalle.setText("Las supervisiones asignadas aparecerán aquí.");
            return;
        }

        String[] registros = datos.split("\n");
        String ultimoRegistro = "";
        for (int i = registros.length - 1; i >= 0; i--) {
            if (!registros[i].trim().isEmpty()) {
                ultimoRegistro = registros[i];
                break;
            }
        }

        String[] partes = ultimoRegistro.split("\\|", -1);
        if (partes.length < 15) {
            txtResumenRecienteTitulo.setText("Supervisión registrada");
            txtResumenRecienteDetalle.setText("Consulta el listado para ver la información.");
            return;
        }

        txtResumenRecienteTitulo.setText(partes[0] + " • " + partes[11]);
        txtResumenRecienteDetalle.setText(partes[2] + " · Responsable: " + partes[8] + " · Checklist: " + partes[12] + " · Reporte: " + partes[13]);
    }

    private void openSidebar() {
        sidebar.setVisibility(View.VISIBLE);
        sidebarOverlay.setVisibility(View.VISIBLE);
        sidebar.setTranslationX(-dpToPx(270));
        sidebar.animate().translationX(0).setDuration(250).start();
    }

    private void closeSidebar() {
        if (sidebar.getVisibility() != View.VISIBLE) return;
        sidebar.animate().translationX(-dpToPx(270)).setDuration(250).withEndAction(() -> {
            sidebar.setVisibility(View.GONE);
            sidebarOverlay.setVisibility(View.GONE);
        }).start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}