package com.pmp.front.activities.tecnico;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pmp.front.R;
import com.pmp.front.activities.LoginActivity;

import java.io.File;
import java.io.IOException;

public class EvidenciasSupervisionTecnicoActivity
        extends Activity {

    private static final String PREFS_SESION =
            "sesion_usuario";

    private static final String KEY_USUARIO =
            "usuario_actual";

    private static final String KEY_ROL =
            "rol_actual";

    private static final String PREFS_SUPERVISIONES =
            "supervisiones_local";

    private static final String KEY_SUPERVISIONES =
            "supervisiones";

    private static final String PREFS_EVIDENCIAS =
            "evidencias_local";

    private static final int REQUEST_CAMARA = 3101;
    private static final int REQUEST_GALERIA = 3102;

    private static final String[] SUFIJOS = {
            "foto_corte",
            "foto_fachada",
            "foto_medidor",
            "foto_selfi"
    };

    private static final String[] NOMBRES = {
            "Foto del corte",
            "Foto de fachada",
            "Foto del medidor asegurado",
            "Foto selfi"
    };

    private TextView btnVolver;
    private TextView btnVolverReporte;

    private TextView txtFolioEvidencias;
    private TextView txtContadorEvidencias;
    private TextView txtModoEvidencias;

    private ImageView[] imagenes;
    private TextView[] estados;
    private TextView[] botones;

    private String folio;
    private String usuarioActual;

    private boolean modoEdicion = true;

    private int evidenciaSeleccionada = -1;

    private String rutaCamaraPendiente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_evidencias_supervision_tecnico
        );

        inicializarVistas();

        if (!cargarSesion()) {
            return;
        }

        folio = getIntent().getStringExtra("folio");

        if (folio == null || folio.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No se encontró el folio",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        if (savedInstanceState != null) {
            evidenciaSeleccionada =
                    savedInstanceState.getInt(
                            "evidenciaSeleccionada",
                            -1
                    );

            rutaCamaraPendiente =
                    savedInstanceState.getString(
                            "rutaCamaraPendiente",
                            ""
                    );
        }

        txtFolioEvidencias.setText(folio);

        if (!cargarEstadoSupervision()) {
            return;
        }

        configurarEventos();
        cargarEvidencias();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {
        super.onSaveInstanceState(outState);

        outState.putInt(
                "evidenciaSeleccionada",
                evidenciaSeleccionada
        );

        outState.putString(
                "rutaCamaraPendiente",
                rutaCamaraPendiente
        );
    }

    private void inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolver);

        btnVolverReporte =
                findViewById(R.id.btnVolverReporte);

        txtFolioEvidencias =
                findViewById(R.id.txtFolioEvidencias);

        txtContadorEvidencias =
                findViewById(R.id.txtContadorEvidencias);

        txtModoEvidencias =
                findViewById(R.id.txtModoEvidencias);

        imagenes = new ImageView[]{
                findViewById(R.id.imgFotoCorte),
                findViewById(R.id.imgFotoFachada),
                findViewById(R.id.imgFotoMedidor),
                findViewById(R.id.imgFotoSelfi)
        };

        estados = new TextView[]{
                findViewById(R.id.txtEstadoFotoCorte),
                findViewById(R.id.txtEstadoFotoFachada),
                findViewById(R.id.txtEstadoFotoMedidor),
                findViewById(R.id.txtEstadoFotoSelfi)
        };

        botones = new TextView[]{
                findViewById(R.id.btnFotoCorte),
                findViewById(R.id.btnFotoFachada),
                findViewById(R.id.btnFotoMedidor),
                findViewById(R.id.btnFotoSelfi)
        };
    }

    private boolean cargarSesion() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SESION,
                        MODE_PRIVATE
                );

        usuarioActual = preferences.getString(
                KEY_USUARIO,
                ""
        );

        String rol = preferences.getString(
                KEY_ROL,
                ""
        );

        if (usuarioActual == null ||
                usuarioActual.trim().isEmpty() ||
                !"tecnico".equalsIgnoreCase(rol)) {

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

    private boolean cargarEstadoSupervision() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_SUPERVISIONES,
                        MODE_PRIVATE
                );

        String datos = preferences.getString(
                KEY_SUPERVISIONES,
                ""
        );

        if (datos == null || datos.trim().isEmpty()) {
            mostrarError();
            return false;
        }

        String[] registros = datos.split("\n");

        for (String registro : registros) {
            if (registro.trim().isEmpty()) {
                continue;
            }

            String[] partes =
                    registro.split("\\|", -1);

            if (partes.length < 15) {
                continue;
            }

            if (!folio.equalsIgnoreCase(
                    partes[0].trim()
            )) {
                continue;
            }

            if (!usuarioActual.equalsIgnoreCase(
                    partes[7].trim()
            )) {
                mostrarError();
                return false;
            }

            String estadoGeneral = partes[11];
            String estadoReporte = partes[13];

            modoEdicion =
                    !"Pendiente de revisión".equalsIgnoreCase(
                            estadoGeneral
                    ) &&
                            !"Finalizada".equalsIgnoreCase(
                                    estadoGeneral
                            ) &&
                            !"Enviado".equalsIgnoreCase(
                                    estadoReporte
                            ) &&
                            !"Validado".equalsIgnoreCase(
                                    estadoReporte
                            );

            if (modoEdicion) {
                txtModoEvidencias.setText(
                        "Puedes agregar, reemplazar o eliminar fotografías."
                );
            } else {
                txtModoEvidencias.setText(
                        "Evidencias en modo de consulta."
                );
            }

            return true;
        }

        mostrarError();
        return false;
    }

    private void configurarEventos() {
        btnVolver.setOnClickListener(v -> finish());

        btnVolverReporte.setOnClickListener(v ->
                finish()
        );

        for (int i = 0; i < botones.length; i++) {
            final int indice = i;

            botones[i].setOnClickListener(v ->
                    abrirOpcionesEvidencia(indice)
            );

            imagenes[i].setOnClickListener(v -> {
                if (modoEdicion) {
                    abrirOpcionesEvidencia(indice);
                }
            });
        }
    }

    private void abrirOpcionesEvidencia(
            int indice
    ) {
        if (!modoEdicion) {
            Toast.makeText(
                    this,
                    "Las evidencias están en modo de consulta",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String ruta = obtenerRuta(indice);

        String[] opciones;

        if (ruta.isEmpty()) {
            opciones = new String[]{
                    "Tomar fotografía",
                    "Elegir de galería"
            };
        } else {
            opciones = new String[]{
                    "Tomar nueva fotografía",
                    "Elegir otra de galería",
                    "Eliminar evidencia"
            };
        }

        new AlertDialog.Builder(this)
                .setTitle(NOMBRES[indice])
                .setItems(
                        opciones,
                        (dialog, which) -> {
                            if (which == 0) {
                                abrirCamara(indice);
                            } else if (which == 1) {
                                abrirGaleria(indice);
                            } else {
                                confirmarEliminar(indice);
                            }
                        }
                )
                .show();
    }

    private void abrirCamara(int indice) {
        Intent intent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        if (intent.resolveActivity(
                getPackageManager()
        ) == null) {

            Toast.makeText(
                    this,
                    "No se encontró una aplicación de cámara",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {
            File archivo = crearArchivoImagen(indice);

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            );

            evidenciaSeleccionada = indice;
            rutaCamaraPendiente =
                    archivo.getAbsolutePath();

            intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    uri
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivityForResult(
                    intent,
                    REQUEST_CAMARA
            );
        } catch (IOException exception) {
            Toast.makeText(
                    this,
                    "No se pudo preparar la fotografía",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void abrirGaleria(int indice) {
        evidenciaSeleccionada = indice;

        Intent intent = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("image/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                REQUEST_GALERIA
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == REQUEST_CAMARA) {
            procesarResultadoCamara(resultCode);
            return;
        }

        if (requestCode == REQUEST_GALERIA) {
            procesarResultadoGaleria(
                    resultCode,
                    data
            );
        }
    }

    private void procesarResultadoCamara(
            int resultCode
    ) {
        if (evidenciaSeleccionada < 0 ||
                rutaCamaraPendiente.isEmpty()) {
            return;
        }

        if (resultCode == RESULT_OK) {
            guardarRuta(
                    evidenciaSeleccionada,
                    rutaCamaraPendiente
            );

            Toast.makeText(
                    this,
                    "Fotografía guardada",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            File archivo =
                    new File(rutaCamaraPendiente);

            if (archivo.exists()) {
                archivo.delete();
            }
        }

        evidenciaSeleccionada = -1;
        rutaCamaraPendiente = "";

        cargarEvidencias();
    }

    private void procesarResultadoGaleria(
            int resultCode,
            Intent data
    ) {
        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null ||
                evidenciaSeleccionada < 0) {

            evidenciaSeleccionada = -1;
            return;
        }

        Uri uri = data.getData();

        try {
            int permisos =
                    data.getFlags() & (
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

            getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            permisos
                    );
        } catch (SecurityException ignored) {
        }

        guardarRuta(
                evidenciaSeleccionada,
                uri.toString()
        );

        evidenciaSeleccionada = -1;

        cargarEvidencias();

        Toast.makeText(
                this,
                "Evidencia seleccionada",
                Toast.LENGTH_SHORT
        ).show();
    }

    private File crearArchivoImagen(
            int indice
    ) throws IOException {

        File directorio = new File(
                getFilesDir(),
                "evidencias"
        );

        if (!directorio.exists() &&
                !directorio.mkdirs()) {

            throw new IOException(
                    "No se pudo crear el directorio"
            );
        }

        String nombre =
                limpiarNombreArchivo(folio) +
                        "_" +
                        SUFIJOS[indice] +
                        "_" +
                        System.currentTimeMillis();

        return File.createTempFile(
                nombre,
                ".jpg",
                directorio
        );
    }

    private void guardarRuta(
            int indice,
            String nuevaRuta
    ) {
        String rutaAnterior =
                obtenerRuta(indice);

        if (!rutaAnterior.isEmpty() &&
                !rutaAnterior.equals(nuevaRuta)) {

            eliminarArchivoLocal(rutaAnterior);
        }

        getSharedPreferences(
                PREFS_EVIDENCIAS,
                MODE_PRIVATE
        ).edit()
                .putString(
                        obtenerClave(indice),
                        nuevaRuta
                )
                .apply();
    }

    private void cargarEvidencias() {
        for (int i = 0; i < imagenes.length; i++) {
            mostrarEvidencia(i);
        }

        actualizarContador();
    }

    private void mostrarEvidencia(int indice) {
        String ruta = obtenerRuta(indice);

        imagenes[indice].setImageDrawable(null);

        if (ruta.isEmpty()) {
            imagenes[indice].setScaleType(
                    ImageView.ScaleType.CENTER_INSIDE
            );

            imagenes[indice].setImageResource(
                    android.R.drawable.ic_menu_camera
            );

            estados[indice].setText(
                    "Evidencia pendiente"
            );

            botones[indice].setText(
                    modoEdicion
                            ? "Agregar fotografía"
                            : "Sin evidencia"
            );

            botones[indice].setEnabled(modoEdicion);
            botones[indice].setAlpha(
                    modoEdicion ? 1f : 0.55f
            );

            return;
        }

        try {
            imagenes[indice].setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            if (ruta.startsWith("content://") ||
                    ruta.startsWith("file://")) {

                imagenes[indice].setImageURI(
                        Uri.parse(ruta)
                );
            } else {
                File archivo = new File(ruta);

                if (!archivo.exists()) {
                    eliminarRutaGuardada(indice);
                    mostrarEvidencia(indice);
                    return;
                }

                imagenes[indice].setImageURI(
                        Uri.fromFile(archivo)
                );
            }

            estados[indice].setText(
                    "Evidencia registrada"
            );

            botones[indice].setText(
                    modoEdicion
                            ? "Reemplazar fotografía"
                            : "Solo lectura"
            );

            botones[indice].setEnabled(modoEdicion);
            botones[indice].setAlpha(
                    modoEdicion ? 1f : 0.55f
            );
        } catch (Exception exception) {
            estados[indice].setText(
                    "No se pudo cargar la imagen"
            );
        }
    }

    private void confirmarEliminar(int indice) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar evidencia")
                .setMessage(
                        "¿Deseas eliminar " +
                                NOMBRES[indice].toLowerCase() +
                                "?"
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(
                        "Eliminar",
                        (dialog, which) ->
                                eliminarEvidencia(indice)
                )
                .show();
    }

    private void eliminarEvidencia(int indice) {
        String ruta = obtenerRuta(indice);

        eliminarArchivoLocal(ruta);
        eliminarRutaGuardada(indice);

        cargarEvidencias();

        Toast.makeText(
                this,
                "Evidencia eliminada",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void eliminarRutaGuardada(
            int indice
    ) {
        getSharedPreferences(
                PREFS_EVIDENCIAS,
                MODE_PRIVATE
        ).edit()
                .remove(obtenerClave(indice))
                .apply();
    }

    private void eliminarArchivoLocal(
            String ruta
    ) {
        if (ruta == null ||
                ruta.trim().isEmpty() ||
                ruta.startsWith("content://") ||
                ruta.startsWith("file://")) {
            return;
        }

        File archivo = new File(ruta);

        if (archivo.exists()) {
            archivo.delete();
        }
    }

    private String obtenerRuta(int indice) {
        String ruta = getSharedPreferences(
                PREFS_EVIDENCIAS,
                MODE_PRIVATE
        ).getString(
                obtenerClave(indice),
                ""
        );

        return ruta == null
                ? ""
                : ruta.trim();
    }

    private String obtenerClave(int indice) {
        return folio + "_" + SUFIJOS[indice];
    }

    private void actualizarContador() {
        int cantidad = 0;

        for (int i = 0; i < SUFIJOS.length; i++) {
            if (!obtenerRuta(i).isEmpty()) {
                cantidad++;
            }
        }

        txtContadorEvidencias.setText(
                cantidad + " de 4 evidencias registradas"
        );
    }

    private String limpiarNombreArchivo(
            String texto
    ) {
        return texto.replaceAll(
                "[^a-zA-Z0-9_-]",
                "_"
        );
    }

    private void mostrarError() {
        new AlertDialog.Builder(this)
                .setTitle("Supervisión no disponible")
                .setMessage(
                        "No se pudo abrir la información " +
                                "de esta supervisión."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Cerrar",
                        (dialog, which) -> finish()
                )
                .show();
    }
}