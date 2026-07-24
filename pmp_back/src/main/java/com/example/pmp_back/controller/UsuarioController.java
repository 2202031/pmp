package com.example.pmp_back.controller;

import com.example.pmp_back.model.Usuario;
import com.example.pmp_back.repository.UsuarioRepository;
import com.example.pmp_back.util.Validador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.example.pmp_back.repository.AsignacionRepository asignacionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int ID_ROL_SUPERVISOR = 2;

    // 1. OBTENER PERFIL POR USERNAME
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfilPorUsername(@RequestParam String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            Map<String, Object> data = new HashMap<>();
            data.put("nombre", usuario.getNombre());
            data.put("email", usuario.getEmail() != null ? usuario.getEmail() : "");
            data.put("username", usuario.getUsername());
            data.put("rpe", usuario.getRpe());
            data.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
            data.put("id_rol", usuario.getIdRol());
            data.put("zona", usuario.getZona() != null ? usuario.getZona() : "");

            return ResponseEntity.ok(data);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Usuario no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // 2. ACTUALIZAR PERFIL
    @PutMapping("/perfil/actualizar")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Map<String, String> datosPerfil) {
        String usernameOriginal = datosPerfil.get("usernameOriginal");
        String nuevoNombre = datosPerfil.get("nombre");
        String nuevoEmail = datosPerfil.get("email");
        String nuevoTelefono = datosPerfil.get("telefono");

        // ---- VALIDACIÓN según columnas de la tabla usuarios ----
        String error = Validador.obligatorio(usernameOriginal, "usernameOriginal", 255);
        if (error == null) error = Validador.obligatorio(nuevoNombre, "nombre", 255);
        if (error == null) error = Validador.validarEmail(nuevoEmail);
        if (error == null) error = Validador.validarTelefono(nuevoTelefono);

        if (error != null) {
            Map<String, String> respuestaError = new HashMap<>();
            respuestaError.put("status", "error");
            respuestaError.put("message", error);
            return ResponseEntity.badRequest().body(respuestaError);
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(usernameOriginal);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setNombre(nuevoNombre);
            usuario.setEmail(nuevoEmail);
            usuario.setTelefono(nuevoTelefono);

            usuarioRepository.save(usuario);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Perfil actualizado correctamente");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "No se pudo encontrar al usuario para actualizar");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // 3. CREAR USUARIO (el Administrador crea Supervisores)
    @PostMapping("/crear")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> datos) {
        try {
            String username = Validador.texto(datos, "username");
            String nombre = Validador.texto(datos, "nombre");
            String rpe = Validador.texto(datos, "rpe");
            String password = Validador.texto(datos, "password");
            String telefono = Validador.texto(datos, "telefono");
            String zona = Validador.texto(datos, "zona");
            String email = Validador.texto(datos, "email");
            String idRolTexto = Validador.texto(datos, "idRol");

            // ---- VALIDACIÓN según columnas de la tabla usuarios ----
            String error = Validador.validarUsername(username);
            if (error == null) error = Validador.obligatorio(nombre, "nombre", 255);
            if (error == null) error = Validador.obligatorio(password, "password", 255);
            if (error == null) error = Validador.opcional(rpe, "rpe", 255);
            if (error == null) error = Validador.validarEmail(email);
            if (error == null) error = Validador.validarTelefono(telefono);
            if (error == null) error = Validador.opcional(zona, "zona", 255);
            if (error == null) error = Validador.validarEntero(idRolTexto, "idRol", false);

            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            Integer idRol = ID_ROL_SUPERVISOR;
            if (!idRolTexto.isEmpty()) {
                Integer convertido = Validador.aEntero(idRolTexto);
                if (convertido != null) idRol = convertido;
            }

            if (usuarioRepository.existsByUsername(username)) {
                Map<String, String> conflicto = new HashMap<>();
                conflicto.put("status", "error");
                conflicto.put("message", "Ya existe un usuario con el nombre de usuario '" + username + "'. Elige otro.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(conflicto);
            }

            Usuario nuevo = new Usuario();
            nuevo.setUsername(username);
            nuevo.setNombre(nombre);
            nuevo.setRpe(rpe);
            nuevo.setTelefono(telefono.isEmpty() ? null : telefono);
            nuevo.setZona(zona.isEmpty() ? null : zona);
            nuevo.setEmail(email.isEmpty() ? null : email);
            nuevo.setIdRol(idRol);
            nuevo.setContrasenia(passwordEncoder.encode(password));

            usuarioRepository.save(nuevo);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Usuario creado correctamente");
            response.put("id_usuario", nuevo.getIdUsuarios());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al crear usuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 3b. EDITAR UN SUPERVISOR YA EXISTENTE (el Administrador lo edita desde Personal Operativo)
    @PutMapping("/editar-supervisor")
    public ResponseEntity<?> editarSupervisor(@RequestBody Map<String, Object> datos) {
        try {
            String username = Validador.texto(datos, "username");
            String nombre = Validador.texto(datos, "nombre");
            String email = Validador.texto(datos, "email");
            String telefono = Validador.texto(datos, "telefono");
            String zona = Validador.texto(datos, "zona");
            String password = Validador.texto(datos, "password");

            // ---- VALIDACIÓN ----
            String error = Validador.validarUsername(username);
            if (error == null) error = Validador.opcional(nombre, "nombre", 255);
            if (error == null) error = Validador.validarEmail(email);
            if (error == null) error = Validador.validarTelefono(telefono);
            if (error == null) error = Validador.opcional(zona, "zona", 255);
            if (error == null) error = Validador.opcional(password, "password", 255);

            if (error != null) {
                Map<String, String> respuestaError = new HashMap<>();
                respuestaError.put("status", "error");
                respuestaError.put("message", error);
                return ResponseEntity.badRequest().body(respuestaError);
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
            if (usuarioOpt.isEmpty()) {
                Map<String, String> noEncontrado = new HashMap<>();
                noEncontrado.put("status", "error");
                noEncontrado.put("message", "No se encontró el usuario '" + username + "'.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(noEncontrado);
            }

            Usuario usuario = usuarioOpt.get();
            if (!nombre.isEmpty()) usuario.setNombre(nombre);
            if (!email.isEmpty()) usuario.setEmail(email);
            if (!telefono.isEmpty()) usuario.setTelefono(telefono);
            if (!zona.isEmpty()) usuario.setZona(zona);

            // La contraseña solo se actualiza si mandan una nueva no vacía
            if (!password.isEmpty()) {
                usuario.setContrasenia(passwordEncoder.encode(password));
            }

            usuarioRepository.save(usuario);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Supervisor actualizado correctamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al editar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 4. LISTAR SUPERVISORES (para el spinner de "responsable" y Personal Operativo)
    @GetMapping("/supervisores")
    public ResponseEntity<?> listarSupervisores() {
        List<Usuario> supervisores = usuarioRepository.findByIdRol(ID_ROL_SUPERVISOR);

        List<Map<String, Object>> lista = new ArrayList<>();
        for (Usuario u : supervisores) {
            Map<String, Object> item = new HashMap<>();
            item.put("id_usuario", u.getIdUsuarios());
            item.put("nombre", u.getNombre());
            item.put("username", u.getUsername());
            item.put("email", u.getEmail() != null ? u.getEmail() : "");
            item.put("telefono", u.getTelefono() != null ? u.getTelefono() : "");
            item.put("zona", u.getZona() != null ? u.getZona() : "");
            item.put("rpe", u.getRpe());
            lista.add(item);
        }
        return ResponseEntity.ok(lista);
    }

    // 5. LISTAR TODOS LOS USUARIOS
    @GetMapping
    public ResponseEntity<?> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Usuario u : usuarios) {
            Map<String, Object> item = new HashMap<>();
            item.put("id_usuario", u.getIdUsuarios());
            item.put("nombre", u.getNombre());
            item.put("username", u.getUsername());
            item.put("email", u.getEmail() != null ? u.getEmail() : "");
            item.put("telefono", u.getTelefono() != null ? u.getTelefono() : "");
            item.put("zona", u.getZona() != null ? u.getZona() : "");
            item.put("rpe", u.getRpe());
            item.put("id_rol", u.getIdRol());
            lista.add(item);
        }
        return ResponseEntity.ok(lista);
    }

    // =========================================================================
    // ELIMINAR CUENTA
    // =========================================================================
    // Solo se permite eliminar cuando la cuenta NO tiene supervisiones
    // relacionadas. La llave foránea de asigar_super está definida como
    // ON DELETE SET NULL, así que borrar sin esta comprobación dejaría
    // supervisiones huérfanas (sin supervisor asignado).
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
            if (usuarioOpt.isEmpty()) {
                Map<String, String> noEncontrado = new HashMap<>();
                noEncontrado.put("status", "error");
                noEncontrado.put("message", "No existe un usuario con el id " + id + ".");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(noEncontrado);
            }

            Usuario usuario = usuarioOpt.get();

            int comoSupervisor = asignacionRepository.findByIdSupervisor(id).size();
            int comoAdministrador = asignacionRepository.findByIdAdministrador(id).size();
            int relacionadas = comoSupervisor + comoAdministrador;

            if (relacionadas > 0) {
                Map<String, Object> conflicto = new HashMap<>();
                conflicto.put("status", "error");
                conflicto.put("message", "No se puede eliminar a '" + usuario.getNombre()
                        + "' porque tiene " + relacionadas
                        + " supervisión(es) relacionada(s). Elimina o reasigna esas supervisiones primero.");
                conflicto.put("supervisionesRelacionadas", relacionadas);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(conflicto);
            }

            usuarioRepository.delete(usuario);

            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("status", "success");
            respuesta.put("message", "Cuenta eliminada correctamente.");
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error al eliminar la cuenta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
