package com.example.pmp_back.repository;

import com.example.pmp_back.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Integer> {
    Optional<Actividad> findByIdActividad(String idActividad);
}
