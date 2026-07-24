package com.example.pmp_back.repository;

import com.example.pmp_back.model.ReportePdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportePdfRepository extends JpaRepository<ReportePdf, Integer> {
    List<ReportePdf> findByFolio(String folio);
    List<ReportePdf> findAllByOrderByFechaGeneracionDesc();
}
