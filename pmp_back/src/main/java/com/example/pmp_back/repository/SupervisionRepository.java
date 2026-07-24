package com.example.pmp_back.repository;

import com.example.pmp_back.model.Supervision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupervisionRepository extends JpaRepository<Supervision, Integer> {
    Optional<Supervision> findByFolio(String folio);
}
