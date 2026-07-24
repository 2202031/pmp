package com.pmp.front.activities.supervisor;

import com.pmp.front.Config;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pmp.front.activities.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportesPdfListActivity extends Activity {

    private static final String PREFS_SESION = "sesion_usuario";
    private static final String KEY_ROL = "rol_actual";

    // URL base que apunta al controlador de reportes de tu API en Spring Boot
    private static String baseUrl() { return Config.BASE_URL + "/api/reportes"; }

    private LinearLayout contenedor;
    private TextView txtVacio;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!sesionValida()) {
            return;
        }

        setContentView(construirVista());
        cargarReportes();
    }

    private boolean sesionValida() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESION, MODE_PRIVATE);
        String rol = preferences.getString(KEY_ROL, "");

        if (!"administrador".equalsIgnoreCase(rol)) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }

    // CORREGIDO: Reestructuración de la jerarquía de vistas dinámicas con ScrollView
    private View construirVista() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(16), dp(24), dp(16), dp(16));

        TextView btnVolver = new TextView(this);
        btnVolver.setText("← Volver");
        btnVolver.setTextSize(16);
        btnVolver.setTextColor(Color.rgb(0, 99, 65));
        btnVolver.setTypeface(null, Typeface.BOLD);
        btnVolver.setPadding(0, 0, 0, dp(16));
        btnVolver.setOnClickListener(v -> finish());
        raiz.addView(btnVolver);

        TextView titulo = new TextView(this);
        titulo.setText("Reportes PDF");
        titulo.setTextSize(22);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.rgb(0, 99, 65));
        titulo.setPadding(0, 0, 0, dp(4));
        raiz.addView(titulo);

        TextView subtitulo = new TextView(this);
        subtitulo.setText("Reportes ya emitidos por los supervisores. Toca uno para descargarlo.");
        subtitulo.setTextSize(13);
        subtitulo.setTextColor(Color.DKGRAY);
        subtitulo.setPadding(0, 0, 0, dp(16));
        raiz.addView(subtitulo);

        txtVacio = new TextView(this);
        txtVacio.setText("Aún no hay reportes PDF subidos.");
        txtVacio.setTextColor(Color.GRAY);
        txtVacio.setTextSize(14);
        txtVacio.setGravity(Gravity.CENTER);
        txtVacio.setPadding(0, dp(32), 0, 0);
        txtVacio.setVisibility(View.GONE);
        raiz.addView(txtVacio);

        contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(contenedor);

        scroll.addView(raiz);
        return scroll;
    }

    private void cargarReportes() {
        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(6000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONArray lista = new JSONArray(sb.toString());
                    mainHandler.post(() -> pintarLista(lista));
                } else {
                    mainHandler.post(() -> Toast.makeText(this, "No se pudieron cargar los reportes del servidor", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void pintarLista(JSONArray lista) {
        contenedor.removeAllViews();

        if (lista.length() == 0) {
            txtVacio.setVisibility(View.VISIBLE);
            return;
        }
        txtVacio.setVisibility(View.GONE);

        for (int i = 0; i < lista.length(); i++) {
            JSONObject item = lista.optJSONObject(i);
            if (item == null) continue;

            int id = item.optInt("id", -1);
            String folio = item.optString("folio", "0");
            String nombreArchivo = item.optString("nombreArchivo", "reporte.pdf");
            String supervisor = item.optString("usernameSupervisor", "N/D");
            String fecha = item.optString("fechaGeneracion", "");

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.VERTICAL);
            fila.setPadding(dp(12), dp(12), dp(12), dp(12));
            fila.setBackgroundColor(Color.rgb(245, 247, 245));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(10));
            fila.setLayoutParams(params);

            TextView txtFolio = new TextView(this);
            txtFolio.setText("Folio: " + folio + " · " + nombreArchivo);
            txtFolio.setTypeface(null, Typeface.BOLD);
            txtFolio.setTextColor(Color.rgb(17, 24, 39));
            txtFolio.setTextSize(14);
            fila.addView(txtFolio);

            TextView txtDetalle = new TextView(this);
            txtDetalle.setText("Supervisor: " + supervisor + "  ·  " + fecha);
            txtDetalle.setTextSize(11);
            txtDetalle.setTextColor(Color.rgb(107, 114, 128));
            txtDetalle.setPadding(0, dp(4), 0, dp(6));
            fila.addView(txtDetalle);

            TextView txtAccion = new TextView(this);
            txtAccion.setText("Toca para descargar y abrir ⬇");
            txtAccion.setTextSize(11);
            txtAccion.setTypeface(null, Typeface.BOLD);
            txtAccion.setTextColor(Color.rgb(0, 99, 65));
            txtAccion.setGravity(Gravity.END);
            fila.addView(txtAccion);

            fila.setOnClickListener(v -> descargarYAbrir(id, nombreArchivo));

            contenedor.addView(fila);
        }
    }

    private void descargarYAbrir(int idReporte, String nombreArchivo) {
        Toast.makeText(this, "Descargando reporte...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl() + "/" + idReporte + "/descargar");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> Toast.makeText(this, "No se pudo obtener el archivo del servidor", Toast.LENGTH_SHORT).show());
                    return;
                }

                File carpeta = new File(getFilesDir(), "reportes");
                if (!carpeta.exists()) {
                    carpeta.mkdirs();
                }
                File archivo = new File(carpeta, nombreArchivo);

                // Transferencia segura de flujo de datos binario (Stream)
                try (InputStream is = connection.getInputStream();
                     FileOutputStream fos = new FileOutputStream(archivo)) {
                    byte[] buffer = new byte[4096];
                    int leidos;
                    while ((leidos = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, leidos);
                    }
                    fos.flush();
                }

                mainHandler.post(() -> abrirPdf(archivo));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Error al procesar la descarga", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void abrirPdf(File archivo) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Abrir reporte PDF"));

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay una aplicación instalada para visualizar PDFs", Toast.LENGTH_SHORT).show();
        }
    }

    // Utilidad interna para conversión de píxeles independientes (DP)
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}