package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pmp.front.R;

public class PersonalOperativoActivity extends Activity {

    private LinearLayout contenedorPersonal;
    private TextView txtContador, emptyState, btnVolver, btnAgregarTecnico;
    private EditText etBuscarTecnico;

    private static final String PREFS = "personal_operativo";
    private static final String KEY_TECNICOS = "tecnicos";

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
                cargarTecnicos(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarTecnicos(etBuscarTecnico.getText().toString());
    }

    private void cargarTecnicos(String filtro) {
        contenedorPersonal.removeAllViews();

        String data = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_TECNICOS, "");

        if (data.trim().isEmpty()) {
            txtContador.setText("0 técnicos encontrados");
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        String[] registros = data.split("\n");
        int encontrados = 0;

        for (int i = 0; i < registros.length; i++) {
            String[] partes = registros[i].split("\\|");

            if (partes.length < 6) continue;

            String nombre = partes[0];
            String correo = partes[1];
            String telefono = partes[2];
            String zona = partes[3];
            String usuario = partes[4];

            String busqueda = (nombre + correo + telefono + zona + usuario).toLowerCase();

            if (!busqueda.contains(filtro.toLowerCase())) continue;

            encontrados++;
            contenedorPersonal.addView(
                    crearCardTecnico(i, nombre, correo, telefono, zona, usuario)
            );
        }

        txtContador.setText(encontrados + " técnicos encontrados");
        emptyState.setVisibility(encontrados == 0 ? View.VISIBLE : View.GONE);
    }

    private View crearCardTecnico(
            int index,
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario
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

        btnEditar.setOnClickListener(v -> editarTecnico(index, nombre, correo, telefono, zona, usuario));

        btnEliminar.setOnClickListener(v -> confirmarEliminar(index, nombre));

        acciones.addView(btnVer);
        acciones.addView(btnEditar);
        acciones.addView(btnEliminar);

        card.addView(filaSuperior);
        card.addView(linea);
        card.addView(acciones);

        return card;
    }

    private void editarTecnico(
            int index,
            String nombre,
            String correo,
            String telefono,
            String zona,
            String usuario
    ) {
        String password = obtenerPasswordPorIndex(index);

        Intent intent = new Intent(this, RegistrarTecnicoActivity.class);
        intent.putExtra("editIndex", index);
        intent.putExtra("nombre", nombre);
        intent.putExtra("correo", correo);
        intent.putExtra("telefono", telefono);
        intent.putExtra("zona", zona);
        intent.putExtra("usuario", usuario);
        intent.putExtra("password", password);
        startActivity(intent);
    }

    private String obtenerPasswordPorIndex(int index) {
        String data = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_TECNICOS, "");

        if (data.trim().isEmpty()) return "";

        String[] registros = data.split("\n");

        if (index < 0 || index >= registros.length) return "";

        String[] partes = registros[index].split("\\|");

        if (partes.length < 6) return "";

        return partes[5];
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

    private void confirmarEliminar(int index, String nombre) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar técnico")
                .setMessage("¿Deseas eliminar a " + nombre + "?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTecnico(index))
                .show();
    }

    private void eliminarTecnico(int indexEliminar) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String data = prefs.getString(KEY_TECNICOS, "");

        if (data.trim().isEmpty()) return;

        String[] registros = data.split("\n");
        StringBuilder nuevoData = new StringBuilder();

        for (int i = 0; i < registros.length; i++) {
            if (i == indexEliminar) continue;

            if (nuevoData.length() > 0) nuevoData.append("\n");
            nuevoData.append(registros[i]);
        }

        prefs.edit().putString(KEY_TECNICOS, nuevoData.toString()).apply();
        cargarTecnicos(etBuscarTecnico.getText().toString());
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