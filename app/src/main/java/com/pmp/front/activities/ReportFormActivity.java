package com.pmp.front.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;

public class ReportFormActivity extends Activity {

    private TextView btnGuardarReporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_form);

        btnGuardarReporte = findViewById(R.id.btnGuardarReporte);

        btnGuardarReporte.setOnClickListener(v -> {
            Toast.makeText(this, "Reporte guardado de forma simulada", Toast.LENGTH_SHORT).show();
        });
    }
}