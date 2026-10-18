
package com.vendorai.backend.service;

import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.QualityEvaluation;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.ml.MlPredictionRequest;
import com.vendorai.backend.ml.MlPredictionResponse;
import com.vendorai.backend.ml.MlPredictionService;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.QualityEvaluationRepository;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorRepository vendorRepository;
    private final QualityEvaluationRepository qualityEvaluationRepository;
    private final MlPredictionService mlPredictionService;
    private final AlertService alertService;
    private final VendorPerformanceService vendorPerformanceService;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            VendorRepository vendorRepository,
            QualityEvaluationRepository qualityEvaluationRepository,
            MlPredictionService mlPredictionService,
            AlertService alertService,
            VendorPerformanceService vendorPerformanceService) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.vendorRepository = vendorRepository;
        this.qualityEvaluationRepository = qualityEvaluationRepository;
        this.mlPredictionService = mlPredictionService;
        this.alertService = alertService;
        this.vendorPerformanceService = vendorPerformanceService;
    }

    public PurchaseOrder createPurchaseOrder(
            PurchaseOrder purchaseOrder,
            Long vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));

        if (purchaseOrder.getQuantity() == null
                || purchaseOrder.getUnitPrice() == null) {
            throw new RuntimeException(
                    "Quantity and unit price are required");
        }

        if (purchaseOrder.getQuantity() <= 0
                || purchaseOrder.getUnitPrice() < 0) {
            throw new RuntimeException(
                    "Quantity must be positive and unit price cannot be negative");
        }

        if (purchaseOrder.getExpectedDeliveryDate() == null) {
            throw new RuntimeException(
                    "Expected delivery date is required for risk prediction");
        }

        if (purchaseOrder.getOrderDate() == null) {
            purchaseOrder.setOrderDate(LocalDate.now());
        }

        if (purchaseOrder.getExpectedDeliveryDate()
                .isBefore(purchaseOrder.getOrderDate())) {
            throw new RuntimeException(
                    "Expected delivery date cannot be before the order date");
        }

        purchaseOrder.setVendor(vendor);

        purchaseOrder.setTotalAmount(
                purchaseOrder.getQuantity()
                        * purchaseOrder.getUnitPrice());

        // Retrieve this vendor's completed delivery history.
        List<PurchaseOrder> history = purchaseOrderRepository.findAll()
                .stream()
                .filter(po -> po.getVendor() != null
                        && Objects.equals(
                                po.getVendor().getId(), vendorId)
                        && po.getActualDeliveryDate() != null)
                .sorted(Comparator.comparing(
                        PurchaseOrder::getActualDeliveryDate).reversed())
                .collect(Collectors.toList());

        // Index historical quality evaluations by purchase order ID.
        Map<Long, QualityEvaluation> qualityByOrder = new HashMap<>();

        for (QualityEvaluation evaluation
                : qualityEvaluationRepository.findAll()) {

            if (evaluation.getPurchaseOrder() != null
                    && evaluation.getPurchaseOrder().getId() != null) {

                qualityByOrder.put(
                        evaluation.getPurchaseOrder().getId(),
                        evaluation);
            }
        }

        // Prepare input for the Python ML prediction API.
        MlPredictionRequest request = new MlPredictionRequest();

        request.setCategory(
                vendor.getCategory() == null
                        ? "General" : vendor.getCategory());

        request.setPriority(
                purchaseOrder.getPriority() == null
                        ? "Medium" : purchaseOrder.getPriority());

        request.setQuantity(purchaseOrder.getQuantity());
        request.setUnit_price(purchaseOrder.getUnitPrice());
        request.setTotal_amount(purchaseOrder.getTotalAmount());

        double plannedDays = Math.max(
                1,
                ChronoUnit.DAYS.between(
                        purchaseOrder.getOrderDate(),
                        purchaseOrder.getExpectedDeliveryDate()));

        request.setPlanned_delivery_days(plannedDays);
        request.setPrevious_po_count(history.size());

        double previousOnTime = rateOnTime(history);
        double previousFulfillment = rateFulfillment(history);
        double previousDelay = averagePositiveDelay(history);
        double previousQualityIssues =
                qualityIssueRate(history, qualityByOrder);

        request.setPrevious_on_time_rate(previousOnTime);
        request.setPrevious_average_delay(previousDelay);

        request.setPrevious_fulfillment_rate(
                history.isEmpty() ? 100.0 : previousFulfillment);

        request.setPrevious_quality_issue_rate(previousQualityIssues);

        // Use the five most recently completed orders.
        List<PurchaseOrder> recent = history.stream()
                .limit(5)
                .collect(Collectors.toList());

        request.setRecent_on_time_rate(rateOnTime(recent));
        request.setRecent_average_delay(averagePositiveDelay(recent));

        request.setRecent_fulfillment_rate(
                recent.isEmpty() ? 100.0 : rateFulfillment(recent));

        // Require a valid ML prediction before saving the order.
        MlPredictionResponse prediction =
                mlPredictionService.predict(request);

        if (prediction == null
                || prediction.getDelivery_risk() == null
                || prediction.getDelivery_risk().isBlank()) {
            throw new RuntimeException(
                    "ML service returned an invalid prediction");
        }

        String risk = prediction.getDelivery_risk().trim();

        if (!risk.equalsIgnoreCase("LOW")
                && !risk.equalsIgnoreCase("MEDIUM")
                && !risk.equalsIgnoreCase("HIGH")) {
            throw new RuntimeException(
                    "ML service returned an unknown risk class: " + risk);
        }

        purchaseOrder.setPredictedDeliveryRisk(risk);
        purchaseOrder.setRiskProbability(
                prediction.getRisk_probability());

        purchaseOrder.setRiskExplanation(
                buildRiskExplanation(
                        risk,
                        plannedDays,
                        previousOnTime,
                        previousDelay,
                        previousFulfillment,
                        previousQualityIssues));

        // Save first so the purchase order receives a database ID.
        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(purchaseOrder);

        // Automatically create an alert for HIGH-risk orders.
        alertService.createPredictiveRiskAlert(savedOrder);

        return savedOrder;
    }

    private double rateOnTime(List<PurchaseOrder> orders) {
        if (orders.isEmpty()) {
            return 0.0;
        }

        long onTime = orders.stream()
                .filter(po -> po.getExpectedDeliveryDate() != null
                        && po.getActualDeliveryDate() != null
                        && !po.getActualDeliveryDate()
                                .isAfter(po.getExpectedDeliveryDate()))
                .count();

        return 100.0 * onTime / orders.size();
    }

    private double rateFulfillment(List<PurchaseOrder> orders) {
        if (orders.isEmpty()) {
            return 100.0;
        }

        double total = 0.0;
        int count = 0;

        for (PurchaseOrder po : orders) {
            if (po.getQuantity() != null
                    && po.getQuantity() > 0
                    && po.getActualDeliveredQuantity() != null) {

                total += 100.0
                        * po.getActualDeliveredQuantity()
                        / po.getQuantity();

                count++;
            }
        }

        return count == 0 ? 100.0 : total / count;
    }

    private double averagePositiveDelay(List<PurchaseOrder> orders) {
        if (orders.isEmpty()) {
            return 0.0;
        }

        double totalDelay = 0.0;
        int count = 0;

        for (PurchaseOrder po : orders) {
            if (po.getExpectedDeliveryDate() == null
                    || po.getActualDeliveryDate() == null) {
                continue;
            }

            long delay = ChronoUnit.DAYS.between(
                    po.getExpectedDeliveryDate(),
                    po.getActualDeliveryDate());

            if (delay > 0) {
                totalDelay += delay;
            }

            count++;
        }

        return count == 0 ? 0.0 : totalDelay / count;
    }

    private double qualityIssueRate(
            List<PurchaseOrder> orders,
            Map<Long, QualityEvaluation> qualityByOrder) {

        if (orders.isEmpty()) {
            return 0.0;
        }

        long ordersWithIssues = orders.stream()
                .filter(po -> {
                    QualityEvaluation evaluation =
                            qualityByOrder.get(po.getId());

                    return evaluation != null
                            && evaluation.getIssueCount() != null
                            && evaluation.getIssueCount() > 0;
                })
                .count();

        return 100.0 * ordersWithIssues / orders.size();
    }

    private String buildRiskExplanation(
            String risk,
            double plannedDays,
            double onTimeRate,
            double averageDelay,
            double fulfillmentRate,
            double qualityIssueRate) {

        List<String> reasons = new ArrayList<>();

        if (onTimeRate < 80.0) {
            reasons.add(String.format(
                    "historical on-time delivery is low (%.1f%%)",
                    onTimeRate));
        }

        if (averageDelay > 2.0) {
            reasons.add(String.format(
                    "average historical delay is %.1f days",
                    averageDelay));
        }

        if (fulfillmentRate < 95.0) {
            reasons.add(String.format(
                    "historical fulfillment is %.1f%%",
                    fulfillmentRate));
        }

        if (qualityIssueRate > 30.0) {
            reasons.add(String.format(
                    "quality issues occurred in %.1f%% of past orders",
                    qualityIssueRate));
        }

        if (plannedDays <= 3.0) {
            reasons.add("the planned delivery window is short");
        }

        String historicalExplanation;

        if (reasons.isEmpty()) {
            historicalExplanation =
                    "No major warning was identified by the configured "
                    + "historical threshold checks";
        } else {
            historicalExplanation = String.join("; ", reasons);
        }

        return "ML-predicted risk: " + risk
                + ". Historical checks: " + historicalExplanation + ". "
                + "These indicators do not represent a feature-level "
                + "explanation of the ML model's prediction.";
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public PurchaseOrder updateDelivery(
            Long id,
            LocalDate actualDeliveryDate,
            Integer actualDeliveredQuantity) {

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Purchase order not found"));

        if (actualDeliveryDate == null
                || actualDeliveredQuantity == null
                || actualDeliveredQuantity < 0) {
            throw new RuntimeException(
                    "A valid delivery date and non-negative delivered quantity are required");
        }

        if (purchaseOrder.getQuantity() == null
                || actualDeliveredQuantity > purchaseOrder.getQuantity()) {
            throw new RuntimeException(
                    "Delivered quantity cannot exceed the ordered quantity");
        }

        purchaseOrder.setActualDeliveryDate(actualDeliveryDate);
        purchaseOrder.setActualDeliveredQuantity(actualDeliveredQuantity);
        purchaseOrder.setStatus("DELIVERED");

        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(purchaseOrder);

        // Recalculate performance after recording the delivery.
        vendorPerformanceService.calculatePerformance(
                savedOrder.getVendor().getId());

        return savedOrder;
    }

    public void deletePurchaseOrder(Long id) {
        if (!purchaseOrderRepository.existsById(id)) {
            throw new RuntimeException("Purchase order not found");
        }

        purchaseOrderRepository.deleteById(id);
    }
}
