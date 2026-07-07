package com.pmp.front;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class NotificacionesHelper {

    private static final String PREFS_NOTIFICACIONES =
            "notificaciones_local";

    private static final String KEY_NOTIFICACIONES =
            "notificaciones";

    private NotificacionesHelper() {
    }

    /*
     * Estructura de cada notificación:
     *
     * 0 id
     * 1 usuario destinatario
     * 2 rol destinatario
     * 3 título
     * 4 mensaje
     * 5 fecha y hora
     * 6 tipo
     * 7 folio relacionado
     * 8 leída
     */

    public static void crear(
            Context context,
            String destinatario,
            String rol,
            String titulo,
            String mensaje,
            String tipo,
            String folio
    ) {
        if (context == null ||
                valorSeguro(rol).isEmpty()) {

            return;
        }

        String id =
                "NOT-" +
                        UUID.randomUUID();

        String fecha =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance()
                                .getTime()
                );

        String registro =
                limpiar(id) + "|" +
                        limpiar(destinatario) + "|" +
                        limpiar(rol) + "|" +
                        limpiar(titulo) + "|" +
                        limpiar(mensaje) + "|" +
                        limpiar(fecha) + "|" +
                        limpiar(tipo) + "|" +
                        limpiar(folio) + "|" +
                        "false";

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NOTIFICACIONES,
                        Context.MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_NOTIFICACIONES,
                        ""
                );

        String actualizados;

        if (valorSeguro(datos).isEmpty()) {
            actualizados =
                    registro;
        } else {
            actualizados =
                    datos.trim() +
                            "\n" +
                            registro;
        }

        preferences.edit()
                .putString(
                        KEY_NOTIFICACIONES,
                        actualizados
                )
                .apply();
    }

    public static List<Notificacion> obtener(
            Context context,
            String usuario,
            String rol
    ) {
        List<Notificacion> resultado =
                new ArrayList<>();

        if (context == null) {
            return resultado;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NOTIFICACIONES,
                        Context.MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_NOTIFICACIONES,
                        ""
                );

        if (valorSeguro(datos).isEmpty()) {
            return resultado;
        }

        String[] registros =
                datos.split("\n");

        /*
         * Se recorren desde el último registro
         * para mostrar primero los avisos recientes.
         */
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

            if (partes.length < 9) {
                continue;
            }

            if (!correspondeUsuario(
                    partes,
                    usuario,
                    rol
            )) {
                continue;
            }

            Notificacion notificacion =
                    new Notificacion();

            notificacion.id =
                    partes[0].trim();

            notificacion.destinatario =
                    partes[1].trim();

            notificacion.rol =
                    partes[2].trim();

            notificacion.titulo =
                    partes[3].trim();

            notificacion.mensaje =
                    partes[4].trim();

            notificacion.fecha =
                    partes[5].trim();

            notificacion.tipo =
                    partes[6].trim();

            notificacion.folio =
                    partes[7].trim();

            notificacion.leida =
                    Boolean.parseBoolean(
                            partes[8].trim()
                    );

            resultado.add(
                    notificacion
            );
        }

        return resultado;
    }

    public static int contarNoLeidas(
            Context context,
            String usuario,
            String rol
    ) {
        int cantidad = 0;

        List<Notificacion> notificaciones =
                obtener(
                        context,
                        usuario,
                        rol
                );

        for (Notificacion notificacion :
                notificaciones) {

            if (!notificacion.leida) {
                cantidad++;
            }
        }

        return cantidad;
    }

    public static void marcarComoLeida(
            Context context,
            String id
    ) {
        modificarLectura(
                context,
                id,
                null,
                null,
                false
        );
    }

    public static void marcarTodasComoLeidas(
            Context context,
            String usuario,
            String rol
    ) {
        modificarLectura(
                context,
                null,
                usuario,
                rol,
                true
        );
    }

    private static void modificarLectura(
            Context context,
            String id,
            String usuario,
            String rol,
            boolean todas
    ) {
        if (context == null) {
            return;
        }

        if (!todas &&
                valorSeguro(id).isEmpty()) {

            return;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NOTIFICACIONES,
                        Context.MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_NOTIFICACIONES,
                        ""
                );

        if (valorSeguro(datos).isEmpty()) {
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

            boolean coincide =
                    partes.length >= 9 &&
                            (
                                    todas
                                            ? correspondeUsuario(
                                            partes,
                                            usuario,
                                            rol
                                    )
                                            : id.equalsIgnoreCase(
                                            partes[0].trim()
                                    )
                            );

            if (coincide) {
                partes[8] =
                        "true";

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
                        KEY_NOTIFICACIONES,
                        actualizados.toString()
                )
                .apply();
    }

    public static void eliminar(
            Context context,
            String id
    ) {
        if (context == null ||
                valorSeguro(id).isEmpty()) {

            return;
        }

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NOTIFICACIONES,
                        Context.MODE_PRIVATE
                );

        String datos =
                preferences.getString(
                        KEY_NOTIFICACIONES,
                        ""
                );

        if (valorSeguro(datos).isEmpty()) {
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

            if (partes.length > 0 &&
                    id.equalsIgnoreCase(
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
                        KEY_NOTIFICACIONES,
                        actualizados.toString()
                )
                .apply();
    }

    private static boolean correspondeUsuario(
            String[] partes,
            String usuario,
            String rol
    ) {
        if (partes == null ||
                partes.length < 9) {

            return false;
        }

        String destinatarioGuardado =
                partes[1].trim();

        String rolGuardado =
                partes[2].trim();

        String usuarioActual =
                valorSeguro(usuario);

        String rolActual =
                valorSeguro(rol);

        if (!rolActual.equalsIgnoreCase(
                rolGuardado
        )) {
            return false;
        }

        /*
         * El asterisco representa una
         * notificación para cualquier usuario
         * que tenga el rol indicado.
         *
         * Se usará principalmente para los
         * avisos dirigidos al Supervisor.
         */
        if ("*".equals(
                destinatarioGuardado
        )) {
            return true;
        }

        return !usuarioActual.isEmpty() &&
                usuarioActual.equalsIgnoreCase(
                        destinatarioGuardado
                );
    }

    private static void agregarRegistro(
            StringBuilder builder,
            String registro
    ) {
        if (builder.length() > 0) {
            builder.append("\n");
        }

        builder.append(
                registro
        );
    }

    private static String unirPartes(
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

            resultado.append(
                    partes[i]
            );
        }

        return resultado.toString();
    }

    private static String limpiar(
            String texto
    ) {
        return valorSeguro(texto)
                .replace("|", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String valorSeguro(
            String valor
    ) {
        return valor == null
                ? ""
                : valor.trim();
    }

    public static class Notificacion {

        public String id = "";
        public String destinatario = "";
        public String rol = "";
        public String titulo = "";
        public String mensaje = "";
        public String fecha = "";
        public String tipo = "";
        public String folio = "";

        public boolean leida = false;
    }
}