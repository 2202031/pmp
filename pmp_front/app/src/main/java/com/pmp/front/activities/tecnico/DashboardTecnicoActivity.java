package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;
import com.pmp.front.activities.NotificacionesActivity;
import com.pmp.front.activities.MiPerfilActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardTecnicoActivity
        extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

    private static final String KEY_NOMBRE =
            "nombre_actual";

    private static final String KEY_ROL =
            "rol_actual";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private View sidebar;
    private View sidebarOverlay;

    private ImageButton btnOpenSidebar;
    private ImageButton btnToggleSidebar;

    private TextView txtInicio;
    private TextView txtMisSupervisiones;
    private TextView txtCalendario;
    private TextView txtNotificaciones;
    private TextView txtMiPerfil;
    private TextView btnLogout;

    private TextView txtNombreTecnico;
    private TextView txtFechaActual;

    private TextView txtCantidadSupervisiones;
    private TextView txtCantidadCalendario;
    private TextView txtCantidadNotificaciones;
    private TextView txtPerfilResumen;

    private TextView txtResumenRecienteTitulo;
    private TextView txtResumenRecienteDetalle;

    private LinearLayout cardMisSupervisiones;
    private LinearLayout cardCalendario;
    private LinearLayout cardNotificaciones;
    private LinearLayout cardPerfil;

    private String usuarioActual = "";
    private String nombreActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_dashboard_tecnico
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        configurarClicks();
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private String datosSupervisionesServidor = "";

    @Override
    protected void onResume() {
        super.onResume();

        if (!usuarioActual.isEmpty()) {
            executorService.execute(() -> {
                com.pmp.front.ServidorSyncHelper.ResultadoSincronizacion resultado =
                        com.pmp.front.ServidorSyncHelper.obtenerSupervisiones("supervisor", usuarioActual);
                datosSupervisionesServidor = resultado.registros;
                mainHandler.post(this::actualizarDashboard);
            });
        }
    }

    private void inicializarVistas() {
        sidebar =
                findViewById(
                        R.id.sidebar
                );

        sidebarOverlay =
                findViewById(
                        R.id.sidebarOverlay
                );

        btnOpenSidebar =
                findViewById(
                        R.id.btnOpenSidebar
                );

        btnToggleSidebar =
                findViewById(
                        R.id.btnToggleSidebar
                );

        txtInicio =
                findViewById(
                        R.id.txtInicio
                );

        txtMisSupervisiones =
                findViewById(
                        R.id.txtMisSupervisiones
                );

        txtCalendario =
                findViewById(
                        R.id.txtCalendario
                );

        txtNotificaciones =
                findViewById(
                        R.id.txtNotificaciones
                );

        txtMiPerfil =
                findViewById(
                        R.id.txtMiPerfil
                );

        btnLogout =
                findViewById(
                        R.id.btnLogout
                );

        txtNombreTecnico =
                findViewById(
                        R.id.txtNombreTecnico
                );

        txtFechaActual =
                findViewById(
                        R.id.txtFechaActual
                );

        txtCantidadSupervisiones =
                findViewById(
                        R.id.txtCantidadSupervisiones
                );

        txtCantidadCalendario =
                findViewById(
                        R.id.txtCantidadCalendario
                );

        txtCantidadNotificaciones =
                findViewById(
                        R.id.txtCantidadNotificaciones
                );

        txtPerfilResumen =
                findViewById(
                        R.id.txtPerfilResumen
                );

        txtResumenRecienteTitulo =
                findViewById(
                        R.id.txtResumenRecienteTitulo
                );

        txtResumenRecienteDetalle =
                findViewById(
                        R.id.txtResumenRecienteDetalle
                );

        cardMisSupervisiones =
                findViewById(
                        R.id.cardMisSupervisiones
                );

        cardCalendario =
                findViewById(
                        R.id.cardCalendario
                );

        cardNotificaciones =
                findViewById(
                        R.id.cardNotificaciones
                );

        cardPerfil =
                findViewById(
                        R.id.cardPerfil
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

        String rolActual =
                valorSeguro(
                        preferences.getString(
                                KEY_ROL,
                                ""
                        )
                );

        if (usuarioActual.isEmpty() ||
                !"supervisor".equalsIgnoreCase(
                        rolActual
                )) {

            regresarLogin();
            return false;
        }

        if (nombreActual.isEmpty()) {
            nombreActual =
                    usuarioActual;
        }

        return true;
    }

    private void configurarClicks() {
        btnOpenSidebar.setOnClickListener(
                v -> abrirSidebar()
        );

        btnToggleSidebar.setOnClickListener(
                v -> cerrarSidebar()
        );

        sidebarOverlay.setOnClickListener(
                v -> cerrarSidebar()
        );

        txtInicio.setOnClickListener(
                v -> cerrarSidebar()
        );

        txtMisSupervisiones.setOnClickListener(v -> {
            cerrarSidebar();
            abrirMisSupervisiones();
        });

        cardMisSupervisiones.setOnClickListener(
                v -> abrirMisSupervisiones()
        );

        txtCalendario.setOnClickListener(v -> {
            cerrarSidebar();
            abrirCalendario();
        });

        cardCalendario.setOnClickListener(
                v -> abrirCalendario()
        );

        txtNotificaciones.setOnClickListener(v -> {
            cerrarSidebar();
            abrirNotificaciones();
        });

        cardNotificaciones.setOnClickListener(
                v -> abrirNotificaciones()
        );

        txtMiPerfil.setOnClickListener(v -> {
            cerrarSidebar();
            abrirMiPerfil();
        });

        cardPerfil.setOnClickListener(
                v -> abrirMiPerfil()
        );

        btnLogout.setOnClickListener(
                v -> cerrarSesion()
        );
    }

    private void actualizarContadorNotificaciones() {
        NotificacionesHelper.obtener(this, usuarioActual, "supervisor", new NotificacionesHelper.NotificacionesCallback() {
            @Override
            public void onSuccess(java.util.List<NotificacionesHelper.Notificacion> notificaciones) {
                int cantidadNotificaciones = 0;
                for (NotificacionesHelper.Notificacion n : notificaciones) {
                    if (!n.leida) cantidadNotificaciones++;
                }

                txtCantidadNotificaciones.setText(cantidadNotificaciones + " sin leer");
                txtNotificaciones.setText("●   Notificaciones      " + cantidadNotificaciones);

                com.pmp.front.BarraNotificacionHelper.mostrarNuevasEnBarra(DashboardTecnicoActivity.this, notificaciones);
            }

            @Override
            public void onError(Exception e) {
                txtCantidadNotificaciones.setText("-- sin leer");
                txtNotificaciones.setText("●   Notificaciones      --");
            }
        });
    }

    private void actualizarDashboard() {
        mostrarInformacionTecnico();

        String supervisiones = datosSupervisionesServidor;

        int cantidadSupervisiones =
                contarSupervisionesAsignadas(
                        supervisiones
                );

        int cantidadProximas =
                contarSupervisionesProximas(
                        supervisiones
                );

        actualizarContadorNotificaciones();

        txtCantidadSupervisiones.setText(
                cantidadSupervisiones +
                        (
                                cantidadSupervisiones == 1
                                        ? " asignada"
                                        : " asignadas"
                        )
        );

        txtCantidadCalendario.setText(
                cantidadProximas +
                        (
                                cantidadProximas == 1
                                        ? " próxima"
                                        : " próximas"
                        )
        );

        txtPerfilResumen.setText(
                nombreActual
        );

        mostrarSupervisionReciente(
                supervisiones
        );
    }

    private void mostrarInformacionTecnico() {
        txtNombreTecnico.setText(
                "Bienvenido, " +
                        nombreActual
        );

        String fecha =
                new SimpleDateFormat(
                        "dd 'de' MMMM 'de' yyyy",
                        new Locale(
                                "es",
                                "MX"
                        )
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        txtFechaActual.setText(
                fecha
        );
    }

    private int contarSupervisionesAsignadas(
            String datos
    ) {
        if (datos == null ||
                datos.trim().isEmpty()) {

            return 0;
        }

        int cantidad =
                0;

        String[] registros =
                datos.split("\n");

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (usuarioActual.equalsIgnoreCase(
                    partes[7].trim()
            )) {
                cantidad++;
            }
        }

        return cantidad;
    }

    private int contarSupervisionesProximas(
            String datos
    ) {
        if (datos == null ||
                datos.trim().isEmpty()) {

            return 0;
        }

        int cantidad =
                0;

        String[] registros =
                datos.split("\n");

        for (String registro :
                registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (!usuarioActual.equalsIgnoreCase(
                    partes[7].trim()
            )) {
                continue;
            }

            if ("Finalizada".equalsIgnoreCase(
                    partes[11].trim()
            )) {
                continue;
            }

            if (esFechaActualOFutura(
                    partes[1]
            )) {
                cantidad++;
            }
        }

        return cantidad;
    }

    private boolean esFechaActualOFutura(
            String fechaTexto
    ) {
        if (fechaTexto == null ||
                fechaTexto.trim().isEmpty()) {

            return false;
        }

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                );

        formato.setLenient(
                false
        );

        try {
            Date fechaSupervision =
                    formato.parse(
                            fechaTexto.trim()
                    );

            String hoyTexto =
                    formato.format(
                            Calendar.getInstance()
                                    .getTime()
                    );

            Date fechaActual =
                    formato.parse(
                            hoyTexto
                    );

            if (fechaSupervision == null ||
                    fechaActual == null) {

                return false;
            }

            return !fechaSupervision.before(
                    fechaActual
            );

        } catch (ParseException exception) {
            return false;
        }
    }

    private void mostrarSupervisionReciente(
            String datos
    ) {
        if (datos == null ||
                datos.trim().isEmpty()) {

            mostrarSinSupervisiones();
            return;
        }

        String[] registros =
                datos.split("\n");

        String[] supervisionReciente =
                null;

        for (int i = registros.length - 1;
             i >= 0;
             i--) {

            String registro =
                    registros[i];

            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (usuarioActual.equalsIgnoreCase(
                    partes[7].trim()
            )) {
                supervisionReciente =
                        partes;

                break;
            }
        }

        if (supervisionReciente == null) {
            mostrarSinSupervisiones();
            return;
        }

        txtResumenRecienteTitulo.setText(
                supervisionReciente[0] +
                        " • " +
                        supervisionReciente[11]
        );

        txtResumenRecienteDetalle.setText(
                "Circuito: " +
                        supervisionReciente[2] +
                        "\n" +

                        "Fecha: " +
                        supervisionReciente[1] +
                        "\n" +

                        "Lugar: " +
                        supervisionReciente[3] +
                        "\n" +

                        "Checklist: " +
                        supervisionReciente[12] +
                        " · Reporte: " +
                        supervisionReciente[13]
        );
    }

    private void mostrarSinSupervisiones() {
        txtResumenRecienteTitulo.setText(
                "Sin supervisiones asignadas"
        );

        txtResumenRecienteDetalle.setText(
                "Cuando el Supervisor te asigne una supervisión aparecerá aquí."
        );
    }

    private void abrirMisSupervisiones() {
        Intent intent =
                new Intent(
                        DashboardTecnicoActivity.this,
                        MisSupervisionesTecnicoActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirCalendario() {
        Intent intent =
                new Intent(
                        DashboardTecnicoActivity.this,
                        CalendarioTecnicoActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirNotificaciones() {
        Intent intent =
                new Intent(
                        DashboardTecnicoActivity.this,
                        NotificacionesActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirMiPerfil() {
        Intent intent =
                new Intent(
                        DashboardTecnicoActivity.this,
                        MiPerfilActivity.class
                );

        startActivity(
                intent
        );
    }

    private void cerrarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        preferences.edit()
                .clear()
                .apply();

        regresarLogin();
    }

    private void regresarLogin() {
        Intent intent =
                new Intent(
                        DashboardTecnicoActivity.this,
                        com.pmp.front.activities.ConfiguracionServidorActivity.class
                );

        intent.putExtra("forzar_config", true);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private void abrirSidebar() {
        sidebar.setVisibility(
                View.VISIBLE
        );

        sidebarOverlay.setVisibility(
                View.VISIBLE
        );

        sidebar.setTranslationX(
                -dpToPx(270)
        );

        sidebar.animate()
                .translationX(0)
                .setDuration(250)
                .start();
    }

    private void cerrarSidebar() {
        if (sidebar.getVisibility() !=
                View.VISIBLE) {

            return;
        }

        sidebar.animate()
                .translationX(
                        -dpToPx(270)
                )
                .setDuration(250)
                .withEndAction(() -> {
                    sidebar.setVisibility(
                            View.GONE
                    );

                    sidebarOverlay.setVisibility(
                            View.GONE
                    );
                })
                .start();
    }

    @Override
    public void onBackPressed() {
        if (sidebar.getVisibility() ==
                View.VISIBLE) {

            cerrarSidebar();
            return;
        }

        super.onBackPressed();
    }

    private String valorSeguro(
            String valor
    ) {
        return valor == null
                ? ""
                : valor.trim();
    }

    private int dpToPx(
            int dp
    ) {
        return (int) (
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}