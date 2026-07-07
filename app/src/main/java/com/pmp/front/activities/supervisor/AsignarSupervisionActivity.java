package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.NotificacionesHelper;
import com.pmp.front.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AsignarSupervisionActivity extends Activity {

    private static final String PREFS_PERSONAL =
            "personal_operativo";

    private static final String KEY_TECNICOS =
            "tecnicos";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_HORARIOS_SUPERVISION =
            "horarios_supervision_local";

    private static final String PREFIJO_HORA_SUPERVISION =
            "hora_";

    private TextView btnVolver;
    private TextView btnGuardarAsignacion;
    private TextView btnQuitarHoraProgramada;
    private TextView txtPersonalApoyo;

    private EditText etFolio;
    private EditText etFecha;
    private EditText etHoraProgramada;
    private EditText etLugar;
    private EditText etDescripcion;
    private EditText etObservaciones;

    private Spinner spCircuito;
    private Spinner spPrioridad;
    private Spinner spTecnicoResponsable;

    private final List<String> nombresTecnicos =
            new ArrayList<>();

    private final List<String> usuariosTecnicos =
            new ArrayList<>();

    private final Set<String> apoyosSeleccionados =
            new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_asignar_supervision
        );

        inicializarVistas();
        configurarCircuitos();
        configurarPrioridades();
        cargarPersonalOperativo();
        configurarEventos();
        establecerFechaActual();

        etHoraProgramada.setText("");
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnGuardarAsignacion =
                findViewById(
                        R.id.btnGuardarAsignacion
                );

        btnQuitarHoraProgramada =
                findViewById(
                        R.id.btnQuitarHoraProgramada
                );

        txtPersonalApoyo =
                findViewById(
                        R.id.txtPersonalApoyo
                );

        etFolio =
                findViewById(R.id.etFolio);

        etFecha =
                findViewById(R.id.etFecha);

        etHoraProgramada =
                findViewById(
                        R.id.etHoraProgramada
                );

        etLugar =
                findViewById(R.id.etLugar);

        etDescripcion =
                findViewById(
                        R.id.etDescripcion
                );

        etObservaciones =
                findViewById(
                        R.id.etObservaciones
                );

        spCircuito =
                findViewById(R.id.spCircuito);

        spPrioridad =
                findViewById(R.id.spPrioridad);

        spTecnicoResponsable =
                findViewById(
                        R.id.spTecnicoResponsable
                );
    }

    private void configurarCircuitos() {
        String[] circuitos = {
                "Selecciona un circuito",
                "Circuito 1",
                "Circuito 2",
                "Circuito 3",
                "Circuito 4",
                "Circuito 5",
                "Circuito 6"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        circuitos
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spCircuito.setAdapter(adapter);
    }

    private void configurarPrioridades() {
        String[] prioridades = {
                "Selecciona una prioridad",
                "Baja",
                "Media",
                "Alta",
                "Urgente"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        prioridades
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spPrioridad.setAdapter(adapter);
    }

    private void cargarPersonalOperativo() {
        nombresTecnicos.clear();
        usuariosTecnicos.clear();

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_PERSONAL,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_TECNICOS,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            configurarSpinnerSinTecnicos();
            return;
        }

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            /*
             * Formato del personal operativo:
             *
             * 0 nombre
             * 1 correo
             * 2 teléfono
             * 3 zona o área
             * 4 usuario
             * 5 contraseña
             */

            if (partes.length < 6) {
                continue;
            }

            String nombre =
                    partes[0].trim();

            String usuario =
                    partes[4].trim();

            if (nombre.isEmpty() ||
                    usuario.isEmpty()) {

                continue;
            }

            nombresTecnicos.add(nombre);
            usuariosTecnicos.add(usuario);
        }

        configurarSpinnerTecnicos();
    }

    private void configurarSpinnerSinTecnicos() {
        List<String> opciones =
                new ArrayList<>();

        opciones.add(
                "No hay personal operativo registrado"
        );

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        opciones
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spTecnicoResponsable.setAdapter(
                adapter
        );

        spTecnicoResponsable.setEnabled(
                false
        );
    }

    private void configurarSpinnerTecnicos() {
        List<String> opciones =
                new ArrayList<>();

        opciones.add(
                "Selecciona un técnico responsable"
        );

        for (int i = 0;
             i < nombresTecnicos.size();
             i++) {

            String nombre =
                    nombresTecnicos.get(i);

            String usuario =
                    usuariosTecnicos.get(i);

            opciones.add(
                    nombre +
                            " • " +
                            usuario
            );
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        opciones
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spTecnicoResponsable.setAdapter(
                adapter
        );

        spTecnicoResponsable.setEnabled(
                !nombresTecnicos.isEmpty()
        );
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        etFecha.setOnClickListener(
                v -> mostrarSelectorFecha()
        );

        etHoraProgramada.setOnClickListener(
                v -> mostrarSelectorHora()
        );

        btnQuitarHoraProgramada.setOnClickListener(
                v -> etHoraProgramada.setText("")
        );

        spTecnicoResponsable
                .setOnItemSelectedListener(
                        new AdapterView
                                .OnItemSelectedListener() {

                            @Override
                            public void onItemSelected(
                                    AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id
                            ) {
                                quitarResponsableDeApoyos();
                            }

                            @Override
                            public void onNothingSelected(
                                    AdapterView<?> parent
                            ) {
                                // No requiere acción.
                            }
                        }
                );

        txtPersonalApoyo.setOnClickListener(
                v -> mostrarSelectorApoyos()
        );

        btnGuardarAsignacion.setOnClickListener(
                v -> validarYGuardar()
        );
    }

    private void establecerFechaActual() {
        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                );

        etFecha.setText(
                formato.format(
                        Calendar.getInstance()
                                .getTime()
                )
        );
    }

    private void mostrarSelectorFecha() {
        Calendar calendario =
                Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            String fecha =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            dayOfMonth,
                                            month + 1,
                                            year
                                    );

                            etFecha.setText(fecha);
                        },
                        calendario.get(
                                Calendar.YEAR
                        ),
                        calendario.get(
                                Calendar.MONTH
                        ),
                        calendario.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }

    private void mostrarSelectorHora() {
        Calendar calendario =
                Calendar.getInstance();

        String horaActual =
                normalizarHora(
                        etHoraProgramada
                                .getText()
                                .toString()
                );

        if (!horaActual.isEmpty()) {
            String[] partes =
                    horaActual.split(":");

            calendario.set(
                    Calendar.HOUR_OF_DAY,
                    Integer.parseInt(partes[0])
            );

            calendario.set(
                    Calendar.MINUTE,
                    Integer.parseInt(partes[1])
            );
        }

        TimePickerDialog dialog =
                new TimePickerDialog(
                        this,
                        (view, hourOfDay, minute) -> {
                            String hora =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            hourOfDay,
                                            minute
                                    );

                            etHoraProgramada.setText(
                                    hora
                            );
                        },
                        calendario.get(
                                Calendar.HOUR_OF_DAY
                        ),
                        calendario.get(
                                Calendar.MINUTE
                        ),
                        true
                );

        dialog.show();
    }

    private void mostrarSelectorApoyos() {
        if (nombresTecnicos.isEmpty()) {
            Toast.makeText(
                    this,
                    "No hay personal operativo registrado",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (spTecnicoResponsable
                .getSelectedItemPosition() <= 0) {

            Toast.makeText(
                    this,
                    "Selecciona primero al técnico responsable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String usuarioResponsable =
                obtenerUsuarioResponsable();

        List<String> nombresDisponibles =
                new ArrayList<>();

        List<String> usuariosDisponibles =
                new ArrayList<>();

        for (int i = 0;
             i < nombresTecnicos.size();
             i++) {

            String usuario =
                    usuariosTecnicos.get(i);

            if (usuario.equalsIgnoreCase(
                    usuarioResponsable
            )) {
                continue;
            }

            nombresDisponibles.add(
                    nombresTecnicos.get(i)
            );

            usuariosDisponibles.add(
                    usuario
            );
        }

        if (nombresDisponibles.isEmpty()) {
            Toast.makeText(
                    this,
                    "No hay otro técnico disponible como apoyo",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] opciones =
                nombresDisponibles.toArray(
                        new String[0]
                );

        boolean[] seleccionados =
                new boolean[opciones.length];

        Set<String> seleccionTemporal =
                new LinkedHashSet<>(
                        apoyosSeleccionados
                );

        for (int i = 0;
             i < usuariosDisponibles.size();
             i++) {

            seleccionados[i] =
                    apoyosSeleccionados.contains(
                            usuariosDisponibles.get(i)
                    );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Personal de apoyo"
                )
                .setMultiChoiceItems(
                        opciones,
                        seleccionados,
                        (dialog, which, isChecked) -> {
                            String usuario =
                                    usuariosDisponibles
                                            .get(which);

                            if (isChecked) {
                                seleccionTemporal.add(
                                        usuario
                                );
                            } else {
                                seleccionTemporal.remove(
                                        usuario
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Aceptar",
                        (dialog, which) -> {
                            apoyosSeleccionados.clear();

                            apoyosSeleccionados.addAll(
                                    seleccionTemporal
                            );

                            actualizarTextoApoyos();
                        }
                )
                .show();
    }

    private void quitarResponsableDeApoyos() {
        String usuarioResponsable =
                obtenerUsuarioResponsable();

        if (usuarioResponsable.isEmpty()) {
            return;
        }

        apoyosSeleccionados.remove(
                usuarioResponsable
        );

        actualizarTextoApoyos();
    }

    private String obtenerUsuarioResponsable() {
        int posicion =
                spTecnicoResponsable
                        .getSelectedItemPosition();

        if (posicion <= 0) {
            return "";
        }

        int indice =
                posicion - 1;

        if (indice < 0 ||
                indice >= usuariosTecnicos.size()) {

            return "";
        }

        return usuariosTecnicos.get(
                indice
        );
    }

    private String obtenerNombreResponsable() {
        int posicion =
                spTecnicoResponsable
                        .getSelectedItemPosition();

        if (posicion <= 0) {
            return "";
        }

        int indice =
                posicion - 1;

        if (indice < 0 ||
                indice >= nombresTecnicos.size()) {

            return "";
        }

        return nombresTecnicos.get(
                indice
        );
    }

    private void actualizarTextoApoyos() {
        if (apoyosSeleccionados.isEmpty()) {
            txtPersonalApoyo.setText(
                    "Seleccionar personal de apoyo (opcional)"
            );

            return;
        }

        List<String> nombresSeleccionados =
                new ArrayList<>();

        for (String usuario :
                apoyosSeleccionados) {

            String nombre =
                    obtenerNombrePorUsuario(
                            usuario
                    );

            if (!nombre.isEmpty()) {
                nombresSeleccionados.add(
                        nombre
                );
            }
        }

        if (nombresSeleccionados.isEmpty()) {
            txtPersonalApoyo.setText(
                    "Seleccionar personal de apoyo (opcional)"
            );

            return;
        }

        txtPersonalApoyo.setText(
                TextUtils.join(
                        ", ",
                        nombresSeleccionados
                )
        );
    }

    private String obtenerNombrePorUsuario(
            String usuarioBuscado
    ) {
        for (int i = 0;
             i < usuariosTecnicos.size();
             i++) {

            if (usuariosTecnicos
                    .get(i)
                    .equalsIgnoreCase(
                            usuarioBuscado
                    )) {

                return nombresTecnicos.get(i);
            }
        }

        return "";
    }

    private void validarYGuardar() {
        String folio =
                limpiar(
                        etFolio.getText()
                                .toString()
                );

        String fecha =
                limpiar(
                        etFecha.getText()
                                .toString()
                );

        String horaIngresada =
                limpiar(
                        etHoraProgramada
                                .getText()
                                .toString()
                );

        String horaProgramada =
                normalizarHora(
                        horaIngresada
                );

        String lugar =
                limpiar(
                        etLugar.getText()
                                .toString()
                );

        String descripcion =
                limpiar(
                        etDescripcion.getText()
                                .toString()
                );

        String observaciones =
                limpiar(
                        etObservaciones.getText()
                                .toString()
                );

        if (folio.isEmpty()) {
            etFolio.setError(
                    "Ingresa el folio"
            );

            etFolio.requestFocus();
            return;
        }

        if (folio.length() < 3) {
            etFolio.setError(
                    "El folio es demasiado corto"
            );

            etFolio.requestFocus();
            return;
        }

        if (folioDuplicado(folio)) {
            etFolio.setError(
                    "Ya existe una supervisión con este folio"
            );

            etFolio.requestFocus();
            return;
        }

        if (fecha.isEmpty()) {
            etFecha.setError(
                    "Selecciona la fecha"
            );

            return;
        }

        if (!horaIngresada.isEmpty() &&
                horaProgramada.isEmpty()) {

            etHoraProgramada.setError(
                    "Selecciona una hora válida"
            );

            return;
        }

        if (spCircuito
                .getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Selecciona un circuito",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (lugar.isEmpty()) {
            etLugar.setError(
                    "Ingresa el lugar o referencia"
            );

            etLugar.requestFocus();
            return;
        }

        if (spPrioridad
                .getSelectedItemPosition() == 0) {

            Toast.makeText(
                    this,
                    "Selecciona una prioridad",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (nombresTecnicos.isEmpty()) {
            Toast.makeText(
                    this,
                    "Primero registra personal operativo",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (spTecnicoResponsable
                .getSelectedItemPosition() <= 0) {

            Toast.makeText(
                    this,
                    "Selecciona al técnico responsable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (descripcion.isEmpty()) {
            etDescripcion.setError(
                    "Ingresa la descripción"
            );

            etDescripcion.requestFocus();
            return;
        }

        guardarAsignacion(
                folio,
                fecha,
                horaProgramada,
                lugar,
                descripcion,
                observaciones
        );
    }

    private void guardarAsignacion(
            String folio,
            String fecha,
            String horaProgramada,
            String lugar,
            String descripcion,
            String observaciones
    ) {
        String circuito =
                spCircuito.getSelectedItem()
                        .toString();

        String prioridad =
                spPrioridad.getSelectedItem()
                        .toString();

        String usuarioResponsable =
                obtenerUsuarioResponsable();

        String nombreResponsable =
                obtenerNombreResponsable();

        String usuariosApoyo =
                TextUtils.join(
                        ",",
                        apoyosSeleccionados
                );

        List<String> nombresApoyo =
                new ArrayList<>();

        for (String usuario :
                apoyosSeleccionados) {

            String nombre =
                    obtenerNombrePorUsuario(
                            usuario
                    );

            if (!nombre.isEmpty()) {
                nombresApoyo.add(nombre);
            }
        }

        String personalApoyo =
                TextUtils.join(
                        ",",
                        nombresApoyo
                );

        String estadoGeneral =
                "Asignada";

        String estadoChecklist =
                "Pendiente";

        String estadoReporte =
                "Bloqueado";

        String fechaRegistro =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        /*
         * Estructura estable de la supervisión:
         *
         * 0  folio
         * 1  fecha de supervisión
         * 2  circuito
         * 3  lugar
         * 4  prioridad
         * 5  descripción
         * 6  observaciones
         * 7  usuario responsable
         * 8  nombre responsable
         * 9  usuarios de apoyo
         * 10 nombres de apoyo
         * 11 estado general
         * 12 estado checklist
         * 13 estado reporte
         * 14 fecha y hora de registro
         */

        String nuevaSupervision =
                folio + "|" +
                        fecha + "|" +
                        circuito + "|" +
                        lugar + "|" +
                        prioridad + "|" +
                        descripcion + "|" +
                        observaciones + "|" +
                        usuarioResponsable + "|" +
                        nombreResponsable + "|" +
                        usuariosApoyo + "|" +
                        personalApoyo + "|" +
                        estadoGeneral + "|" +
                        estadoChecklist + "|" +
                        estadoReporte + "|" +
                        fechaRegistro;

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String registrosAnteriores =
                preferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        String registrosActualizados;

        if (registrosAnteriores == null ||
                registrosAnteriores
                        .trim()
                        .isEmpty()) {

            registrosActualizados =
                    nuevaSupervision;

        } else {
            registrosActualizados =
                    registrosAnteriores.trim() +
                            "\n" +
                            nuevaSupervision;
        }

        preferences.edit()
                .putString(
                        KEY_SUPERVISIONES,
                        registrosActualizados
                )
                .apply();

        guardarHoraProgramada(
                folio,
                horaProgramada
        );

        crearNotificacionAsignacion(
                usuarioResponsable,
                folio,
                circuito,
                fecha,
                horaProgramada
        );

        String detalleHora;

        if (!horaProgramada.isEmpty()) {
            detalleHora =
                    "\nHora programada: " +
                            horaProgramada;

        } else if (fechaEsHoy(fecha)) {
            detalleHora =
                    "\nHora: se utilizará la hora de asignación";

        } else {
            detalleHora =
                    "\nHora: sin hora programada";
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Asignación registrada"
                )
                .setMessage(
                        "La supervisión " +
                                folio +
                                " fue asignada a " +
                                nombreResponsable +
                                ".\n\n" +
                                "Circuito: " +
                                circuito +
                                "\nFecha: " +
                                fecha +
                                detalleHora +
                                "\nEstado: Asignada" +
                                "\nChecklist: Pendiente" +
                                "\nReporte técnico: Bloqueado"
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Aceptar",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void crearNotificacionAsignacion(
            String usuarioResponsable,
            String folio,
            String circuito,
            String fecha,
            String horaProgramada
    ) {
        if (usuarioResponsable == null ||
                usuarioResponsable
                        .trim()
                        .isEmpty()) {

            return;
        }

        String textoHora =
                horaProgramada.isEmpty()
                        ? ""
                        : " a las " +
                        horaProgramada;

        NotificacionesHelper.crear(
                this,
                usuarioResponsable,
                "tecnico",
                "Nueva supervisión asignada",
                "Se te asignó la supervisión " +
                        folio +
                        " del " +
                        circuito +
                        " para el " +
                        fecha +
                        textoHora +
                        ".",
                "ASIGNACION",
                folio
        );
    }

    private void guardarHoraProgramada(
            String folio,
            String horaProgramada
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_HORARIOS_SUPERVISION,
                        MODE_PRIVATE
                );

        String clave =
                PREFIJO_HORA_SUPERVISION +
                        folio;

        SharedPreferences.Editor editor =
                preferences.edit();

        if (horaProgramada == null ||
                horaProgramada.trim().isEmpty()) {

            editor.remove(clave);
        } else {
            editor.putString(
                    clave,
                    horaProgramada.trim()
            );
        }

        editor.apply();
    }

    private boolean fechaEsHoy(
            String fecha
    ) {
        if (fecha == null ||
                fecha.trim().isEmpty()) {

            return false;
        }

        String hoy =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        return hoy.equals(
                fecha.trim()
        );
    }

    private String normalizarHora(
            String hora
    ) {
        if (hora == null ||
                hora.trim().isEmpty()) {

            return "";
        }

        String[] partes =
                hora.trim().split(":");

        if (partes.length != 2) {
            return "";
        }

        try {
            int horas =
                    Integer.parseInt(
                            partes[0]
                    );

            int minutos =
                    Integer.parseInt(
                            partes[1]
                    );

            if (horas < 0 ||
                    horas > 23 ||
                    minutos < 0 ||
                    minutos > 59) {

                return "";
            }

            return String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    horas,
                    minutos
            );

        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private boolean folioDuplicado(
            String folioBuscado
    ) {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

            return false;
        }

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length == 0) {
                continue;
            }

            String folioGuardado =
                    partes[0].trim();

            if (folioBuscado.equalsIgnoreCase(
                    folioGuardado
            )) {
                return true;
            }
        }

        return false;
    }

    private String limpiar(
            String texto
    ) {
        if (texto == null) {
            return "";
        }

        return texto
                .trim()
                .replace("|", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}