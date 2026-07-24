package com.pmp.front;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Trae las asignaciones/supervisiones directo del servidor (MySQL) y las
 * deja listas en memoria, en el mismo formato de 15 campos separados por "|"
 * que ya usan las pantallas de Calendario, Supervisiones y Dashboard.
 *
 * Ya NO se guarda nada en SharedPreferences: cada pantalla debe volver a
 * llamar a obtenerSupervisiones(...) cada vez que necesite datos frescos
 * (por ejemplo en onResume), y quedarse con el resultado en un campo de la
 * propia Activity mientras la usa.
 *
 * IMPORTANTE: hace una llamada de red, así que SIEMPRE debe invocarse desde
 * un hilo de fondo (nunca desde el hilo principal de la UI).
 */
public class ServidorSyncHelper {

    private static final SimpleDateFormat FORMATO_REGISTRO =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    public static class ResultadoSincronizacion {
        public String registros = "";
        public Map<String, String> horariosPorFolio = new HashMap<>();
        public boolean exito = false;
    }

    public static ResultadoSincronizacion obtenerSupervisiones(String rol, String username) {
        ResultadoSincronizacion resultado = new ResultadoSincronizacion();

        try {
            String urlStr = Config.BASE_URL + "/api/asignaciones/calendario?rol=" + rol;
            if (username != null && !username.isEmpty()) {
                urlStr += "&username=" + username;
            }

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return resultado;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONArray eventos = new JSONArray(sb.toString());
            StringBuilder registros = new StringBuilder();

            for (int i = 0; i < eventos.length(); i++) {
                JSONObject e = eventos.getJSONObject(i);

                String folio = e.optString("folio", "");
                String fecha = e.optString("fecha", "");
                String circuito = "";
                String lugar = e.optString("lugar", "");
                String prioridad = e.optString("prioridad", "");
                String descripcion = e.optString("descripcion", "");
                String observaciones = e.optString("observaciones", "");
                String responsable = e.optString("tecnico", "");
                String usernameResponsable = e.optString("usernameSupervisor", "");
                String apoyos = "";
                String estado = e.optString("estado", "Asignada");
                String checklist = "Pendiente";
                String reporteTecnico = "Pendiente";
                String fechaHoraRegistro = FORMATO_REGISTRO.format(new Date());

                if (registros.length() > 0) {
                    registros.append("\n");
                }

                registros.append(folio).append("|")
                        .append(fecha).append("|")
                        .append(circuito).append("|")
                        .append(lugar).append("|")
                        .append(prioridad).append("|")
                        .append(descripcion).append("|")
                        .append(observaciones).append("|")
                        .append(usernameResponsable).append("|")
                        .append(responsable).append("|")
                        .append("").append("|")
                        .append(apoyos).append("|")
                        .append(estado).append("|")
                        .append(checklist).append("|")
                        .append(reporteTecnico).append("|")
                        .append(fechaHoraRegistro);

                String horaProgramada = e.optString("horaProgramada", "");
                if (!horaProgramada.isEmpty() && !folio.isEmpty()) {
                    resultado.horariosPorFolio.put(folio, horaProgramada);
                }
            }

            resultado.registros = registros.toString();
            resultado.exito = true;
            return resultado;

        } catch (Exception ex) {
            ex.printStackTrace();
            return resultado;
        }
    }

    /**
     * Trae todas las "actividades" libres del calendario (notas del
     * Administrador, ligadas o no a una supervisión) en el mismo formato de
     * 10 campos separados por "|" que ya usan las pantallas de calendario.
     * Devuelve "" si no hay nada o falló la red.
     */
    public static String obtenerActividades() {
        try {
            URL url = new URL(Config.BASE_URL + "/api/actividades");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "";
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONArray lista = new JSONArray(sb.toString());
            StringBuilder registros = new StringBuilder();

            for (int i = 0; i < lista.length(); i++) {
                JSONObject a = lista.getJSONObject(i);

                if (registros.length() > 0) registros.append("\n");

                // 0 idActividad | 1 titulo | 2 fecha | 3 hora | 4 descripcion |
                // 5 prioridad | 6 folio | 7 tecnico | 8 estado | 9 fechaRegistro
                registros.append(a.optString("idActividad", "")).append("|")
                        .append(a.optString("titulo", "")).append("|")
                        .append(a.optString("fecha", "")).append("|")
                        .append(a.optString("hora", "")).append("|")
                        .append(a.optString("descripcion", "")).append("|")
                        .append(a.optString("prioridad", "")).append("|")
                        .append(a.optString("folio", "")).append("|")
                        .append(a.optString("tecnico", "")).append("|")
                        .append(a.optString("estado", "")).append("|")
                        .append(a.optString("fechaRegistro", ""));
            }

            return registros.toString();

        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /** Sube (crea o actualiza) una actividad al servidor. Debe llamarse desde un hilo de fondo. */
    public static boolean guardarActividad(String idActividad, String titulo, String fecha, String hora,
                                            String descripcion, String prioridad, String folio,
                                            String tecnico, String estado, String fechaRegistro) {
        try {
            URL url = new URL(Config.BASE_URL + "/api/actividades/guardar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(8000);

            JSONObject json = new JSONObject();
            json.put("idActividad", idActividad);
            json.put("titulo", titulo);
            json.put("fecha", fecha);
            json.put("hora", hora);
            json.put("descripcion", descripcion);
            json.put("prioridad", prioridad);
            json.put("folio", folio);
            json.put("tecnico", tecnico);
            json.put("estado", estado);
            json.put("fechaRegistro", fechaRegistro);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Elimina una actividad del servidor por su idActividad. Debe llamarse desde un hilo de fondo. */
    public static boolean eliminarActividad(String idActividad) {
        try {
            URL url = new URL(Config.BASE_URL + "/api/actividades/" + idActividad);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(8000);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
