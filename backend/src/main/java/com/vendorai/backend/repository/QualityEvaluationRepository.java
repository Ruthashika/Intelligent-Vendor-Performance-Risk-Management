package com.vendorai.backend.repository;

import com.vendorai.backend.entity.QualityEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityEvaluationRepository
        extends JpaRepository<QualityEvaluation, Long> {
}