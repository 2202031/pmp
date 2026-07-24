package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;
import java.util.Set;

public class CalendarioTecnicoActivity extends Activity {

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

    private static final String PREFS_HORARIOS =
            "horarios_supervision_local";

    private static final String PREFIJO_HORA =
            "hora_";

    private static final String PREFS_CALENDARIO =
            "calendario_local";

    private static final String KEY_ACTIVIDADES =
            "actividades";

    private static final Locale LOCALE_MEXICO =
            new Locale("es", "MX");

    private TextView btnVolver;
    private TextView btnMesAnterior;
    private TextView btnMesSiguiente;
    private TextView btnHoy;

    private TextView txtMesAnio;
    private TextView txtResumenMes;
    private TextView txtFechaSeleccionada;
    private TextView txtResumenDia;

    private GridLayout gridCalendario;

    private LinearLayout emptyStateDia;
    private LinearLayout containerEventosDia;

    private String usuarioActual = "";
    private String nombreActual = "";

    private final Calendar mesVisible =
            Calendar.getInstance();

    private final Calendar fechaSeleccionada =
            Calendar.getInstance();

    private final List<EventoCalendario> todosEventos =
            new ArrayList<>();

    private final Set<String> foliosAsignados =
            new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_calendario_tecnico
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        prepararFechas();
        configurarEventos();
        sincronizarYRecargar();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gridCalendario != null &&
                usuarioActual != null &&
                !usuarioActual.trim().isEmpty()) {

            sincronizarYRecargar();
        }
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private String datosSupervisionesServidor = "";
    private final java.util.Map<String, String> horariosServidor = new java.util.HashMap<>();
    private String datosActividadesServidor = "";

    // Trae del servidor las supervisiones asignadas a este supervisor y
    // luego vuelve a pintar el calendario con la lógica local ya existente,
    // pero leyendo de memoria en vez de SharedPreferences.
    private void sincronizarYRecargar() {
        executorService.execute(() -> {
            com.pmp.front.ServidorSyncHelper.ResultadoSincronizacion resultado =
                    com.pmp.front.ServidorSyncHelper.obtenerSupervisiones("supervisor", usuarioActual);

            datosSupervisionesServidor = resultado.registros;
            horariosServidor.clear();
            horariosServidor.putAll(resultado.horariosPorFolio);
            datosActividadesServidor = com.pmp.front.ServidorSyncHelper.obtenerActividades();

            mainHandler.post(this::recargarCalendario);
        });
    }

    private void inicializarVistas() {
        btnVolver =
                findViewById(R.id.btnVolver);

        btnMesAnterior =
                findViewById(R.id.btnMesAnterior);

        btnMesSiguiente =
                findViewById(R.id.btnMesSiguiente);

        btnHoy =
                findViewById(R.id.btnHoy);

        txtMesAnio =
                findViewById(R.id.txtMesAnio);

        txtResumenMes =
                findViewById(R.id.txtResumenMes);

        txtFechaSeleccionada =
                findViewById(
                        R.id.txtFechaSeleccionada
                );

        txtResumenDia =
                findViewById(R.id.txtResumenDia);

        gridCalendario =
                findViewById(R.id.gridCalendario);

        emptyStateDia =
                findViewById(R.id.emptyStateDia);

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

        String rol =
                valorSeguro(
                        preferences.getString(
                                KEY_ROL,
                                ""
                        )
                );

        if (usuarioActual.isEmpty() ||
                !"supervisor".equalsIgnoreCase(rol)) {

            Intent intent = new Intent(
                    CalendarioTecnicoActivity.this,
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
        foliosAsignados.clear();

        cargarSupervisionesAsignadas();
        cargarActividadesRelacionadas();

        renderizarCalendario();
    }

    private void cargarSupervisionesAsignadas() {
        String datos = datosSupervisionesServidor;

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

            String usuarioResponsable =
                    partes[7].trim();

            if (!usuarioActual.equalsIgnoreCase(
                    usuarioResponsable
            )) {
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

            String descripcion =
                    partes[5].trim();

            String nombreResponsable =
                    partes[8].trim();

            String personalApoyo =
                    partes[10].trim();

            String estado =
                    partes[11].trim();

            String fechaHoraRegistro =
                    partes[14].trim();

            Calendar registroAsignacion =
                    parsearFechaRegistro(
                            fechaHoraRegistro
                    );

            String horaProgramada =
                    normalizarHora(
                            horariosServidor.getOrDefault(folio, "")
                    );

            /*
             * Compatibilidad con registros que
             * temporalmente pudieran tener campo 16.
             */
            if (horaProgramada.isEmpty() &&
                    partes.length > 15) {

                horaProgramada =
                        normalizarHora(
                                partes[15]
                        );
            }

            if (fechaSupervision.isEmpty() &&
                    registroAsignacion != null) {

                fechaSupervision =
                        formatoFecha(
                                registroAsignacion
                        );
            }

            if (fechaSupervision.isEmpty()) {
                continue;
            }

            Calendar fechaCalendar =
                    parsearFecha(
                            fechaSupervision
                    );

            boolean mismoDiaAsignacion =
                    registroAsignacion != null &&
                            fechaCalendar != null &&
                            mismoDia(
                                    registroAsignacion,
                                    fechaCalendar
                            );

            String horaSupervision;
            String origenHora;

            if (!horaProgramada.isEmpty()) {
                horaSupervision =
                        horaProgramada;

                origenHora =
                        "Hora programada";

            } else if (
                    mismoDiaAsignacion &&
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

            foliosAsignados.add(
                    folio.toLowerCase(Locale.ROOT)
            );

            EventoCalendario evento =
                    new EventoCalendario();

            evento.tipo =
                    "Supervisión";

            evento.id =
                    folio;

            evento.titulo =
                    "Supervisión " + folio;

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
                            ) +
                            (
                                    descripcion.isEmpty()
                                            ? ""
                                            : "\nDescripción: " +
                                            descripcion
                            );

            evento.prioridad =
                    prioridad;

            evento.folio =
                    folio;

            evento.tecnico =
                    nombreResponsable;

            evento.personalApoyo =
                    personalApoyo;

            evento.estado =
                    estado;

            evento.esSupervision =
                    true;

            todosEventos.add(evento);
        }
    }

    private void cargarActividadesRelacionadas() {
        String datos = datosActividadesServidor;

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

            String folio =
                    partes[6].trim();

            String tecnicoRelacionado =
                    partes[7].trim();

            boolean vinculadaASupervision =
                    !folio.isEmpty() &&
                            foliosAsignados.contains(
                                    folio.toLowerCase(
                                            Locale.ROOT
                                    )
                            );

            boolean asignadaDirectamente =
                    folio.isEmpty() &&
                            coincideTecnico(
                                    tecnicoRelacionado
                            );

            if (!vinculadaASupervision &&
                    !asignadaDirectamente) {

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
                    folio;

            evento.tecnico =
                    tecnicoRelacionado;

            evento.estado =
                    partes[8];

            evento.fechaRegistro =
                    partes[9];

            evento.esSupervision =
                    false;

            todosEventos.add(evento);
        }
    }

    private boolean coincideTecnico(
            String tecnico
    ) {
        if (tecnico == null ||
                tecnico.trim().isEmpty()) {

            return false;
        }

        String valor =
                tecnico.trim();

        return valor.equalsIgnoreCase(
                usuarioActual
        ) || (
                !nombreActual.isEmpty() &&
                        valor.equalsIgnoreCase(
                                nombreActual
                        )
        );
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
         * Calendar:
         * domingo = 1
         * lunes = 2
         *
         * El calendario se presenta desde lunes.
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

            gridCalendario.addView(
                    crearCeldaDia(
                            fecha,
                            posicion
                    )
            );

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

        txtMesAnio.setText(
                primeraLetraMayuscula(
                        formato.format(
                                mesVisible.getTime()
                        )
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

        LinearLayout.LayoutParams marcadorParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(10)
                );

        marcadorParams.setMargins(
                0,
                dp(4),
                0,
                0
        );

        marcadores.setLayoutParams(
                marcadorParams
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

            limpiarHora(fechaSeleccionada);

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

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "EEEE d 'de' MMMM 'de' yyyy",
                        LOCALE_MEXICO
                );

        txtFechaSeleccionada.setText(
                primeraLetraMayuscula(
                        formato.format(
                                fechaSeleccionada.getTime()
                        )
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
                        ? "Sin hora programada"
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

        if (!evento.tecnico
                .trim()
                .isEmpty()) {

            tarjeta.addView(
                    crearTextoDetalle(
                            "Técnico: " +
                                    evento.tecnico
                    )
            );
        }

        if (!evento.personalApoyo
                .trim()
                .isEmpty()) {

            tarjeta.addView(
                    crearTextoDetalle(
                            "Personal de apoyo: " +
                                    evento.personalApoyo
                    )
            );
        }

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

        if (!evento.folio
                .trim()
                .isEmpty() &&
                foliosAsignados.contains(
                        evento.folio.toLowerCase(
                                Locale.ROOT
                        )
                )) {

            TextView btnAbrir =
                    crearBotonAbrir();

            btnAbrir.setOnClickListener(
                    v -> abrirSupervision(
                            evento.folio
                    )
            );

            tarjeta.addView(btnAbrir);

            tarjeta.setClickable(true);
            tarjeta.setFocusable(true);

            tarjeta.setOnClickListener(
                    v -> abrirSupervision(
                            evento.folio
                    )
            );
        }

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

        badge.setGravity(Gravity.CENTER);

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

    private TextView crearBotonAbrir() {
        TextView boton =
                new TextView(this);

        boton.setText(
                "Abrir supervisión"
        );

        boton.setTextColor(Color.WHITE);
        boton.setTextSize(13);

        boton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        boton.setGravity(Gravity.CENTER);

        boton.setBackgroundResource(
                R.drawable.bg_button_login
        );

        boton.setClickable(true);
        boton.setFocusable(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        params.setMargins(
                0,
                dp(16),
                0,
                0
        );

        boton.setLayoutParams(params);

        return boton;
    }

    private void abrirSupervision(
            String folio
    ) {
        if (folio == null ||
                folio.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "No se encontró el folio",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                CalendarioTecnicoActivity.this,
                DetalleSupervisionTecnicoActivity.class
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

            if (fechaEvento != null &&
                    mismoDia(
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

        return calendar == null
                ? ""
                : formatoFecha(calendar);
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

        return calendar == null
                ? ""
                : formatoHora(calendar);
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

    private boolean mismoDia(
            Calendar primera,
            Calendar segunda
    ) {
        return primera.get(Calendar.YEAR) ==
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

    private String valorSeguro(
            String valor
    ) {
        return valor == null
                ? ""
                : valor.trim();
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
        String personalApoyo = "";
        String estado = "";
        String fechaRegistro = "";

        boolean esSupervision = false;
    }
}