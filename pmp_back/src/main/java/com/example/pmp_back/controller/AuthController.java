package com.example.pmp_back.controller;

import com.example.pmp_back.model.Usuario;
import com.example.pmp_back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // id_rol = 1 -> ADMINISTRADOR (asigna, crea usuarios, ve reportes)
    // id_rol = 2 -> SUPERVISOR    (llena el checklist/reporte asignado)
    private static final int ID_ROL_ADMINISTRADOR = 1;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData == null ? null : loginData.get("username");
        String password = loginData == null ? null : loginData.get("password");

        // Validación mínima de entrada. Nota: a propósito NO se revela si el
        // usuario existe o no; el mensaje de error es genérico por seguridad.
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Debes enviar usuario y contraseña.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        username = username.trim();

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty()) {
            // Diagnóstico solo en la consola del servidor (al cliente se le
            // sigue devolviendo un mensaje genérico por seguridad).
            System.out.println("[LOGIN] Usuario NO encontrado en la BD: '" + username + "'");
        }

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            if (usuario.getContrasenia() == null) {
                System.out.println("[LOGIN] El usuario '" + username + "' no tiene contraseña guardada en la BD.");
            } else if (!coincideContrasenia(password, usuario.getContrasenia())) {
                String guardada = usuario.getContrasenia();
                String tipo = (guardada.startsWith("$2a$") || guardada.startsWith("$2b$") || guardada.startsWith("$2y$"))
                        ? "BCrypt (hasheada)" : "texto plano";
                System.out.println("[LOGIN] Contraseña incorrecta para '" + username
                        + "'. Formato guardado: " + tipo
                        + ". Longitud guardada: " + guardada.length()
                        + ". Longitud recibida: " + password.length());
            }

            if (usuario.getContrasenia() != null && coincideContrasenia(password, usuario.getContrasenia())) {

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Login correcto");
                response.put("id_usuario", usuario.getIdUsuarios());
                response.put("nombre", usuario.getNombre());
                response.put("username", usuario.getUsername());
                response.put("rpe", usuario.getRpe());

                String rolTexto = "supervisor";
                if (usuario.getIdRol() != null && usuario.getIdRol() == ID_ROL_ADMINISTRADOR) {
                    rolTexto = "administrador";
                }

                response.put("rol", rolTexto);
                response.put("id_rol", usuario.getIdRol());

                return ResponseEntity.ok(response);
            }
        }

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "Usuario o contraseña incorrectos");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    private boolean coincideContrasenia(String enviada, String guardada) {
        if (enviada == null || guardada == null) return false;

        String almacenada = guardada.trim();

        if (almacenada.startsWith("$2a$") || almacenada.startsWith("$2b$") || almacenada.startsWith("$2y$")) {
            return passwordEncoder.matches(enviada, almacenada);
        }
        // Contraseñas antiguas en texto plano. Se compara sin espacios sobrantes
        // por si la fila quedó con espacios al final en la base de datos.
        return almacenada.equals(enviada.trim());
    }
}
