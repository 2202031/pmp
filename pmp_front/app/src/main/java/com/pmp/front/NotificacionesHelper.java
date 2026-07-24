package com.pmp.front;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NotificacionesHelper {

    private static String baseUrl() { return Config.BASE_URL + "/api/notificaciones"; }

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private NotificacionesHelper() {
    }

    public interface NotificacionesCallback {
        void onSuccess(List<Notificacion> notificaciones);
        void onError(Exception e);
    }

    public interface OperacionCallback {
        void onCompletado(boolean exito);
    }

    public static void crear(
            Context context,
            String destinatario,
            String rol,
            String titulo,
            String mensaje,
            String tipo,
            String folio,
            OperacionCallback callback
    ) {
        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl() + "/crear");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("username", destinatario);
                json.put("rolUsuario", rol);
                json.put("titulo", titulo);
                json.put("mensaje", mensaje);
                json.put("tipo", tipo);
                json.put("folio", folio);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                boolean exito = conn.getResponseCode() == HttpURLConnection.HTTP_CREATED ||
                        conn.getResponseCode() == HttpURLConnection.HTTP_OK;

                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(exito));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(false));
                }
            }
        });
    }

    public static void obtener(
            Context context,
            String usuario,
            String rol,
            NotificacionesCallback callback
    ) {
        if (callback == null) return;

        executorService.execute(() -> {
            List<Notificacion> resultado = new ArrayList<>();
            try {
                URL url = new URL(baseUrl() + "?username=" + usuario + "&rol=" + rol);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String linea; // CORREGIDO: Nombre unificado de la variable de lectura
                    while ((linea = br.readLine()) != null) {
                        sb.append(linea.trim());
                    }

                    JSONArray array = new JSONArray(sb.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Notificacion notif = new Notificacion();
                        notif.id = String.valueOf(obj.optInt("id"));
                        notif.destinatario = obj.optString("emailUsuario", "");
                        notif.rol = obj.optString("rolUsuario", "");
                        notif.titulo = obj.optString("titulo", "");
                        notif.mensaje = obj.optString("mensaje", "");
                        notif.fecha = obj.optString("fechaFormateada", "");
                        notif.tipo = obj.optString("tipo", "");
                        notif.folio = obj.optString("folio", "");
                        notif.leida = obj.optBoolean("leida", false);
                        resultado.add(notif);
                    }
                    mainHandler.post(() -> callback.onSuccess(resultado)); // CORREGIDO: Typo corregido a onSuccess
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Error de respuesta del servidor")));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void marcarComoLeida(Context context, String id, OperacionCallback callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl() + "/" + id + "/leer");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");

                boolean exito = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(exito));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(false));
                }
            }
        });
    }

    public static void marcarTodasComoLeidas(Context context, String usuario, String rol, OperacionCallback callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl() + "/leer-todas?username=" + usuario + "&rol=" + rol);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");

                boolean exito = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(exito));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(false));
                }
            }
        });
    }

    public static void eliminar(Context context, String id, OperacionCallback callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(baseUrl() + "/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                boolean exito = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(exito));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mainHandler.post(() -> callback.onCompletado(false));
                }
            }
        });
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