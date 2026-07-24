package com.pmp.front;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Muestra notificaciones en la barra del sistema (la que se baja desde arriba)
 * y en la pantalla de bloqueo, SIN Firebase.
 *
 * Limitación: solo aparecen mientras la app está abierta o recién usada
 * (en segundo plano). Si la app se cierra por completo, Android no puede
 * entregarlas sin un servicio push como Firebase.
 */
public final class BarraNotificacionHelper {

    private static final String CANAL_ID = "pmp_avisos";
    private static final String CANAL_NOMBRE = "Avisos de supervisiones";

    private BarraNotificacionHelper() {
    }

    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Notificaciones sobre asignaciones, checklist y reportes.");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    /**
     * Muestra una notificación en la barra del sistema.
     * @param id  identificador único (para que no se repita/pise). Usa el id de la notificación del servidor.
     */
    public static void mostrar(Context context, int id, String titulo, String mensaje) {
        crearCanal(context);

        // Al tocar la notificación:
        //  - Si hay sesión activa -> se abre el panel de notificaciones del usuario.
        //  - Si no hay sesión     -> se abre la pantalla inicial (configuración/login).
        // Antes siempre se abría la pantalla inicial, y por eso parecía que la
        // notificación "mandaba al inicio de sesión".
        android.content.SharedPreferences sesion =
                context.getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE);
        String usuarioActual = sesion.getString("usuario_actual", "");
        boolean haySesion = usuarioActual != null && !usuarioActual.trim().isEmpty();

        Intent intent;
        if (haySesion) {
            intent = new Intent(context, com.pmp.front.activities.NotificacionesActivity.class);
        } else {
            intent = new Intent(context, com.pmp.front.activities.ConfiguracionServidorActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(context, id, intent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CANAL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentTitle(titulo)
                .setContentText(mensaje)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setStyle(new Notification.BigTextStyle().bigText(mensaje));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(id, builder.build());
        }
    }

    /**
     * Recorre las notificaciones y muestra en la barra solo las NO leídas que
     * no se hayan mostrado antes (para no repetirlas en cada refresco).
     */
    public static void mostrarNuevasEnBarra(Context context, java.util.List<NotificacionesHelper.Notificacion> notificaciones) {
        if (notificaciones == null || notificaciones.isEmpty()) return;

        android.content.SharedPreferences prefs =
                context.getSharedPreferences("barra_notif_mostradas", Context.MODE_PRIVATE);
        java.util.Set<String> yaMostradas =
                new java.util.HashSet<>(prefs.getStringSet("ids", new java.util.HashSet<>()));

        java.util.Set<String> nuevasMostradas = new java.util.HashSet<>(yaMostradas);

        for (NotificacionesHelper.Notificacion n : notificaciones) {
            if (n.leida) continue;
            if (n.id == null || n.id.isEmpty()) continue;
            if (yaMostradas.contains(n.id)) continue;

            int idNumerico;
            try {
                idNumerico = Integer.parseInt(n.id);
            } catch (Exception e) {
                idNumerico = n.id.hashCode();
            }

            mostrar(context, idNumerico, n.titulo, n.mensaje);
            nuevasMostradas.add(n.id);
        }

        prefs.edit().putStringSet("ids", nuevasMostradas).apply();
    }
}
