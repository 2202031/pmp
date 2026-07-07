package com.pmp.front.activities.supervisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarioSupervisorActivity extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_ROL =
            "rol_actual";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_HORARIOS_SUPERVISION =
            "horarios_supervision_local";

    private static final String PREFIJO_HORA_SUPERVISION =
            "hora_";

    private static final String PREFS_CALENDARIO =
            "calendario_local";

    private static final String KEY_ACTIVIDADES =
            "actividades";

    private static final Locale LOCALE_MEXICO =
            new Locale("es", "MX");

    private ScrollView scrollCalendario;

    private TextView btnVolver;
    private TextView btnMesAnterior;
    private TextView btnMesSiguiente;
    private TextView btnHoy;

    private TextView txtMesAnio;
    private TextView txtResumenMes;
    private TextView txtFechaSeleccionada;
    private TextView txtResumenDia;

    private TextView btnAgregarActividad;

    private GridLayout gridCalendario;

    private LinearLayout emptyStateDia;
    private LinearLayout containerEventosDia;

    private final Calendar mesVisible =
            Calendar.getInstance();

    private final Calendar fechaSeleccionada =
            Calendar.getInstance();

    private final List<EventoCalendario> todosEventos =
            new ArrayList<>();

    private final List<String> opcionesSupervisiones =
            new ArrayList<>();

    private final List<String> foliosSupervisiones =
            new ArrayList<>();

    private final List<String> tecnicosSupervisiones =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_calendario_supervisor
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        prepararFechas();
        configurarEventos();
        recargarCalendario();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gridCalendario != null) {
            recargarCalendario();
        }
    }

    private void inicializarVistas() {
        scrollCalendario =
                findViewById(
                        R.id.scrollCalendario
                );

        btnVolver =
                findViewById(
                        R.id.btnVolver
                );

        btnMesAnterior =
                findViewById(
                        R.id.btnMesAnterior
                );

        btnMesSiguiente =
                findViewById(
                        R.id.btnMesSiguiente
                );

        btnHoy =
                findViewById(
                        R.id.btnHoy
                );

        txtMesAnio =
                findViewById(
                        R.id.txtMesAnio
                );

        txtResumenMes =
                findViewById(
                        R.id.txtResumenMes
                );

        txtFechaSeleccionada =
                findViewById(
                        R.id.txtFechaSeleccionada
                );

        txtResumenDia =
                findViewById(
                        R.id.txtResumenDia
                );

        btnAgregarActividad =
                findViewById(
                        R.id.btnAgregarActividad
                );

        gridCalendario =
                findViewById(
                        R.id.gridCalendario
                );

        emptyStateDia =
                findViewById(
                        R.id.emptyStateDia
                );

        containerEventosDia =
                findViewById(
                        R.id.containerEventosDia
                );
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        String rol =
                preferences.getString(
                        KEY_ROL,
                        ""
                );

        if (!"supervisor".equalsIgnoreCase(rol)) {
            Intent intent = new Intent(
                    this,
                    LoginActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return false;
        }

        return true;
    }

    private void prepararFechas() {
        limpiarHora(mesVisible);
        limpiarHora(fechaSeleccionada);

        mesVisible.set(
                Calendar.DAY_OF_MONTH,
                1
        );
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(
                v -> finish()
        );

        btnMesAnterior.setOnClickListener(
                v -> cambiarMes(-1)
        );

        btnMesSiguiente.setOnClickListener(
                v -> cambiarMes(1)
        );

        btnHoy.setOnClickListener(v -> {
            Calendar hoy =
                    Calendar.getInstance();

            limpiarHora(hoy);

            mesVisible.setTimeInMillis(
                    hoy.getTimeInMillis()
            );

            mesVisible.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            fechaSeleccionada.setTimeInMillis(
                    hoy.getTimeInMillis()
            );

            renderizarCalendario();
        });

        btnAgregarActividad.setOnClickListener(
                v -> mostrarDialogoActividad(null)
        );
    }

    private void cambiarMes(
            int cantidadMeses
    ) {
        int diaSeleccionado =
                fechaSeleccionada.get(
                        Calendar.DAY_OF_MONTH
                );

        mesVisible.add(
                Calendar.MONTH,
                cantidadMeses
        );

        mesVisible.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        Calendar nuevaSeleccion =
                (Calendar) mesVisible.clone();

        int ultimoDia =
                nuevaSeleccion.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        nuevaSeleccion.set(
                Calendar.DAY_OF_MONTH,
                Math.min(
                        diaSeleccionado,
                        ultimoDia
                )
        );

        limpiarHora(nuevaSeleccion);

        fechaSeleccionada.setTimeInMillis(
                nuevaSeleccion.getTimeInMillis()
        );

        renderizarCalendario();
    }

    private void recargarCalendario() {
        todosEventos.clear();

        opcionesSupervisiones.clear();
        foliosSupervisiones.clear();
        tecnicosSupervisiones.clear();

        opcionesSupervisiones.add(
                "Sin supervisión vinculada"
        );

        foliosSupervisiones.add("");
        tecnicosSupervisiones.add("");

        cargarSupervisiones();
        cargarActividades();

        renderizarCalendario();
    }

    private void cargarSupervisiones() {
        SharedPreferences supervisionPreferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        SharedPreferences horarioPreferences =
                getSharedPreferences(
                        PREFS_HORARIOS_SUPERVISION,
                        MODE_PRIVATE
                );

        String datos =
                supervisionPreferences.getString(
                        KEY_SUPERVISIONES,
                        ""
                );

        if (datos == null ||
                datos.trim().isEmpty()) {

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

            if (partes.length < 15) {
                continue;
            }

            String folio =
                    partes[0].trim();

            String fechaSupervision =
                    normalizarFecha(
                            partes[1]
                    );

            String circuito =
                    partes[2].trim();

            String lugar =
                    partes[3].trim();

            String prioridad =
                    partes[4].trim();

            String tecnico =
                    partes[8].trim();

            String estado =
                    partes[11].trim();

            String fechaHoraRegistro =
                    partes[14].trim();

            Calendar registroAsignacion =
                    parsearFechaRegistro(
                            fechaHoraRegistro
                    );

            /*
             * La hora programada se conserva fuera
             * del registro principal de 15 campos.
             */
            String horaProgramada =
                    normalizarHora(
                            horarioPreferences.getString(
                                    PREFIJO_HORA_SUPERVISION +
                                            folio,
                                    ""
                            )
                    );

            /*
             * Compatibilidad con cualquier registro
             * temporal que ya tuviera un campo 16.
             */
            if (horaProgramada.isEmpty() &&
                    partes.length > 15) {

                horaProgramada =
                        normalizarHora(
                                partes[15]
                        );
            }

            /*
             * Si no hay una fecha válida, se recupera
             * la fecha automática de asignación.
             */
            if (fechaSupervision.isEmpty() &&
                    registroAsignacion != null) {

                fechaSupervision =
                        formatoFecha(
                                registroAsignacion
                        );
            }

            Calendar fechaSupervisionCalendar =
                    parsearFecha(
                            fechaSupervision
                    );

            boolean fechaEsDiaDeAsignacion =
                    registroAsignacion != null &&
                            fechaSupervisionCalendar != null &&
                            mismoDia(
                                    registroAsignacion,
                                    fechaSupervisionCalendar
                            );

            String horaSupervision;
            String origenHora;

            /*
             * Prioridad:
             *
             * 1. Hora programada.
             * 2. Hora automática, solamente cuando
             *    la fecha de supervisión coincide con
             *    el día en que fue asignada.
             * 3. Sin hora programada.
             */
            if (!horaProgramada.isEmpty()) {
                horaSupervision =
                        horaProgramada;

                origenHora =
                        "Hora programada";

            } else if (
                    fechaEsDiaDeAsignacion &&
                            registroAsignacion != null &&
                            registroContieneHora(
                                    fechaHoraRegistro
                            )
            ) {
                horaSupervision =
                        formatoHora(
                                registroAsignacion
                        );

                origenHora =
                        "Hora automática de asignación";

            } else {
                horaSupervision = "";

                origenHora =
                        "Sin hora programada";
            }

            opcionesSupervisiones.add(
                    folio +
                            " • " +
                            circuito +
                            " • " +
                            valorVisible(
                                    fechaSupervision,
                                    "Sin fecha"
                            )
            );

            foliosSupervisiones.add(
                    folio
            );

            tecnicosSupervisiones.add(
                    tecnico
            );

            if (fechaSupervision.isEmpty()) {
                continue;
            }

            EventoCalendario evento =
                    new EventoCalendario();

            evento.tipo =
                    "Supervisión";

            evento.id =
                    folio;

            evento.titulo =
                    "Supervisión " +
                            folio;

            evento.fecha =
                    fechaSupervision;

            evento.hora =
                    horaSupervision;

            evento.origenHora =
                    origenHora;

            evento.descripcion =
                    "Circuito: " +
                            valorVisible(
                                    circuito,
                                    "Sin circuito"
                            ) +
                            "\nLugar: " +
                            valorVisible(
                                    lugar,
                                    "Sin lugar"
                            );

            evento.prioridad =
                    prioridad;

            evento.folio =
                    folio;

            evento.tecnico =
                    tecnico;

            evento.estado =
                    estado;

            evento.esSupervision =
                    true;

            todosEventos.add(evento);
        }
    }

    private void cargarActividades() {
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

        if (datos == null ||
                datos.trim().isEmpty()) {

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

            if (partes.length < 10) {
                continue;
            }

            String fecha =
                    normalizarFecha(
                            partes[2]
                    );

            if (fecha.isEmpty()) {
                continue;
            }

            EventoCalendario evento =
                    new EventoCalendario();

            evento.tipo =
                    "Actividad";

            evento.id =
                    partes[0];

            evento.titulo =
                    partes[1];

            evento.fecha =
                    fecha;

            evento.hora =
                    normalizarHora(
                            partes[3]
                    );

            evento.origenHora =
                    "Hora de actividad";

            evento.descripcion =
                    partes[4];

            evento.prioridad =
                    partes[5];

            evento.folio =
                    partes[6];

            evento.tecnico =
                    partes[7];

            evento.estado =
                    partes[8];

            evento.fechaRegistro =
                    partes[9];

            evento.esSupervision =
                    false;

            todosEventos.add(evento);
        }
    }

    private void renderizarCalendario() {
        mostrarTituloMes();

        gridCalendario.removeAllViews();

        Calendar primerDia =
                (Calendar) mesVisible.clone();

        primerDia.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        limpiarHora(primerDia);

        int diaSemana =
                primerDia.get(
                        Calendar.DAY_OF_WEEK
                );

        /*
         * Calendar usa domingo = 1 y lunes = 2.
         * Se convierte para comenzar en lunes.
         */
        int desplazamiento =
                (diaSemana + 5) % 7;

        Calendar fechaCelda =
                (Calendar) primerDia.clone();

        fechaCelda.add(
                Calendar.DAY_OF_MONTH,
                -desplazamiento
        );

        for (int posicion = 0;
             posicion < 42;
             posicion++) {

            Calendar fecha =
                    (Calendar) fechaCelda.clone();

            View celda =
                    crearCeldaDia(
                            fecha,
                            posicion
                    );

            gridCalendario.addView(celda);

            fechaCelda.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );
        }

        actualizarResumenMes();
        mostrarEventosDiaSeleccionado();
    }

    private void mostrarTituloMes() {
        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        LOCALE_MEXICO
                );

        String titulo =
                formato.format(
                        mesVisible.getTime()
                );

        txtMesAnio.setText(
                primeraLetraMayuscula(
                        titulo
                )
        );
    }

    private View crearCeldaDia(
            Calendar fecha,
            int posicion
    ) {
        boolean perteneceMes =
                fecha.get(Calendar.MONTH) ==
                        mesVisible.get(Calendar.MONTH) &&

                        fecha.get(Calendar.YEAR) ==
                                mesVisible.get(Calendar.YEAR);

        boolean seleccionado =
                mismoDia(
                        fecha,
                        fechaSeleccionada
                );

        boolean esHoy =
                mismoDia(
                        fecha,
                        Calendar.getInstance()
                );

        List<EventoCalendario> eventos =
                obtenerEventosFecha(fecha);

        boolean tieneSupervision = false;
        boolean tieneActividad = false;

        for (EventoCalendario evento : eventos) {
            if (evento.esSupervision) {
                tieneSupervision = true;
            } else {
                tieneActividad = true;
            }
        }

        LinearLayout celda =
                new LinearLayout(this);

        celda.setOrientation(
                LinearLayout.VERTICAL
        );

        celda.setGravity(
                Gravity.CENTER
        );

        celda.setPadding(
                dp(2),
                dp(7),
                dp(2),
                dp(5)
        );

        int columna =
                posicion % 7;

        int fila =
                posicion / 7;

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.rowSpec =
                GridLayout.spec(fila);

        params.columnSpec =
                GridLayout.spec(
                        columna,
                        1f
                );

        params.width = 0;
        params.height = dp(70);

        params.setMargins(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        celda.setLayoutParams(params);

        celda.setBackground(
                crearFondoDia(
                        seleccionado,
                        esHoy,
                        perteneceMes
                )
        );

        TextView txtDia =
                new TextView(this);

        txtDia.setText(
                String.valueOf(
                        fecha.get(
                                Calendar.DAY_OF_MONTH
                        )
                )
        );

        txtDia.setGravity(
                Gravity.CENTER
        );

        txtDia.setTextSize(14);

        txtDia.setTypeface(
                Typeface.DEFAULT,
                seleccionado
                        ? Typeface.BOLD
                        : Typeface.NORMAL
        );

        if (seleccionado) {
            txtDia.setTextColor(
                    Color.WHITE
            );

        } else if (perteneceMes) {
            txtDia.setTextColor(
                    Color.parseColor(
                            "#111827"
                    )
            );

        } else {
            txtDia.setTextColor(
                    Color.parseColor(
                            "#9CA3AF"
                    )
            );
        }

        celda.addView(txtDia);

        LinearLayout marcadores =
                new LinearLayout(this);

        marcadores.setOrientation(
                LinearLayout.HORIZONTAL
        );

        marcadores.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams marcadoresParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(10)
                );

        marcadoresParams.setMargins(
                0,
                dp(4),
                0,
                0
        );

        marcadores.setLayoutParams(
                marcadoresParams
        );

        if (tieneSupervision) {
            marcadores.addView(
                    crearMarcador(
                            seleccionado
                                    ? "#FFFFFF"
                                    : "#006341"
                    )
            );
        }

        if (tieneActividad) {
            marcadores.addView(
                    crearMarcador(
                            seleccionado
                                    ? "#FCA5A5"
                                    : "#E30613"
                    )
            );
        }

        celda.addView(marcadores);

        if (!eventos.isEmpty()) {
            TextView txtCantidad =
                    new TextView(this);

            txtCantidad.setText(
                    eventos.size() == 1
                            ? "1 evento"
                            : eventos.size() +
                            " eventos"
            );

            txtCantidad.setGravity(
                    Gravity.CENTER
            );

            txtCantidad.setTextSize(8);

            txtCantidad.setTextColor(
                    seleccionado
                            ? Color.WHITE
                            : Color.parseColor(
                            "#6B7280"
                    )
            );

            celda.addView(txtCantidad);
        }

        celda.setClickable(true);
        celda.setFocusable(true);

        celda.setOnClickListener(v -> {
            fechaSeleccionada.setTimeInMillis(
                    fecha.getTimeInMillis()
            );

            limpiarHora(
                    fechaSeleccionada
            );

            boolean cambioMes =
                    fecha.get(Calendar.MONTH) !=
                            mesVisible.get(Calendar.MONTH) ||

                            fecha.get(Calendar.YEAR) !=
                                    mesVisible.get(Calendar.YEAR);

            if (cambioMes) {
                mesVisible.setTimeInMillis(
                        fecha.getTimeInMillis()
                );

                mesVisible.set(
                        Calendar.DAY_OF_MONTH,
                        1
                );
            }

            renderizarCalendario();
        });

        return celda;
    }

    private GradientDrawable crearFondoDia(
            boolean seleccionado,
            boolean esHoy,
            boolean perteneceMes
    ) {
        GradientDrawable fondo =
                new GradientDrawable();

        fondo.setShape(
                GradientDrawable.RECTANGLE
        );

        fondo.setCornerRadius(
                dp(10)
        );

        if (seleccionado) {
            fondo.setColor(
                    Color.parseColor(
                            "#006341"
                    )
            );

            fondo.setStroke(
                    dp(1),
                    Color.parseColor(
                            "#006341"
                    )
            );

        } else if (!perteneceMes) {
            fondo.setColor(
                    Color.parseColor(
                            "#F3F4F6"
                    )
            );

            fondo.setStroke(
                    dp(1),
                    Color.parseColor(
                            "#E5E7EB"
                    )
            );

        } else {
            fondo.setColor(Color.WHITE);

            fondo.setStroke(
                    esHoy
                            ? dp(2)
                            : dp(1),

                    esHoy
                            ? Color.parseColor(
                            "#E30613"
                    )
                            : Color.parseColor(
                            "#DDE7E1"
                    )
            );
        }

        return fondo;
    }

    private View crearMarcador(
            String color
    ) {
        View punto =
                new View(this);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(7),
                        dp(7)
                );

        params.setMargins(
                dp(2),
                0,
                dp(2),
                0
        );

        punto.setLayoutParams(params);

        GradientDrawable fondo =
                new GradientDrawable();

        fondo.setShape(
                GradientDrawable.OVAL
        );

        fondo.setColor(
                Color.parseColor(color)
        );

        punto.setBackground(fondo);

        return punto;
    }

    private void actualizarResumenMes() {
        int supervisiones = 0;
        int actividades = 0;

        for (EventoCalendario evento :
                todosEventos) {

            Calendar fecha =
                    parsearFecha(
                            evento.fecha
                    );

            if (fecha == null) {
                continue;
            }

            if (fecha.get(Calendar.MONTH) !=
                    mesVisible.get(Calendar.MONTH) ||

                    fecha.get(Calendar.YEAR) !=
                            mesVisible.get(Calendar.YEAR)) {

                continue;
            }

            if (evento.esSupervision) {
                supervisiones++;
            } else {
                actividades++;
            }
        }

        txtResumenMes.setText(
                supervisiones +
                        (
                                supervisiones == 1
                                        ? " supervisión"
                                        : " supervisiones"
                        ) +
                        " · " +

                        actividades +
                        (
                                actividades == 1
                                        ? " actividad"
                                        : " actividades"
                        )
        );
    }

    private void mostrarEventosDiaSeleccionado() {
        containerEventosDia.removeAllViews();

        SimpleDateFormat formatoTitulo =
                new SimpleDateFormat(
                        "EEEE d 'de' MMMM 'de' yyyy",
                        LOCALE_MEXICO
                );

        String titulo =
                formatoTitulo.format(
                        fechaSeleccionada.getTime()
                );

        txtFechaSeleccionada.setText(
                primeraLetraMayuscula(
                        titulo
                )
        );

        List<EventoCalendario> eventos =
                obtenerEventosFecha(
                        fechaSeleccionada
                );

        ordenarEventos(eventos);

        int supervisiones = 0;
        int actividades = 0;

        for (EventoCalendario evento : eventos) {
            if (evento.esSupervision) {
                supervisiones++;
            } else {
                actividades++;
            }
        }

        txtResumenDia.setText(
                supervisiones +
                        (
                                supervisiones == 1
                                        ? " supervisión"
                                        : " supervisiones"
                        ) +
                        " · " +

                        actividades +
                        (
                                actividades == 1
                                        ? " actividad"
                                        : " actividades"
                        )
        );

        if (eventos.isEmpty()) {
            emptyStateDia.setVisibility(
                    View.VISIBLE
            );

            containerEventosDia.setVisibility(
                    View.GONE
            );

            return;
        }

        emptyStateDia.setVisibility(
                View.GONE
        );

        containerEventosDia.setVisibility(
                View.VISIBLE
        );

        for (EventoCalendario evento : eventos) {
            containerEventosDia.addView(
                    crearTarjetaEvento(evento)
            );
        }
    }

    private View crearTarjetaEvento(
            EventoCalendario evento
    ) {
        LinearLayout tarjeta =
                new LinearLayout(this);

        tarjeta.setOrientation(
                LinearLayout.VERTICAL
        );

        tarjeta.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        tarjeta.setBackgroundResource(
                R.drawable.bg_card_green
        );

        tarjeta.setElevation(dp(4));

        LinearLayout.LayoutParams tarjetaParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        tarjetaParams.setMargins(
                0,
                0,
                0,
                dp(14)
        );

        tarjeta.setLayoutParams(
                tarjetaParams
        );

        LinearLayout encabezado =
                new LinearLayout(this);

        encabezado.setOrientation(
                LinearLayout.HORIZONTAL
        );

        encabezado.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout informacion =
                new LinearLayout(this);

        informacion.setOrientation(
                LinearLayout.VERTICAL
        );

        informacion.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView txtTitulo =
                new TextView(this);

        txtTitulo.setText(
                evento.titulo
        );

        txtTitulo.setTextSize(16);

        txtTitulo.setTextColor(
                Color.parseColor(
                        "#111827"
                )
        );

        txtTitulo.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        TextView txtHora =
                new TextView(this);

        txtHora.setText(
                evento.hora.isEmpty()
                        ? "Sin hora definida"
                        : evento.hora
        );

        txtHora.setTextSize(12);

        txtHora.setTextColor(
                Color.parseColor(
                        "#4F6B5D"
                )
        );

        informacion.addView(txtTitulo);
        informacion.addView(txtHora);

        TextView badge =
                crearBadge(
                        evento.tipo,
                        evento.esSupervision
                );

        encabezado.addView(informacion);
        encabezado.addView(badge);

        tarjeta.addView(encabezado);

        tarjeta.addView(
                crearTextoDetalle(
                        evento.origenHora
                )
        );

        tarjeta.addView(
                crearTextoDetalle(
                        "Estado: " +
                                valorVisible(
                                        evento.estado,
                                        "Sin estado"
                                )
                )
        );

        tarjeta.addView(
                crearTextoDetalle(
                        "Prioridad: " +
                                valorVisible(
                                        evento.prioridad,
                                        "Sin prioridad"
                                )
                )
        );

        if (!evento.descripcion
                .trim()
                .isEmpty()) {

            tarjeta.addView(
                    crearTextoDetalle(
                            evento.descripcion
                    )
            );
        }

        tarjeta.addView(
                crearTextoDetalle(
                        evento.tecnico
                                .trim()
                                .isEmpty()
                                ? "Sin Técnico relacionado"
                                : "Técnico: " +
                                evento.tecnico
                )
        );

        if (!evento.folio
                .trim()
                .isEmpty() &&
                !evento.esSupervision) {

            tarjeta.addView(
                    crearTextoDetalle(
                            "Supervisión vinculada: " +
                                    evento.folio
                    )
            );
        }

        LinearLayout acciones =
                new LinearLayout(this);

        acciones.setOrientation(
                LinearLayout.HORIZONTAL
        );

        LinearLayout.LayoutParams accionesParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        accionesParams.setMargins(
                0,
                dp(16),
                0,
                0
        );

        acciones.setLayoutParams(
                accionesParams
        );

        if (evento.esSupervision) {
            TextView btnAbrir =
                    crearBoton(
                            "Abrir supervisión",
                            true,
                            false
                    );

            btnAbrir.setOnClickListener(
                    v -> abrirSupervision(
                            evento.folio
                    )
            );

            acciones.addView(btnAbrir);

        } else {
            TextView btnEditar =
                    crearBoton(
                            "Editar",
                            false,
                            false
                    );

            TextView btnEstado =
                    crearBoton(
                            "Estado",
                            true,
                            false
                    );

            TextView btnEliminar =
                    crearBoton(
                            "Eliminar",
                            false,
                            true
                    );

            btnEditar.setOnClickListener(
                    v -> mostrarDialogoActividad(
                            evento
                    )
            );

            btnEstado.setOnClickListener(
                    v -> mostrarDialogoEstado(
                            evento
                    )
            );

            btnEliminar.setOnClickListener(
                    v -> confirmarEliminarActividad(
                            evento
                    )
            );

            acciones.addView(btnEditar);
            acciones.addView(btnEstado);
            acciones.addView(btnEliminar);
        }

        tarjeta.addView(acciones);

        return tarjeta;
    }

    private TextView crearBadge(
            String texto,
            boolean supervision
    ) {
        TextView badge =
                new TextView(this);

        badge.setText(texto);
        badge.setTextSize(10);

        badge.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        badge.setGravity(
                Gravity.CENTER
        );

        badge.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );

        GradientDrawable fondo =
                new GradientDrawable();

        fondo.setCornerRadius(
                dp(18)
        );

        if (supervision) {
            badge.setTextColor(
                    Color.parseColor(
                            "#006341"
                    )
            );

            fondo.setColor(
                    Color.parseColor(
                            "#D1FAE5"
                    )
            );

        } else {
            badge.setTextColor(
                    Color.parseColor(
                            "#B91C1C"
                    )
            );

            fondo.setColor(
                    Color.parseColor(
                            "#FEE2E2"
                    )
            );
        }

        badge.setBackground(fondo);

        return badge;
    }

    private TextView crearTextoDetalle(
            String texto
    ) {
        TextView textView =
                new TextView(this);

        textView.setText(texto);
        textView.setTextSize(13);

        textView.setTextColor(
                Color.parseColor(
                        "#374151"
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(8),
                0,
                0
        );

        textView.setLayoutParams(params);

        return textView;
    }

    private TextView crearBoton(
            String texto,
            boolean principal,
            boolean peligro
    ) {
        TextView boton =
                new TextView(this);

        boton.setText(texto);
        boton.setGravity(Gravity.CENTER);
        boton.setTextSize(11);

        boton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        boton.setClickable(true);
        boton.setFocusable(true);

        if (principal) {
            boton.setBackgroundResource(
                    R.drawable.bg_button_login
            );

            boton.setTextColor(
                    Color.WHITE
            );

        } else {
            boton.setBackgroundResource(
                    R.drawable.bg_input_login
            );

            boton.setTextColor(
                    peligro
                            ? Color.parseColor(
                            "#B91C1C"
                    )
                            : Color.parseColor(
                            "#006341"
                    )
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1
                );

        params.setMargins(
                dp(3),
                0,
                dp(3),
                0
        );

        boton.setLayoutParams(params);

        return boton;
    }

    private void mostrarDialogoActividad(
            EventoCalendario eventoEdicion
    ) {
        boolean editando =
                eventoEdicion != null;

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout formulario =
                new LinearLayout(this);

        formulario.setOrientation(
                LinearLayout.VERTICAL
        );

        formulario.setPadding(
                dp(22),
                dp(10),
                dp(22),
                dp(10)
        );

        scroll.addView(formulario);

        EditText etTitulo =
                crearCampoDialogo(
                        "Título de la actividad"
                );

        EditText etFecha =
                crearCampoDialogo(
                        "Fecha"
                );

        EditText etHora =
                crearCampoDialogo(
                        "Hora"
                );

        EditText etDescripcion =
                crearCampoDialogo(
                        "Descripción"
                );

        etDescripcion.setMinLines(3);

        etDescripcion.setGravity(
                Gravity.TOP |
                        Gravity.START
        );

        Spinner spPrioridad =
                new Spinner(this);

        Spinner spSupervision =
                new Spinner(this);

        EditText etTecnico =
                crearCampoDialogo(
                        "Técnico relacionado"
                );

        formulario.addView(
                crearEtiquetaDialogo(
                        "Título"
                )
        );

        formulario.addView(etTitulo);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Fecha"
                )
        );

        formulario.addView(etFecha);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Hora"
                )
        );

        formulario.addView(etHora);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Descripción"
                )
        );

        formulario.addView(etDescripcion);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Prioridad"
                )
        );

        configurarSpinnerPrioridad(
                spPrioridad
        );

        formulario.addView(spPrioridad);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Supervisión vinculada"
                )
        );

        configurarSpinnerSupervisiones(
                spSupervision
        );

        formulario.addView(spSupervision);

        formulario.addView(
                crearEtiquetaDialogo(
                        "Técnico relacionado"
                )
        );

        formulario.addView(etTecnico);

        Calendar fechaInicial =
                (Calendar) fechaSeleccionada
                        .clone();

        Calendar horaInicial =
                Calendar.getInstance();

        horaInicial.add(
                Calendar.HOUR_OF_DAY,
                1
        );

        horaInicial.set(
                Calendar.MINUTE,
                0
        );

        String estado =
                "Pendiente";

        String fechaRegistro =
                obtenerFechaActual();

        if (editando) {
            etTitulo.setText(
                    eventoEdicion.titulo
            );

            etFecha.setText(
                    eventoEdicion.fecha
            );

            etHora.setText(
                    eventoEdicion.hora
            );

            etDescripcion.setText(
                    eventoEdicion.descripcion
            );

            seleccionarSpinner(
                    spPrioridad,
                    eventoEdicion.prioridad
            );

            seleccionarSupervisionSpinner(
                    spSupervision,
                    eventoEdicion.folio
            );

            etTecnico.setText(
                    eventoEdicion.tecnico
            );

            estado =
                    eventoEdicion.estado;

            fechaRegistro =
                    eventoEdicion.fechaRegistro;

            Calendar fechaEditada =
                    parsearFecha(
                            eventoEdicion.fecha
                    );

            if (fechaEditada != null) {
                fechaInicial =
                        fechaEditada;
            }

            Calendar horaEditada =
                    parsearHora(
                            eventoEdicion.hora
                    );

            if (horaEditada != null) {
                horaInicial =
                        horaEditada;
            }

        } else {
            etFecha.setText(
                    formatoFecha(
                            fechaInicial
                    )
            );

            etHora.setText(
                    formatoHora(
                            horaInicial
                    )
            );

            spPrioridad.setSelection(1);
        }

        final Calendar fechaDialogo =
                fechaInicial;

        final Calendar horaDialogo =
                horaInicial;

        etFecha.setFocusable(false);
        etFecha.setCursorVisible(false);
        etFecha.setClickable(true);

        etHora.setFocusable(false);
        etHora.setCursorVisible(false);
        etHora.setClickable(true);

        etFecha.setOnClickListener(v -> {
            DatePickerDialog dialog =
                    new DatePickerDialog(
                            this,
                            (view, year, month, day) -> {

                                fechaDialogo.set(
                                        year,
                                        month,
                                        day
                                );

                                etFecha.setText(
                                        formatoFecha(
                                                fechaDialogo
                                        )
                                );
                            },
                            fechaDialogo.get(
                                    Calendar.YEAR
                            ),
                            fechaDialogo.get(
                                    Calendar.MONTH
                            ),
                            fechaDialogo.get(
                                    Calendar.DAY_OF_MONTH
                            )
                    );

            dialog.show();
        });

        etHora.setOnClickListener(v -> {
            TimePickerDialog dialog =
                    new TimePickerDialog(
                            this,
                            (view, hour, minute) -> {

                                horaDialogo.set(
                                        Calendar.HOUR_OF_DAY,
                                        hour
                                );

                                horaDialogo.set(
                                        Calendar.MINUTE,
                                        minute
                                );

                                etHora.setText(
                                        formatoHora(
                                                horaDialogo
                                        )
                                );
                            },
                            horaDialogo.get(
                                    Calendar.HOUR_OF_DAY
                            ),
                            horaDialogo.get(
                                    Calendar.MINUTE
                            ),
                            true
                    );

            dialog.show();
        });

        spSupervision.setOnItemSelectedListener(
                new AdapterView
                        .OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position <= 0 ||
                                position >=
                                        tecnicosSupervisiones
                                                .size()) {

                            return;
                        }

                        if (etTecnico.getText()
                                .toString()
                                .trim()
                                .isEmpty()) {

                            etTecnico.setText(
                                    tecnicosSupervisiones
                                            .get(position)
                            );
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        final String estadoFinal =
                estado;

        final String fechaRegistroFinal =
                fechaRegistro;

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                editando
                                        ? "Editar actividad"
                                        : "Agregar actividad"
                        )
                        .setView(scroll)
                        .setNegativeButton(
                                "Cancelar",
                                null
                        )
                        .setPositiveButton(
                                "Guardar",
                                null
                        )
                        .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(v -> {

                    String titulo =
                            etTitulo.getText()
                                    .toString()
                                    .trim();

                    String fecha =
                            normalizarFecha(
                                    etFecha.getText()
                                            .toString()
                            );

                    String hora =
                            normalizarHora(
                                    etHora.getText()
                                            .toString()
                            );

                    String descripcion =
                            etDescripcion.getText()
                                    .toString()
                                    .trim();

                    String prioridad =
                            spPrioridad
                                    .getSelectedItem()
                                    .toString();

                    int posicionSupervision =
                            spSupervision
                                    .getSelectedItemPosition();

                    String folio = "";

                    if (posicionSupervision >= 0 &&
                            posicionSupervision <
                                    foliosSupervisiones
                                            .size()) {

                        folio =
                                foliosSupervisiones
                                        .get(
                                                posicionSupervision
                                        );
                    }

                    String tecnico =
                            etTecnico.getText()
                                    .toString()
                                    .trim();

                    if (titulo.isEmpty()) {
                        etTitulo.setError(
                                "Escribe el título"
                        );

                        etTitulo.requestFocus();
                        return;
                    }

                    if (fecha.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Selecciona una fecha válida",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    if (hora.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Selecciona una hora válida",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String idActividad =
                            editando
                                    ? eventoEdicion.id
                                    : "ACT-" +
                                    System.currentTimeMillis();

                    String registro =
                            limpiar(idActividad) + "|" +
                                    limpiar(titulo) + "|" +
                                    limpiar(fecha) + "|" +
                                    limpiar(hora) + "|" +
                                    limpiar(descripcion) + "|" +
                                    limpiar(prioridad) + "|" +
                                    limpiar(folio) + "|" +
                                    limpiar(tecnico) + "|" +
                                    limpiar(estadoFinal) + "|" +
                                    limpiar(fechaRegistroFinal);

                    guardarOActualizarActividad(
                            idActividad,
                            registro
                    );

                    dialog.dismiss();

                    Calendar nuevaFecha =
                            parsearFecha(fecha);

                    if (nuevaFecha != null) {
                        fechaSeleccionada
                                .setTimeInMillis(
                                        nuevaFecha
                                                .getTimeInMillis()
                                );

                        mesVisible.setTimeInMillis(
                                nuevaFecha
                                        .getTimeInMillis()
                        );

                        mesVisible.set(
                                Calendar.DAY_OF_MONTH,
                                1
                        );
                    }

                    recargarCalendario();

                    Toast.makeText(
                            this,
                            editando
                                    ? "Actividad actualizada"
                                    : "Actividad registrada",
                            Toast.LENGTH_SHORT
                    ).show();
                })
        );

        dialog.show();
    }

    private EditText crearCampoDialogo(
            String hint
    ) {
        EditText campo =
                new EditText(this);

        campo.setHint(hint);

        campo.setTextColor(
                Color.parseColor(
                        "#111827"
                )
        );

        campo.setHintTextColor(
                Color.parseColor(
                        "#9CA3AF"
                )
        );

        campo.setTextSize(14);

        campo.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        campo.setBackgroundResource(
                R.drawable.bg_input_login
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(6),
                0,
                dp(4)
        );

        campo.setLayoutParams(params);

        return campo;
    }

    private TextView crearEtiquetaDialogo(
            String texto
    ) {
        TextView etiqueta =
                new TextView(this);

        etiqueta.setText(texto);

        etiqueta.setTextColor(
                Color.parseColor(
                        "#374151"
                )
        );

        etiqueta.setTextSize(12);

        etiqueta.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(12),
                0,
                0
        );

        etiqueta.setLayoutParams(params);

        return etiqueta;
    }

    private void configurarSpinnerPrioridad(
            Spinner spinner
    ) {
        String[] prioridades = {
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
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);

        spinner.setBackgroundResource(
                R.drawable.bg_input_login
        );

        spinner.setPadding(
                dp(10),
                0,
                dp(10),
                0
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)
                );

        params.setMargins(
                0,
                dp(6),
                0,
                dp(4)
        );

        spinner.setLayoutParams(params);
    }

    private void configurarSpinnerSupervisiones(
            Spinner spinner
    ) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        opcionesSupervisiones
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);

        spinner.setBackgroundResource(
                R.drawable.bg_input_login
        );

        spinner.setPadding(
                dp(10),
                0,
                dp(10),
                0
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(56)
                );

        params.setMargins(
                0,
                dp(6),
                0,
                dp(4)
        );

        spinner.setLayoutParams(params);
    }

    private void seleccionarSpinner(
            Spinner spinner,
            String valor
    ) {
        if (valor == null) {
            return;
        }

        for (int i = 0;
             i < spinner.getCount();
             i++) {

            if (valor.equalsIgnoreCase(
                    spinner.getItemAtPosition(i)
                            .toString()
            )) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void seleccionarSupervisionSpinner(
            Spinner spinner,
            String folio
    ) {
        if (folio == null ||
                folio.trim().isEmpty()) {

            spinner.setSelection(0);
            return;
        }

        for (int i = 0;
             i < foliosSupervisiones.size();
             i++) {

            if (folio.equalsIgnoreCase(
                    foliosSupervisiones.get(i)
            )) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void guardarOActualizarActividad(
            String idActividad,
            String nuevoRegistro
    ) {
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

        StringBuilder actualizados =
                new StringBuilder();

        boolean encontrada = false;

        if (datos != null &&
                !datos.trim().isEmpty()) {

            String[] registros =
                    datos.split("\n");

            for (String registro : registros) {
                if (registro.trim().isEmpty()) {
                    continue;
                }

                String[] partes =
                        registro.split("\\|", -1);

                if (partes.length >= 1 &&
                        idActividad.equalsIgnoreCase(
                                partes[0].trim()
                        )) {

                    agregarRegistro(
                            actualizados,
                            nuevoRegistro
                    );

                    encontrada = true;

                } else {
                    agregarRegistro(
                            actualizados,
                            registro
                    );
                }
            }
        }

        if (!encontrada) {
            agregarRegistro(
                    actualizados,
                    nuevoRegistro
            );
        }

        preferences.edit()
                .putString(
                        KEY_ACTIVIDADES,
                        actualizados.toString()
                )
                .apply();
    }

    private void mostrarDialogoEstado(
            EventoCalendario evento
    ) {
        String[] estados = {
                "Pendiente",
                "Realizada",
                "Cancelada"
        };

        int seleccion = 0;

        for (int i = 0;
             i < estados.length;
             i++) {

            if (estados[i].equalsIgnoreCase(
                    evento.estado
            )) {
                seleccion = i;
                break;
            }
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Cambiar estado"
                        )
                        .setSingleChoiceItems(
                                estados,
                                seleccion,
                                null
                        )
                        .setNegativeButton(
                                "Cancelar",
                                null
                        )
                        .setPositiveButton(
                                "Guardar",
                                null
                        )
                        .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(v -> {

                    int posicion =
                            dialog.getListView()
                                    .getCheckedItemPosition();

                    if (posicion < 0) {
                        return;
                    }

                    actualizarEstadoActividad(
                            evento.id,
                            estados[posicion]
                    );

                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    private void actualizarEstadoActividad(
            String idActividad,
            String estado
    ) {
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

        if (datos == null ||
                datos.trim().isEmpty()) {

            return;
        }

        StringBuilder actualizados =
                new StringBuilder();

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length >= 10 &&
                    idActividad.equalsIgnoreCase(
                            partes[0].trim()
                    )) {

                partes[8] = estado;

                agregarRegistro(
                        actualizados,
                        unirPartes(partes)
                );

            } else {
                agregarRegistro(
                        actualizados,
                        registro
                );
            }
        }

        preferences.edit()
                .putString(
                        KEY_ACTIVIDADES,
                        actualizados.toString()
                )
                .apply();

        recargarCalendario();

        Toast.makeText(
                this,
                "Estado actualizado a " +
                        estado,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void confirmarEliminarActividad(
            EventoCalendario evento
    ) {
        new AlertDialog.Builder(this)
                .setTitle(
                        "Eliminar actividad"
                )
                .setMessage(
                        "¿Deseas eliminar la actividad \"" +
                                evento.titulo +
                                "\"?"
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Eliminar",
                        (dialog, which) ->
                                eliminarActividad(
                                        evento.id
                                )
                )
                .show();
    }

    private void eliminarActividad(
            String idActividad
    ) {
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

        if (datos == null ||
                datos.trim().isEmpty()) {

            return;
        }

        StringBuilder actualizados =
                new StringBuilder();

        String[] registros =
                datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length >= 1 &&
                    idActividad.equalsIgnoreCase(
                            partes[0].trim()
                    )) {

                continue;
            }

            agregarRegistro(
                    actualizados,
                    registro
            );
        }

        preferences.edit()
                .putString(
                        KEY_ACTIVIDADES,
                        actualizados.toString()
                )
                .apply();

        recargarCalendario();

        Toast.makeText(
                this,
                "Actividad eliminada",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void abrirSupervision(
            String folio
    ) {
        Intent intent = new Intent(
                CalendarioSupervisorActivity.this,
                RevisionSupervisionSupervisorActivity.class
        );

        intent.putExtra(
                "folio",
                folio
        );

        startActivity(intent);
    }

    private List<EventoCalendario> obtenerEventosFecha(
            Calendar fecha
    ) {
        List<EventoCalendario> resultado =
                new ArrayList<>();

        for (EventoCalendario evento :
                todosEventos) {

            Calendar fechaEvento =
                    parsearFecha(
                            evento.fecha
                    );

            if (fechaEvento == null) {
                continue;
            }

            if (mismoDia(
                    fecha,
                    fechaEvento
            )) {
                resultado.add(evento);
            }
        }

        return resultado;
    }

    private void ordenarEventos(
            List<EventoCalendario> eventos
    ) {
        Collections.sort(
                eventos,
                (primero, segundo) -> {

                    int minutosPrimero =
                            obtenerMinutosHora(
                                    primero.hora
                            );

                    int minutosSegundo =
                            obtenerMinutosHora(
                                    segundo.hora
                            );

                    if (minutosPrimero !=
                            minutosSegundo) {

                        return Integer.compare(
                                minutosPrimero,
                                minutosSegundo
                        );
                    }

                    if (primero.esSupervision !=
                            segundo.esSupervision) {

                        return primero.esSupervision
                                ? -1
                                : 1;
                    }

                    return primero.titulo
                            .compareToIgnoreCase(
                                    segundo.titulo
                            );
                }
        );
    }

    private int obtenerMinutosHora(
            String hora
    ) {
        if (hora == null ||
                hora.trim().isEmpty()) {

            return Integer.MAX_VALUE;
        }

        try {
            String[] partes =
                    hora.split(":");

            if (partes.length < 2) {
                return Integer.MAX_VALUE;
            }

            return Integer.parseInt(
                    partes[0]
            ) * 60 +
                    Integer.parseInt(
                            partes[1]
                    );

        } catch (Exception exception) {
            return Integer.MAX_VALUE;
        }
    }

    private String normalizarFecha(
            String fecha
    ) {
        Calendar calendar =
                parsearFecha(fecha);

        if (calendar == null) {
            return "";
        }

        return formatoFecha(calendar);
    }

    private Calendar parsearFecha(
            String fecha
    ) {
        if (fecha == null ||
                fecha.trim().isEmpty()) {

            return null;
        }

        String[] formatos = {
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };

        for (String patron : formatos) {
            try {
                SimpleDateFormat formato =
                        new SimpleDateFormat(
                                patron,
                                Locale.getDefault()
                        );

                formato.setLenient(false);

                Date date =
                        formato.parse(
                                fecha.trim()
                        );

                if (date == null) {
                    continue;
                }

                Calendar calendar =
                        Calendar.getInstance();

                calendar.setTime(date);
                limpiarHora(calendar);

                return calendar;

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private Calendar parsearFechaRegistro(
            String fechaRegistro
    ) {
        if (fechaRegistro == null ||
                fechaRegistro.trim().isEmpty()) {

            return null;
        }

        String[] formatos = {
                "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "dd-MM-yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };

        for (String patron : formatos) {
            try {
                SimpleDateFormat formato =
                        new SimpleDateFormat(
                                patron,
                                Locale.getDefault()
                        );

                formato.setLenient(false);

                Date date =
                        formato.parse(
                                fechaRegistro.trim()
                        );

                if (date == null) {
                    continue;
                }

                Calendar calendar =
                        Calendar.getInstance();

                calendar.setTime(date);

                return calendar;

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private boolean registroContieneHora(
            String fechaRegistro
    ) {
        if (fechaRegistro == null) {
            return false;
        }

        return fechaRegistro
                .trim()
                .matches(
                        ".*\\b\\d{1,2}:\\d{2}(:\\d{2})?\\b.*"
                );
    }

    private String normalizarHora(
            String hora
    ) {
        Calendar calendar =
                parsearHora(hora);

        if (calendar == null) {
            return "";
        }

        return formatoHora(calendar);
    }

    private Calendar parsearHora(
            String hora
    ) {
        if (hora == null ||
                hora.trim().isEmpty()) {

            return null;
        }

        String[] formatos = {
                "HH:mm",
                "H:mm"
        };

        for (String patron : formatos) {
            try {
                SimpleDateFormat formato =
                        new SimpleDateFormat(
                                patron,
                                Locale.getDefault()
                        );

                formato.setLenient(false);

                Date date =
                        formato.parse(
                                hora.trim()
                        );

                if (date == null) {
                    continue;
                }

                Calendar calendar =
                        Calendar.getInstance();

                calendar.setTime(date);

                return calendar;

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private String formatoFecha(
            Calendar calendar
    ) {
        return new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(
                calendar.getTime()
        );
    }

    private String formatoHora(
            Calendar calendar
    ) {
        return new SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
        ).format(
                calendar.getTime()
        );
    }

    private String obtenerFechaActual() {
        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(
                Calendar.getInstance()
                        .getTime()
        );
    }

    private boolean mismoDia(
            Calendar primera,
            Calendar segunda
    ) {
        return
                primera.get(Calendar.YEAR) ==
                        segunda.get(Calendar.YEAR) &&

                        primera.get(Calendar.MONTH) ==
                                segunda.get(Calendar.MONTH) &&

                        primera.get(Calendar.DAY_OF_MONTH) ==
                                segunda.get(Calendar.DAY_OF_MONTH);
    }

    private void limpiarHora(
            Calendar calendar
    ) {
        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );
    }

    private String primeraLetraMayuscula(
            String texto
    ) {
        if (texto == null ||
                texto.trim().isEmpty()) {

            return "";
        }

        return texto.substring(0, 1)
                .toUpperCase(
                        LOCALE_MEXICO
                ) +
                texto.substring(1);
    }

    private String valorVisible(
            String valor,
            String alternativo
    ) {
        if (valor == null ||
                valor.trim().isEmpty()) {

            return alternativo;
        }

        return valor.trim();
    }

    private void agregarRegistro(
            StringBuilder builder,
            String registro
    ) {
        if (builder.length() > 0) {
            builder.append("\n");
        }

        builder.append(registro);
    }

    private String unirPartes(
            String[] partes
    ) {
        StringBuilder resultado =
                new StringBuilder();

        for (int i = 0;
             i < partes.length;
             i++) {

            if (i > 0) {
                resultado.append("|");
            }

            resultado.append(partes[i]);
        }

        return resultado.toString();
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

    private int dp(
            int value
    ) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    private static class EventoCalendario {

        String tipo = "";
        String id = "";
        String titulo = "";
        String fecha = "";
        String hora = "";
        String origenHora = "";
        String descripcion = "";
        String prioridad = "";
        String folio = "";
        String tecnico = "";
        String estado = "";
        String fechaRegistro = "";

        boolean esSupervision = false;
    }
}