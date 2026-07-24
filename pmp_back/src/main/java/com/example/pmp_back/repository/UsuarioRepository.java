package com.example.pmp_back.repository;

import com.example.pmp_back.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Este método extra nos servirá más adelante para el Login en tu app móvil
   Optional<Usuario> findByUsername(String username);

   java.util.List<Usuario> findByIdRol(Integer idRol);

   boolean existsByUsername(String username);

}