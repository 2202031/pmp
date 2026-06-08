package com.pmp.front.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pmp.front.R;

public class DashboardActivity extends Activity {

    private LinearLayout sidebar;

    private ImageButton btnToggleSidebar;

    private TextView txtLogo;
    private TextView txtInicio;
    private TextView txtReportes;
    private TextView txtUbicacion;
    private TextView txtPerfil;
    private TextView btnLogout;

    private boolean sidebarOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sidebar = findViewById(R.id.sidebar);

        btnToggleSidebar = findViewById(R.id.btnToggleSidebar);

        txtLogo = findViewById(R.id.txtLogo);
        txtInicio = findViewById(R.id.txtInicio);
        txtReportes = findViewById(R.id.txtReportes);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        txtPerfil = findViewById(R.id.txtPerfil);

        txtReportes.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ReportFormActivity.class);
            startActivity(intent);
        });

        btnLogout = findViewById(R.id.btnLogout);

        btnToggleSidebar.setOnClickListener(v -> toggleSidebar());

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void toggleSidebar() {

        int startWidth = sidebar.getWidth();
        int endWidth = sidebarOpen ? dpToPx(65) : dpToPx(150);

        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);

        animator.setDuration(250);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {

            int animatedValue = (int) animation.getAnimatedValue();

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) sidebar.getLayoutParams();

            params.width = animatedValue;

            sidebar.setLayoutParams(params);
        });

        animator.start();

        if (sidebarOpen) {

            txtLogo.setVisibility(View.GONE);

            txtInicio.setText("I");
            txtReportes.setText("R");
            txtUbicacion.setText("U");
            txtPerfil.setText("P");

            btnLogout.setText("S");

            sidebarOpen = false;

        } else {

            txtLogo.setVisibility(View.VISIBLE);

            txtInicio.setText("Inicio");
            txtReportes.setText("Reportes");
            txtUbicacion.setText("Ubicación");
            txtPerfil.setText("Perfil");

            btnLogout.setText("Salir");

            sidebarOpen = true;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}