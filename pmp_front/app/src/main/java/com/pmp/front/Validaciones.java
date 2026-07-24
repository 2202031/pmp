package com.pmp.front;

import android.widget.EditText;

/**
 * Validaciones de entrada alineadas con el esquema real de la base de datos.
 *
 * Cada método devuelve null si el valor es válido, o un mensaje explicando
 * al usuario qué está mal y qué debe escribir.
 *
 * Límites tomados de la BD (pmp_back):
 *   usuarios:          nombre/rpe/username/contraseña varchar(255) NOT NULL
 *                      email/telefono/zona varchar(255) NULL
 *   asigar_super:      folio varchar(50) NOT NULL UNIQUE, fecha DATE NOT NULL,
 *                      referencia(lugar) varchar(200) NOT NULL,
 *                      prioridad varchar(100), descripcion varchar(100),
 *                      hora_programada varchar(10), observaciones TEXT
 *   creacion_reporte:  anio_notificacion INT, kwh FLOAT, importe DECIMAL(38,2),
 *                      rpu/numero_corte/tarifa/status_servicio varchar(100)
 */
public final class Validaciones {

    private Validaciones() {
    }

    // ---------- Utilidades ----------

    public static String texto(EditText campo) {
        return campo == null || campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    /** Marca el error en el campo y le da el foco. Devuelve siempre false (validación fallida). */
    public static boolean marcarError(EditText campo, String mensaje) {
        if (campo != null) {
            campo.setError(mensaje);
            campo.requestFocus();
        }
        return false;
    }

    // ---------- Genéricas ----------

    /** Obligatorio y con longitud máxima. */
    public static String obligatorio(String valor, String nombreCampo, int maxLongitud) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Escribe " + nombreCampo + ". Este campo es obligatorio.";
        }
        if (valor.trim().length() > maxLongitud) {
            return nombreCampo + " es demasiado largo (máximo " + maxLongitud + " caracteres). Actualmente tiene " + valor.trim().length() + ".";
        }
        return null;
    }

    /** Opcional, pero si se escribe algo respeta la longitud máxima. */
    public static String opcional(String valor, String nombreCampo, int maxLongitud) {
        if (valor == null || valor.trim().isEmpty()) return null;
        if (valor.trim().length() > maxLongitud) {
            return nombreCampo + " es demasiado largo (máximo " + maxLongitud + " caracteres). Actualmente tiene " + valor.trim().length() + ".";
        }
        return null;
    }

    // ---------- Numéricas (según tipo de columna) ----------

    /** Columna INT: solo dígitos, sin puntos, guiones ni letras. */
    public static String entero(String valor, String nombreCampo, boolean obligatorio) {
        if (valor == null || valor.trim().isEmpty()) {
            return obligatorio ? "Escribe " + nombreCampo + ". Este campo es obligatorio." : null;
        }
        String v = valor.trim();
        if (!v.matches("\\d+")) {
            return nombreCampo + " debe ser un número entero (solo dígitos, sin guiones, puntos ni letras). Ejemplo: 2868138";
        }
        try {
            long n = Long.parseLong(v);
            if (n > Integer.MAX_VALUE) {
                return nombreCampo + " es demasiado grande. El valor máximo permitido es " + Integer.MAX_VALUE + ".";
            }
        } catch (Exception e) {
            return nombreCampo + " debe ser un número entero válido.";
        }
        return null;
    }

    /** Columna FLOAT/DECIMAL: dígitos con punto decimal opcional. */
    public static String decimal(String valor, String nombreCampo, boolean obligatorio) {
        if (valor == null || valor.trim().isEmpty()) {
            return obligatorio ? "Escribe " + nombreCampo + ". Este campo es obligatorio." : null;
        }
        String v = valor.trim().replace(",", ".");
        if (!v.matches("\\d+(\\.\\d+)?")) {
            return nombreCampo + " debe ser un número (usa punto para decimales, sin letras ni símbolos). Ejemplo: 115104.5";
        }
        return null;
    }

    // ---------- Específicas del proyecto ----------

    /**
     * Folio: obligatorio, máximo 50, sin espacios (se usa dentro de URLs y como clave única).
     */
    public static String folio(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Escribe el folio. Este campo es obligatorio.";
        }
        String v = valor.trim();
        if (v.length() > 50) {
            return "El folio es demasiado largo (máximo 50 caracteres). Actualmente tiene " + v.length() + ".";
        }
        if (v.contains(" ")) {
            return "El folio no puede tener espacios. Usa guiones, por ejemplo: SUP-14072026";
        }
        if (!v.matches("[A-Za-z0-9\\-_]+")) {
            return "El folio solo admite letras, números, guion (-) y guion bajo (_). Ejemplo: SUP-14072026";
        }
        return null;
    }

    /** Nombre de usuario: obligatorio, sin espacios, máximo 255. */
    public static String username(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Escribe el nombre de usuario. Este campo es obligatorio.";
        }
        String v = valor.trim();
        if (v.contains(" ")) {
            return "El nombre de usuario no puede tener espacios. Ejemplo: juan.perez";
        }
        if (v.length() > 255) {
            return "El nombre de usuario es demasiado largo (máximo 255 caracteres).";
        }
        if (!v.matches("[A-Za-z0-9._\\-]+")) {
            return "El nombre de usuario solo admite letras, números, punto (.), guion (-) y guion bajo (_). Ejemplo: juan.perez";
        }
        return null;
    }

    /** Contraseña: obligatoria, mínimo 4 caracteres. */
    public static String password(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "Escribe la contraseña. Este campo es obligatorio.";
        }
        if (valor.length() < 4) {
            return "La contraseña es muy corta. Debe tener al menos 4 caracteres.";
        }
        if (valor.length() > 255) {
            return "La contraseña es demasiado larga (máximo 255 caracteres).";
        }
        return null;
    }

    /** Correo: opcional en la BD, pero si se escribe debe tener formato válido. */
    public static String email(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null; // es opcional
        String v = valor.trim();
        if (v.length() > 255) {
            return "El correo es demasiado largo (máximo 255 caracteres).";
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches()) {
            return "El correo no tiene un formato válido. Ejemplo: nombre@dominio.com";
        }
        return null;
    }

    /** Teléfono: opcional, solo dígitos, 10 posiciones típicas en México. */
    public static String telefono(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null; // es opcional
        String v = valor.trim().replaceAll("[\\s\\-()]", "");
        if (!v.matches("\\d+")) {
            return "El teléfono solo admite números. Ejemplo: 5512345678";
        }
        if (v.length() < 10 || v.length() > 15) {
            return "El teléfono debe tener entre 10 y 15 dígitos. Actualmente tiene " + v.length() + ".";
        }
        return null;
    }

    /** Fecha en formato dd/MM/yyyy. */
    public static String fecha(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Selecciona la fecha. Este campo es obligatorio.";
        }
        String v = valor.trim();
        if (!v.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return "La fecha debe tener el formato dd/mm/aaaa. Ejemplo: 14/07/2026";
        }
        return null;
    }

    /** Hora en formato HH:mm (opcional). */
    public static String hora(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null; // opcional
        String v = valor.trim();
        if (!v.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            return "La hora debe tener el formato HH:mm en 24 horas. Ejemplo: 14:30";
        }
        return null;
    }

    /** Dirección IP o URL del servidor. */
    public static String direccionServidor(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Escribe la dirección IP del servidor. Ejemplo: 192.168.1.75";
        }
        String v = valor.trim().replaceFirst("^https?://", "");
        // Quitamos el puerto si lo trae para validar solo el host.
        String host = v.contains(":") ? v.substring(0, v.indexOf(':')) : v;
        String puerto = v.contains(":") ? v.substring(v.indexOf(':') + 1) : "";

        if (!puerto.isEmpty() && !puerto.matches("\\d{1,5}")) {
            return "El puerto debe ser un número. Ejemplo: 192.168.1.75:8080";
        }

        // Si parece IP (solo dígitos y puntos), validamos cada octeto.
        if (host.matches("[\\d.]+")) {
            String[] partes = host.split("\\.");
            if (partes.length != 4) {
                return "La dirección IP debe tener 4 números separados por puntos. Ejemplo: 192.168.1.75";
            }
            for (String p : partes) {
                if (p.isEmpty() || !p.matches("\\d{1,3}")) {
                    return "La dirección IP no es válida. Ejemplo: 192.168.1.75";
                }
                int n = Integer.parseInt(p);
                if (n > 255) {
                    return "Cada número de la IP debe estar entre 0 y 255. Revisa el valor " + p + ".";
                }
            }
            return null;
        }

        // Si no es IP, aceptamos un nombre de host razonable.
        if (!host.matches("[A-Za-z0-9.\\-]+")) {
            return "La dirección no es válida. Escribe una IP como 192.168.1.75 o un nombre de servidor.";
        }
        return null;
    }
}
