package com.example.pmp_back.repository;

import com.example.pmp_back.model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
    Optional<Asignacion> findByFolio(String folio);
    List<Asignacion> findByIdSupervisor(Integer idSupervisor);
    List<Asignacion> findByIdAdministrador(Integer idAdministrador);
}
