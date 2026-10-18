
package com.vendorai.backend.service;

import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.QualityEvaluation;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.QualityEvaluationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QualityEvaluationService {

    private final QualityEvaluationRepository qualityEvaluationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorPerformanceService vendorPerformanceService;

    public QualityEvaluationService(
            QualityEvaluationRepository qualityEvaluationRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            VendorPerformanceService vendorPerformanceService) {

        this.qualityEvaluationRepository = qualityEvaluationRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.vendorPerformanceService = vendorPerformanceService;
    }

    public QualityEvaluation createEvaluation(
            QualityEvaluation evaluation,
            Long purchaseOrderId) {

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() ->
                        new RuntimeException("Purchase order not found"));

        evaluation.setPurchaseOrder(purchaseOrder);

        QualityEvaluation savedEvaluation =
                qualityEvaluationRepository.save(evaluation);

        // Recalculate performance only for completed deliveries.
        if ("DELIVERED".equalsIgnoreCase(purchaseOrder.getStatus())) {
            vendorPerformanceService.calculatePerformance(
                    purchaseOrder.getVendor().getId());
        }

        return savedEvaluation;
    }

    public List<QualityEvaluation> getAllEvaluations() {
        return qualityEvaluationRepository.findAll();
    }

    public Optional<QualityEvaluation> getEvaluationById(Long id) {
        return qualityEvaluationRepository.findById(id);
    }

    public void deleteEvaluation(Long id) {
        QualityEvaluation evaluation =
                qualityEvaluationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Quality evaluation not found"));

        Long vendorId = evaluation.getPurchaseOrder()
                .getVendor().getId();

        boolean wasDelivered = "DELIVERED".equalsIgnoreCase(
                evaluation.getPurchaseOrder().getStatus());

        qualityEvaluationRepository.delete(evaluation);

        // Keep the score consistent after removing an evaluation.
        if (wasDelivered) {
            vendorPerformanceService.calculatePerformance(vendorId);
        }
    }
}
