package com.pmp.front;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuración central de red de la app.
 *
 * La dirección del servidor ya NO está fija en el código: se guarda en el
 * dispositivo y se configura desde la pantalla "Configuración de servidor"
 * (ConfiguracionServidorActivity) antes de iniciar sesión.
 */
public final class Config {

    private static final String PREFS = "config_servidor";
    private static final String KEY_BASE_URL = "base_url";

    // Valor por defecto (emulador). En teléfono físico se sobreescribe.
    private static final String DEFAULT_URL = "http://10.0.2.2:8080";

    // Compatibilidad con el código que usa Config.BASE_URL directo.
    public static String BASE_URL = DEFAULT_URL;

    private Config() {
    }

    public static void init(Context context) {
        BASE_URL = getBaseUrl(context);
    }

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String guardada = prefs.getString(KEY_BASE_URL, "");
        String url = (guardada == null || guardada.trim().isEmpty()) ? DEFAULT_URL : guardada.trim();
        BASE_URL = url;
        return url;
    }

    public static boolean hayUrlConfigurada(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String guardada = prefs.getString(KEY_BASE_URL, "");
        return guardada != null && !guardada.trim().isEmpty();
    }

    public static void guardarBaseUrl(Context context, String entrada) {
        String url = normalizar(entrada);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, url).apply();
        BASE_URL = url;
    }

    public static void limpiar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_BASE_URL).apply();
        BASE_URL = DEFAULT_URL;
    }

    public static String normalizar(String entrada) {
        if (entrada == null) return DEFAULT_URL;
        String v = entrada.trim();
        if (v.isEmpty()) return DEFAULT_URL;

        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            v = "http://" + v;
        }

        String sinEsquema = v.replaceFirst("^https?://", "");
        if (!sinEsquema.contains(":")) {
            v = v + ":8080";
        }

        if (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
