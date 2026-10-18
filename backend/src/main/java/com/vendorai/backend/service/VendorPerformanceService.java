package com.vendorai.backend.service;
import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.QualityEvaluation;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.QualityEvaluationRepository;
import com.vendorai.backend.repository.VendorPerformanceScoreRepository;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
@Service
public class VendorPerformanceService {
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final QualityEvaluationRepository qualityEvaluationRepository;
    private final VendorPerformanceScoreRepository performanceScoreRepository;
    public VendorPerformanceService(
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            QualityEvaluationRepository qualityEvaluationRepository,
            VendorPerformanceScoreRepository performanceScoreRepository) {
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.qualityEvaluationRepository = qualityEvaluationRepository;
        this.performanceScoreRepository = performanceScoreRepository;
    }
    public VendorPerformanceScore calculatePerformance(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll()
                .stream()
                .filter(order -> order.getVendor() != null)
                .filter(order -> order.getVendor().getId() != null
                        && order.getVendor().getId().equals(vendorId))
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus()))
                .toList();
        if (orders.isEmpty()) {
            throw new RuntimeException(
                    "No completed orders found for this vendor");
        }
        int datedOrderCount = 0;
        int onTimeOrders = 0;
        double totalPositiveDelay = 0;
        double totalOrderedQuantity = 0;
        double totalDeliveredQuantity = 0;
        int validQuantityOrderCount = 0;
        for (PurchaseOrder order : orders) {
            if (order.getExpectedDeliveryDate() != null
                    && order.getActualDeliveryDate() != null) {
                datedOrderCount++;
                long delay = ChronoUnit.DAYS.between(
                        order.getExpectedDeliveryDate(),
                        order.getActualDeliveryDate());
                if (delay <= 0) {
                    onTimeOrders++;
                } else {
                    totalPositiveDelay += delay;
                }
            }
            if (order.getQuantity() != null
                    && order.getActualDeliveredQuantity() != null
                    && order.getQuantity() > 0
                    && order.getActualDeliveredQuantity() >= 0) {
                totalOrderedQuantity += order.getQuantity();
                totalDeliveredQuantity += order.getActualDeliveredQuantity();
                validQuantityOrderCount++;
            }
        }
        Double onTimeDeliveryRate = null;
        Double averageDeliveryDelay = null;
        Double delayScore = null;
        if (datedOrderCount > 0) {
            onTimeDeliveryRate =
                    (double) onTimeOrders / datedOrderCount * 100.0;
            averageDeliveryDelay =
                    totalPositiveDelay / datedOrderCount;
            delayScore = Math.max(
                    0.0, 100.0 - averageDeliveryDelay * 20.0);
        }
        Double fulfillmentRate = null;
        if (validQuantityOrderCount > 0 && totalOrderedQuantity > 0) {
            fulfillmentRate = Math.min(
                    100.0,
                    totalDeliveredQuantity / totalOrderedQuantity * 100.0);
        }
        List<QualityEvaluation> evaluations =
                qualityEvaluationRepository.findAll()
                        .stream()
                        .filter(evaluation -> evaluation != null)
                        .filter(evaluation -> evaluation.getPurchaseOrder() != null)
                        .filter(evaluation ->
                                evaluation.getPurchaseOrder().getVendor() != null)
                        .filter(evaluation -> {
                            Long id = evaluation.getPurchaseOrder()
                                    .getVendor().getId();
                            return id != null && id.equals(vendorId);
                        })
                        .filter(evaluation -> evaluation.getIssueCount() != null)
                        .toList();
        Double qualityIssueRate = null;
        Double qualityScore = null;
        if (!evaluations.isEmpty()) {
            long evaluationsWithIssues = evaluations.stream()
                    .filter(evaluation -> evaluation.getIssueCount() > 0)
                    .count();
            qualityIssueRate =
                    (double) evaluationsWithIssues / evaluations.size() * 100.0;
            qualityScore = 100.0 - qualityIssueRate;
        }
        double weightedTotal = 0.0;
        double availableWeight = 0.0;
        if (onTimeDeliveryRate != null) {
            weightedTotal += onTimeDeliveryRate * 0.40;
            availableWeight += 0.40;
        }
        if (qualityScore != null) {
            weightedTotal += qualityScore * 0.20;
            availableWeight += 0.20;
        }
        if (fulfillmentRate != null) {
            weightedTotal += fulfillmentRate * 0.20;
            availableWeight += 0.20;
        }
        if (delayScore != null) {
            weightedTotal += delayScore * 0.20;
            availableWeight += 0.20;
        }
        if (availableWeight == 0.0) {
            throw new RuntimeException(
                    "Insufficient completed-order data to calculate performance score");
        }
        double performanceScore = weightedTotal / availableWeight;
        VendorPerformanceScore performance = new VendorPerformanceScore();
        performance.setVendor(vendor);
        performance.setOnTimeDeliveryRate(onTimeDeliveryRate);
        performance.setAverageDeliveryDelay(averageDeliveryDelay);
        performance.setQualityIssueRate(qualityIssueRate);
        performance.setFulfillmentRate(fulfillmentRate);
        performance.setPerformanceScore(performanceScore);
        performance.setCalculatedAt(LocalDateTime.now());
        return performanceScoreRepository.save(performance);
    }
    public List<VendorPerformanceScore> getVendorPerformanceHistory(
            Long vendorId) {
        if (!vendorRepository.existsById(vendorId)) {
            throw new RuntimeException("Vendor not found");
        }
        return performanceScoreRepository.findByVendorId(vendorId);
    }
}
