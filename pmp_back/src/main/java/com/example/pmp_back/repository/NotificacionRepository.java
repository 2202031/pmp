package com.example.pmp_back.repository;

import com.example.pmp_back.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    // Busca las notificaciones correspondientes a un usuario y rol específicos
    List<Notificacion> findByEmailUsuarioAndRolUsuarioOrderByIdDesc(String emailUsuario, String rolUsuario);
}
