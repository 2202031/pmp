package com.pmp.front.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.widget.EditText;
import android.widget.TextView;

import com.pmp.front.Config;
import com.pmp.front.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pantalla que se muestra antes del login para configurar a qué servidor
 * (backend) se conectará la app dentro de la red WiFi.
 *
 * La detección automática NO asume máscara /24: lee la máscara real que el
 * DHCP asignó al teléfono y calcula el rango verdadero de la subred.
 * Como una red grande (/16 = 65,534 hosts) es impráctica de escanear completa
 * desde un móvil, el escaneo se hace por prioridad:
 *   1) La puerta de enlace (gateway).
 *   2) El /24 propio del teléfono (lo más probable).
 *   3) El resto de la subred, en bloques, hasta un tope razonable.
 * Además se escanea en paralelo para que sea rápido.
 */
public class ConfiguracionServidorActivity extends Activity {

    private EditText etIpServidor;
    private TextView btnAutodetectar;
    private TextView btnGuardarServidor;
    private TextView txtEstadoDeteccion;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Hilos simultáneos para probar direcciones. */
    private static final int HILOS = 48;

    /** Tope de direcciones a probar, para no dejar el teléfono escaneando horas. */
    private static final int MAX_DIRECCIONES = 4096;

    private static final int PUERTO = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Config.init(this);

        boolean forzarConfig = getIntent().getBooleanExtra("forzar_config", false);
        if (Config.hayUrlConfigurada(this) && !forzarConfig) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_configuracion_servidor);

        etIpServidor = findViewById(R.id.etIpServidor);
        btnAutodetectar = findViewById(R.id.btnAutodetectar);
        btnGuardarServidor = findViewById(R.id.btnGuardarServidor);
        txtEstadoDeteccion = findViewById(R.id.txtEstadoDeteccion);

        if (Config.hayUrlConfigurada(this)) {
            String actual = Config.getBaseUrl(this)
                    .replaceFirst("^https?://", "")
                    .replaceFirst(":" + PUERTO + "$", "");
            etIpServidor.setText(actual);
        }

        btnGuardarServidor.setOnClickListener(v -> guardarYContinuar());
        btnAutodetectar.setOnClickListener(v -> autodetectar());
    }

    private void guardarYContinuar() {
        String entrada = etIpServidor.getText().toString().trim();

        String error = com.pmp.front.Validaciones.direccionServidor(entrada);
        if (error != null) {
            etIpServidor.setError(error);
            etIpServidor.requestFocus();
            return;
        }

        Config.guardarBaseUrl(this, entrada);

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // =====================================================================
    //  DETECCIÓN AUTOMÁTICA
    // =====================================================================

    private void autodetectar() {
        DatosRed red = leerDatosRed();
        if (red == null) {
            txtEstadoDeteccion.setText("No se pudo leer la red WiFi. Revisa que estés conectado o escribe la IP a mano.");
            return;
        }

        List<String> candidatas = construirListaCandidatas(red);
        if (candidatas.isEmpty()) {
            txtEstadoDeteccion.setText("No se pudo calcular el rango de la red. Escribe la IP a mano.");
            return;
        }

        btnAutodetectar.setEnabled(false);
        txtEstadoDeteccion.setText("Buscando en " + candidatas.size()
                + " direcciones (máscara /" + red.prefijo + ")...");

        executor.execute(() -> escanear(candidatas, red));
    }

    private void escanear(List<String> candidatas, DatosRed red) {
        final AtomicBoolean encontrado = new AtomicBoolean(false);
        final AtomicInteger indice = new AtomicInteger(0);
        final AtomicInteger probadas = new AtomicInteger(0);
        final int total = candidatas.size();

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);

        for (int h = 0; h < HILOS; h++) {
            pool.execute(() -> {
                while (!encontrado.get()) {
                    int i = indice.getAndIncrement();
                    if (i >= total) return;

                    String ip = candidatas.get(i);
                    if (respondeBackend(ip)) {
                        if (encontrado.compareAndSet(false, true)) {
                            mainHandler.post(() -> {
                                etIpServidor.setText(ip);
                                etIpServidor.setError(null);
                                txtEstadoDeteccion.setText("Servidor encontrado en " + ip);
                                btnAutodetectar.setEnabled(true);
                            });
                        }
                        return;
                    }

                    int hechas = probadas.incrementAndGet();
                    if (hechas % 50 == 0 && !encontrado.get()) {
                        mainHandler.post(() -> txtEstadoDeteccion.setText(
                                "Buscando... (" + hechas + "/" + total + ")"));
                    }
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(3, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (!encontrado.get()) {
            final String aviso = red.subredRecortada
                    ? "No se encontró el servidor en las primeras " + total
                      + " direcciones. La red es muy grande (/" + red.prefijo
                      + "); escribe la IP a mano."
                    : "No se encontró el servidor en la red. Verifica que el backend esté encendido y escribe la IP a mano.";

            mainHandler.post(() -> {
                txtEstadoDeteccion.setText(aviso);
                btnAutodetectar.setEnabled(true);
            });
        }
    }

    /**
     * Construye la lista de direcciones a probar, ordenada por probabilidad:
     * gateway, luego el /24 del teléfono, luego el resto de la subred real.
     */
    private List<String> construirListaCandidatas(DatosRed red) {
        List<String> lista = new ArrayList<>();
        java.util.Set<Long> agregadas = new java.util.HashSet<>();

        // 1) La puerta de enlace (a veces el servidor vive ahí o cerca).
        if (red.gateway != 0) {
            agregar(lista, agregadas, red.gateway, red);
        }

        // 2) El /24 propio del teléfono: es lo más probable.
        long baseMiBloque = red.ip & 0xFFFFFF00L;
        for (int i = 1; i <= 254; i++) {
            agregar(lista, agregadas, baseMiBloque + i, red);
        }

        // 3) El resto de la subred real (según la máscara), en orden,
        //    hasta el tope para no eternizar el escaneo.
        for (long dir = red.primeraDireccion; dir <= red.ultimaDireccion; dir++) {
            if (lista.size() >= MAX_DIRECCIONES) {
                red.subredRecortada = true;
                break;
            }
            agregar(lista, agregadas, dir, red);
        }

        return lista;
    }

    private void agregar(List<String> lista, java.util.Set<Long> agregadas, long dir, DatosRed red) {
        if (lista.size() >= MAX_DIRECCIONES) {
            red.subredRecortada = true;
            return;
        }
        // Solo direcciones válidas dentro de la subred y distintas de la propia.
        if (dir < red.primeraDireccion || dir > red.ultimaDireccion) return;
        if (dir == red.ip) return;
        if (!agregadas.add(dir)) return;
        lista.add(aTexto(dir));
    }

    private boolean respondeBackend(String ip) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + ip + ":" + PUERTO + "/api/usuarios");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(400);
            conn.setReadTimeout(400);
            // Cualquier respuesta HTTP significa que algo escucha en ese puerto.
            return conn.getResponseCode() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // =====================================================================
    //  LECTURA DE LA RED (IP, MÁSCARA, GATEWAY)
    // =====================================================================

    private static class DatosRed {
        long ip;
        long mascara;
        long gateway;
        int prefijo;            // bits de red (ej. 24, 16)
        long primeraDireccion;  // primer host utilizable
        long ultimaDireccion;   // último host utilizable
        boolean subredRecortada; // true si tuvimos que limitar el escaneo
    }

    private DatosRed leerDatosRed() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) return null;

            DhcpInfo dhcp = wifi.getDhcpInfo();
            int ipInt = wifi.getConnectionInfo().getIpAddress();
            if (ipInt == 0 && dhcp != null) ipInt = dhcp.ipAddress;
            if (ipInt == 0) return null;

            DatosRed red = new DatosRed();
            red.ip = aLong(ipInt);

            // La máscara real asignada por DHCP. Si no viene, asumimos /24
            // como último recurso (y se avisa en pantalla el prefijo usado).
            long mascara = (dhcp != null && dhcp.netmask != 0)
                    ? aLong(dhcp.netmask)
                    : 0xFFFFFF00L;

            // Algunos equipos reportan la máscara con los bytes invertidos.
            // Una máscara válida debe ser un bloque continuo de 1s.
            if (!mascaraValida(mascara)) {
                mascara = 0xFFFFFF00L;
            }

            red.mascara = mascara;
            red.gateway = (dhcp != null) ? aLong(dhcp.gateway) : 0;
            red.prefijo = Long.bitCount(mascara);

            long direccionRed = red.ip & mascara;
            long broadcast = direccionRed | (~mascara & 0xFFFFFFFFL);

            red.primeraDireccion = direccionRed + 1;
            red.ultimaDireccion = broadcast - 1;

            if (red.ultimaDireccion < red.primeraDireccion) return null;

            return red;
        } catch (Exception e) {
            return null;
        }
    }

    /** Una máscara válida es 1s continuos seguidos de 0s (ej. 255.255.255.0). */
    private boolean mascaraValida(long mascara) {
        long invertida = ~mascara & 0xFFFFFFFFL;
        return ((invertida + 1) & invertida) == 0;
    }

    /**
     * Android entrega la IP en orden de bytes invertido (little-endian).
     * Formatter.formatIpAddress ya lo maneja; aquí lo pasamos a un long
     * en orden normal para poder hacer las operaciones de bits.
     */
    private long aLong(int ipAndroid) {
        String texto = Formatter.formatIpAddress(ipAndroid); // "192.168.1.34"
        String[] partes = texto.split("\\.");
        if (partes.length != 4) return 0;
        long valor = 0;
        for (String p : partes) {
            valor = (valor << 8) | (Integer.parseInt(p) & 0xFF);
        }
        return valor;
    }

    private String aTexto(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }
}
