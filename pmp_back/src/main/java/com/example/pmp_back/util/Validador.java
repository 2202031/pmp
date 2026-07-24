package com.example.pmp_back.util;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Validaciones del lado del servidor, alineadas con el esquema real de pmp_back.
 *
 * El frontend ya valida para ayudar al usuario, pero el backend NO debe confiar
 * en eso: aquí se revisa de nuevo antes de tocar la base de datos.
 *
 * Cada método devuelve null si el valor es válido, o un mensaje de error.
 */
public final class Validador {

    private Validador() {
    }

    // ---------- Lectura segura del cuerpo de la petición ----------

    /** Devuelve el valor como texto recortado, o "" si viene nulo. */
    public static String texto(Map<String, Object> datos, String clave) {
        if (datos == null) return "";
        Object v = datos.get(clave);
        return v == null ? "" : v.toString().trim();
    }

    // ---------- Genéricas ----------

    public static String obligatorio(String valor, String nombreCampo, int maxLongitud) {
        if (valor == null || valor.trim().isEmpty()) {
            return "El campo '" + nombreCampo + "' es obligatorio.";
        }
        if (valor.trim().length() > maxLongitud) {
            return "El campo '" + nombreCampo + "' excede el máximo de " + maxLongitud
                    + " caracteres (recibido: " + valor.trim().length() + ").";
        }
        return null;
    }

    public static String opcional(String valor, String nombreCampo, int maxLongitud) {
        if (valor == null || valor.trim().isEmpty()) return null;
        if (valor.trim().length() > maxLongitud) {
            return "El campo '" + nombreCampo + "' excede el máximo de " + maxLongitud
                    + " caracteres (recibido: " + valor.trim().length() + ").";
        }
        return null;
    }

    // ---------- Numéricas (según tipo de columna) ----------

    /** Columna INT. Devuelve mensaje de error si no es un entero válido. */
    public static String validarEntero(String valor, String nombreCampo, boolean obligatorio) {
        if (valor == null || valor.trim().isEmpty()) {
            return obligatorio ? "El campo '" + nombreCampo + "' es obligatorio." : null;
        }
        if (!valor.trim().matches("-?\\d+")) {
            return "El campo '" + nombreCampo + "' debe ser un número entero (solo dígitos). Valor recibido: '" + valor + "'.";
        }
        try {
            Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return "El campo '" + nombreCampo + "' está fuera del rango permitido para un entero. Valor recibido: '" + valor + "'.";
        }
        return null;
    }

    /** Columna FLOAT. */
    public static String validarDecimal(String valor, String nombreCampo, boolean obligatorio) {
        if (valor == null || valor.trim().isEmpty()) {
            return obligatorio ? "El campo '" + nombreCampo + "' es obligatorio." : null;
        }
        String v = valor.trim().replace(",", ".");
        if (!v.matches("-?\\d+(\\.\\d+)?")) {
            return "El campo '" + nombreCampo + "' debe ser un número (usa punto para decimales). Valor recibido: '" + valor + "'.";
        }
        return null;
    }

    // ---------- Conversores seguros (después de validar) ----------

    /** Convierte a Integer; devuelve null si no se puede (no lanza excepción). */
    public static Integer aEntero(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Convierte a Float; devuelve null si no se puede. */
    public static Float aFloat(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        try {
            return Float.parseFloat(valor.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    /** Convierte a BigDecimal; devuelve null si no se puede. */
    public static BigDecimal aBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        try {
            return new BigDecimal(valor.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Específicas del proyecto ----------

    /** folio VARCHAR(50) NOT NULL UNIQUE: sin espacios, solo letras/números/-/_ */
    public static String validarFolio(String valor) {
        String base = obligatorio(valor, "folio", 50);
        if (base != null) return base;
        String v = valor.trim();
        if (!v.matches("[A-Za-z0-9\\-_]+")) {
            return "El campo 'folio' solo admite letras, números, guion (-) y guion bajo (_). Valor recibido: '" + valor + "'.";
        }
        return null;
    }

    /** username VARCHAR(255) NOT NULL UNIQUE: sin espacios. */
    public static String validarUsername(String valor) {
        String base = obligatorio(valor, "username", 255);
        if (base != null) return base;
        if (valor.trim().contains(" ")) {
            return "El campo 'username' no puede contener espacios. Valor recibido: '" + valor + "'.";
        }
        return null;
    }

    /** email VARCHAR(255) NULL: si viene, debe tener formato válido. */
    public static String validarEmail(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        String base = opcional(valor, "email", 255);
        if (base != null) return base;
        if (!valor.trim().matches("^[\\w.+\\-]+@[\\w\\-]+\\.[\\w\\-.]+$")) {
            return "El campo 'email' no tiene un formato válido. Valor recibido: '" + valor + "'.";
        }
        return null;
    }

    /** telefono VARCHAR(255) NULL: si viene, solo dígitos (10 a 15). */
    public static String validarTelefono(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        String v = valor.trim().replaceAll("[\\s\\-()]", "");
        if (!v.matches("\\d+")) {
            return "El campo 'telefono' solo admite dígitos. Valor recibido: '" + valor + "'.";
        }
        if (v.length() < 10 || v.length() > 15) {
            return "El campo 'telefono' debe tener entre 10 y 15 dígitos (recibido: " + v.length() + ").";
        }
        return null;
    }

    /** Fecha en formato dd/MM/yyyy. */
    public static String validarFecha(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "El campo 'fecha' es obligatorio.";
        }
        if (!valor.trim().matches("\\d{2}/\\d{2}/\\d{4}")) {
            return "El campo 'fecha' debe tener el formato dd/MM/yyyy. Valor recibido: '" + valor + "'.";
        }
        return null;
    }
}
