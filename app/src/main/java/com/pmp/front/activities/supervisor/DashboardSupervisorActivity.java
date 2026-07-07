package com.pmp.front.activities.supervisor;

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

import java.io.File;

public class DashboardSupervisorActivity
        extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

    private static final String PREFS_PERSONAL =
            "personal_operativo";

    private static final String KEY_TECNICOS =
            "tecnicos";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_PDF =
            "reportes_pdf_local";

    private static final String PREFIJO_RUTA_PDF =
            "ruta_";

    private static final String PREFS_CALENDARIO =
            "calendario_local";

    private static final String KEY_ACTIVIDADES =
            "actividades";

    private View sidebar;
    private View sidebarOverlay;

    private ImageButton btnOpenSidebar;
    private ImageButton btnToggleSidebar;

    private TextView txtInicio;
    private TextView txtReportes;
    private TextView txtUbicacion;
    private TextView txtPerfil;
    private TextView txtPersonal;
    private TextView txtNotificaciones;
    private TextView txtMiPerfil;

    private TextView btnLogout;
    private TextView btnNuevaSupervision;

    private TextView txtCantidadSupervisiones;
    private TextView txtCantidadPersonal;
    private TextView txtCantidadReportesPdf;
    private TextView txtCantidadCalendario;

    private TextView txtResumenRecienteTitulo;
    private TextView txtResumenRecienteDetalle;

    private LinearLayout cardReportes;
    private LinearLayout cardPersonal;
    private LinearLayout cardCalendario;
    private LinearLayout cardVistaPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_dashboard_supervisor
        );

        inicializarVistas();
        configurarClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        actualizarDashboard();
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

        txtReportes =
                findViewById(
                        R.id.txtReportes
                );

        txtUbicacion =
                findViewById(
                        R.id.txtUbicacion
                );

        txtPerfil =
                findViewById(
                        R.id.txtPerfil
                );

        txtPersonal =
                findViewById(
                        R.id.txtPersonal
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

        btnNuevaSupervision =
                findViewById(
                        R.id.btnNuevaSupervision
                );

        txtCantidadSupervisiones =
                findViewById(
                        R.id.txtCantidadSupervisiones
                );

        txtCantidadPersonal =
                findViewById(
                        R.id.txtCantidadPersonal
                );

        txtCantidadReportesPdf =
                findViewById(
                        R.id.txtCantidadReportesPdf
                );

        txtCantidadCalendario =
                findViewById(
                        R.id.txtCantidadCalendario
                );

        txtResumenRecienteTitulo =
                findViewById(
                        R.id.txtResumenRecienteTitulo
                );

        txtResumenRecienteDetalle =
                findViewById(
                        R.id.txtResumenRecienteDetalle
                );

        cardReportes =
                findViewById(
                        R.id.cardReportes
                );

        cardPersonal =
                findViewById(
                        R.id.cardPersonal
                );

        cardCalendario =
                findViewById(
                        R.id.cardCalendario
                );

        cardVistaPdf =
                findViewById(
                        R.id.cardVistaPdf
                );
    }

    private void configurarClicks() {
        btnOpenSidebar.setOnClickListener(
                v -> openSidebar()
        );

        btnToggleSidebar.setOnClickListener(
                v -> closeSidebar()
        );

        sidebarOverlay.setOnClickListener(
                v -> closeSidebar()
        );

        txtInicio.setOnClickListener(
                v -> closeSidebar()
        );

        btnNuevaSupervision.setOnClickListener(
                v -> abrirAsignacionSupervision()
        );

        txtReportes.setOnClickListener(v -> {
            closeSidebar();
            abrirListadoSupervisiones();
        });

        cardReportes.setOnClickListener(
                v -> abrirListadoSupervisiones()
        );

        txtPersonal.setOnClickListener(v -> {
            closeSidebar();
            abrirPersonalOperativo();
        });

        cardPersonal.setOnClickListener(
                v -> abrirPersonalOperativo()
        );

        txtPerfil.setOnClickListener(v -> {
            closeSidebar();
            abrirListadoReportesPdf();
        });

        cardVistaPdf.setOnClickListener(
                v -> abrirListadoReportesPdf()
        );

        txtUbicacion.setOnClickListener(v -> {
            closeSidebar();
            abrirCalendario();
        });

        cardCalendario.setOnClickListener(
                v -> abrirCalendario()
        );

        txtNotificaciones.setOnClickListener(v -> {
            closeSidebar();
            abrirNotificaciones();
        });

        txtMiPerfil.setOnClickListener(v -> {
            closeSidebar();
            abrirMiPerfil();
        });

        btnLogout.setOnClickListener(
                v -> cerrarSesion()
        );
    }

    private void abrirAsignacionSupervision() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        AsignarSupervisionActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirListadoSupervisiones() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        SupervisionesSupervisorActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirListadoReportesPdf() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        SupervisionesSupervisorActivity.class
                );

        intent.putExtra(
                "modo",
                "reportes"
        );

        startActivity(
                intent
        );
    }

    private void abrirPersonalOperativo() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        PersonalOperativoActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirCalendario() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        CalendarioSupervisorActivity.class
                );

        startActivity(
                intent
        );
    }

    private void abrirNotificaciones() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        NotificacionesActivity.class
                );

        startActivity(
                intent
        );
    }
    private void abrirMiPerfil() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        MiPerfilActivity.class
                );

        startActivity(
                intent
        );
    }

    private void cerrarSesion() {
        Intent intent =
                new Intent(
                        DashboardSupervisorActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private void actualizarDashboard() {
        actualizarCantidadPersonal();

        SharedPreferences supervisionPreferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String supervisionesGuardadas =
                supervisionPreferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        actualizarCantidadSupervisiones(
                supervisionesGuardadas
        );

        actualizarCantidadReportesPdf(
                supervisionesGuardadas
        );

        actualizarCantidadCalendario();

        actualizarCantidadNotificaciones();

        mostrarSupervisionReciente(
                supervisionesGuardadas
        );
    }

    private void actualizarCantidadPersonal() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PERSONAL,
                        MODE_PRIVATE
                );

        String personalGuardado =
                preferences.getString(
                        KEY_TECNICOS,
                        ""
                );

        int cantidadPersonal =
                contarRegistros(
                        personalGuardado
                );

        txtCantidadPersonal.setText(
                cantidadPersonal +
                        (
                                cantidadPersonal == 1
                                        ? " registro"
                                        : " registros"
                        )
        );
    }

    private void actualizarCantidadSupervisiones(
            String datos
    ) {
        int cantidadSupervisiones =
                contarRegistros(
                        datos
                );

        txtCantidadSupervisiones.setText(
                cantidadSupervisiones +
                        (
                                cantidadSupervisiones == 1
                                        ? " registrada"
                                        : " registradas"
                        )
        );
    }

    private void actualizarCantidadReportesPdf(
            String datos
    ) {
        int disponibles =
                0;

        int generados =
                0;

        if (datos != null &&
                !datos.trim().isEmpty()) {

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

                if (!esReporteDisponible(
                        partes
                )) {
                    continue;
                }

                disponibles++;

                if (existePdf(
                        partes[0]
                )) {
                    generados++;
                }
            }
        }

        String textoDisponibles =
                disponibles +
                        (
                                disponibles == 1
                                        ? " disponible"
                                        : " disponibles"
                        );

        String textoGenerados =
                generados +
                        (
                                generados == 1
                                        ? " generado"
                                        : " generados"
                        );

        txtCantidadReportesPdf.setText(
                textoDisponibles +
                        " · " +
                        textoGenerados
        );
    }

    private void actualizarCantidadCalendario() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_CALENDARIO,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_ACTIVIDADES,
                        ""
                );

        int pendientes =
                0;

        if (datos != null &&
                !datos.trim().isEmpty()) {

            String[] registros =
                    datos.split("\n");

            for (String registro :
                    registros) {

                if (registro.trim().isEmpty()) {
                    continue;
                }

                String[] partes =
                        registro.split("\\|", -1);

                if (partes.length < 10) {
                    continue;
                }

                if ("Pendiente".equalsIgnoreCase(
                        partes[8].trim()
                )) {
                    pendientes++;
                }
            }
        }

        txtCantidadCalendario.setText(
                pendientes +
                        (
                                pendientes == 1
                                        ? " pendiente"
                                        : " pendientes"
                        )
        );
    }

    private void actualizarCantidadNotificaciones() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        String usuario =
                preferences.getString(
                        KEY_USUARIO,
                        ""
                );

        if (usuario == null) {
            usuario =
                    "";
        }

        int noLeidas =
                NotificacionesHelper.contarNoLeidas(
                        this,
                        usuario.trim(),
                        "supervisor"
                );

        txtNotificaciones.setText(
                "◌   Notificaciones      " +
                        noLeidas
        );
    }

    private boolean esReporteDisponible(
            String[] partes
    ) {
        return "Finalizada".equalsIgnoreCase(
                partes[11].trim()
        ) &&
                "Completado".equalsIgnoreCase(
                        partes[12].trim()
                ) &&
                "Validado".equalsIgnoreCase(
                        partes[13].trim()
                );
    }

    private boolean existePdf(
            String folio
    ) {
        if (folio == null ||
                folio.trim().isEmpty()) {

            return false;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PDF,
                        MODE_PRIVATE
                );

        String rutaGuardada =
                preferences.getString(
                        PREFIJO_RUTA_PDF +
                                folio,
                        ""
                );

        if (rutaGuardada != null &&
                !rutaGuardada.trim().isEmpty()) {

            File archivoGuardado =
                    new File(
                            rutaGuardada.trim()
                    );

            if (archivoGuardado.exists()) {
                return true;
            }
        }

        String folioLimpio =
                folio.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        File directorio =
                new File(
                        getFilesDir(),
                        "reportes"
                );

        File archivoEsperado =
                new File(
                        directorio,
                        "Reporte_Supervision_" +
                                folioLimpio +
                                ".pdf"
                );

        return archivoEsperado.exists();
    }

    private int contarRegistros(
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

            if (!registro.trim().isEmpty()) {
                cantidad++;
            }
        }

        return cantidad;
    }

    private void mostrarSupervisionReciente(
            String datos
    ) {
        if (datos == null ||
                datos.trim().isEmpty()) {

            txtResumenRecienteTitulo.setText(
                    "Sin supervisiones registradas"
            );

            txtResumenRecienteDetalle.setText(
                    "Las supervisiones asignadas aparecerán aquí."
            );

            return;
        }

        String[] registros =
                datos.split("\n");

        String ultimoRegistro =
                "";

        for (int i = registros.length - 1;
             i >= 0;
             i--) {

            if (!registros[i]
                    .trim()
                    .isEmpty()) {

                ultimoRegistro =
                        registros[i];

                break;
            }
        }

        String[] partes =
                ultimoRegistro.split(
                        "\\|",
                        -1
                );

        if (partes.length < 15) {
            txtResumenRecienteTitulo.setText(
                    "Supervisión registrada"
            );

            txtResumenRecienteDetalle.setText(
                    "Consulta el listado para ver la información."
            );

            return;
        }

        String folio =
                partes[0];

        String circuito =
                partes[2];

        String responsable =
                partes[8];

        String estado =
                partes[11];

        String checklist =
                partes[12];

        String reporte =
                partes[13];

        txtResumenRecienteTitulo.setText(
                folio +
                        " • " +
                        estado
        );

        txtResumenRecienteDetalle.setText(
                circuito +
                        " · Responsable: " +
                        responsable +
                        " · Checklist: " +
                        checklist +
                        " · Reporte: " +
                        reporte
        );
    }

    private void openSidebar() {
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

    private void closeSidebar() {
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