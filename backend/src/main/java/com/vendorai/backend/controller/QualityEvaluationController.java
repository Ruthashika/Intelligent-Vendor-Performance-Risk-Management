package com.vendorai.backend.controller;

import com.vendorai.backend.entity.QualityEvaluation;
import com.vendorai.backend.service.QualityEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-evaluations")
@CrossOrigin(origins = "*")
public class QualityEvaluationController {

    private final QualityEvaluationService qualityEvaluationService;

    public QualityEvaluationController(
            QualityEvaluationService qualityEvaluationService) {

        this.qualityEvaluationService = qualityEvaluationService;
    }

    @PostMapping
    public ResponseEntity<?> createEvaluation(
            @RequestParam Long purchaseOrderId,
            @RequestBody QualityEvaluation evaluation) {

        try {
            return ResponseEntity.ok(
                    qualityEvaluationService.createEvaluation(
                            evaluation,
                            purchaseOrderId
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public List<QualityEvaluation> getAllEvaluations() {
        return qualityEvaluationService.getAllEvaluations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QualityEvaluation> getEvaluationById(
            @PathVariable Long id) {

        return qualityEvaluationService
                .getEvaluationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvaluation(
            @PathVariable Long id) {

        try {
            qualityEvaluationService.deleteEvaluation(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}