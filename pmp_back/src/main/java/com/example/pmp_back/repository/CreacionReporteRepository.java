package com.example.pmp_back.repository;

import com.example.pmp_back.model.CreacionReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreacionReporteRepository extends JpaRepository<CreacionReporte, Integer> {
    Optional<CreacionReporte> findByFolio(String folio);
    List<CreacionReporte> findByIdUsuarios(Integer idUsuarios);
}
