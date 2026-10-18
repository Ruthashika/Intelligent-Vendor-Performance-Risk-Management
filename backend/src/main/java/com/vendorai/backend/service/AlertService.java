
package com.vendorai.backend.service;

import com.vendorai.backend.entity.Alert;
import com.vendorai.backend.entity.VendorCompliance;
import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.repository.AlertRepository;
import com.vendorai.backend.repository.VendorComplianceRepository;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.VendorPerformanceScoreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final VendorComplianceRepository complianceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorPerformanceScoreRepository performanceScoreRepository;

    public AlertService(
            AlertRepository alertRepository,
            VendorComplianceRepository complianceRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            VendorPerformanceScoreRepository performanceScoreRepository) {

        this.alertRepository = alertRepository;
        this.complianceRepository = complianceRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.performanceScoreRepository = performanceScoreRepository;
    }

    // Generate alerts for delayed deliveries.
    public List<Alert> generateDeliveryAlerts() {

        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();

        for (PurchaseOrder order : orders) {

            if (order.getExpectedDeliveryDate() == null) {
                continue;
            }

            boolean isLate = false;

            if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
                isLate = LocalDate.now().isAfter(
                        order.getExpectedDeliveryDate());
            } else if (order.getActualDeliveryDate() != null) {
                isLate = order.getActualDeliveryDate().isAfter(
                        order.getExpectedDeliveryDate());
            }

            if (!isLate) {
                continue;
            }

            boolean alreadyExists = alertRepository.findByStatus("OPEN")
                    .stream()
                    .anyMatch(alert ->
                            alert.getPurchaseOrder() != null
                            && Objects.equals(
                                    alert.getPurchaseOrder().getId(),
                                    order.getId())
                            && "DELIVERY_DELAY".equals(
                                    alert.getAlertType()));

            if (!alreadyExists) {
                Alert alert = new Alert();
                alert.setAlertType("DELIVERY_DELAY");
                alert.setSeverity("HIGH");
                alert.setVendor(order.getVendor());
                alert.setPurchaseOrder(order);

                if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
                    alert.setMessage(
                            "Purchase order " + order.getPoNumber()
                            + " was delivered after the expected delivery date.");
                } else {
                    alert.setMessage(
                            "Purchase order " + order.getPoNumber()
                            + " has passed its expected delivery date.");
                }

                alertRepository.save(alert);
            }
        }

        return alertRepository.findByStatus("OPEN");
    }

    // Create an alert when vendor performance is low.
    public Alert createPerformanceAlert(
            VendorPerformanceScore performanceScore) {

        if (performanceScore == null
                || performanceScore.getPerformanceScore() >= 60) {
            return null;
        }

        Alert alert = new Alert();
        alert.setAlertType("LOW_VENDOR_PERFORMANCE");
        alert.setSeverity("MEDIUM");
        alert.setVendor(performanceScore.getVendor());

        alert.setMessage(
                "Vendor performance score is "
                + String.format(
                        "%.2f",
                        performanceScore.getPerformanceScore())
                + "/100.");

        return alertRepository.save(alert);
    }

    // Create an alert when ML predicts HIGH delivery risk.
    public Alert createPredictiveRiskAlert(PurchaseOrder order) {

        if (order == null
                || !"HIGH".equalsIgnoreCase(
                        order.getPredictedDeliveryRisk())
                || order.getId() == null) {
            return null;
        }

        boolean alreadyExists = alertRepository.findByStatus("OPEN")
                .stream()
                .anyMatch(alert ->
                        alert.getPurchaseOrder() != null
                        && Objects.equals(
                                alert.getPurchaseOrder().getId(),
                                order.getId())
                        && "PREDICTED_DELIVERY_RISK".equals(
                                alert.getAlertType()));

        if (alreadyExists) {
            return null;
        }

        Alert alert = new Alert();
        alert.setAlertType("PREDICTED_DELIVERY_RISK");
        alert.setSeverity("HIGH");
        alert.setVendor(order.getVendor());
        alert.setPurchaseOrder(order);

        double probability = order.getRiskProbability() == null
                ? 0.0
                : order.getRiskProbability() * 100.0;

        alert.setMessage(
                "High delivery risk predicted for purchase order "
                + order.getPoNumber()
                + ". Estimated probability: "
                + String.format("%.2f%%", probability)
                + ". Review the order before its expected delivery date.");

        return alertRepository.save(alert);
    }

    // Generate alerts for expired or soon-to-expire compliance documents.
    public List<Alert> generateComplianceAlerts() {
        LocalDate today = LocalDate.now();
        for (VendorCompliance document : complianceRepository.findAll()) {
            if (document.getVendor() == null
                    || document.getVendor().getId() == null
                    || document.getExpiryDate() == null) {
                continue;
            }
            LocalDate expiry = document.getExpiryDate();
            String alertType;
            String severity;
            if (expiry.isBefore(today)) {
                alertType = "COMPLIANCE_EXPIRED";
                severity = "HIGH";
            } else if (!expiry.isAfter(today.plusDays(30))) {
                alertType = "COMPLIANCE_EXPIRING";
                severity = "MEDIUM";
            } else {
                continue;
            }
            String message = "Compliance document '"
                    + document.getDocumentName()
                    + "' (record ID " + document.getId()
                    + ") expires on " + expiry + ".";
            boolean alreadyExists = alertRepository.findByStatus("OPEN")
                    .stream()
                    .anyMatch(alert ->
                            alert.getVendor() != null
                            && java.util.Objects.equals(
                                    alert.getVendor().getId(),
                                    document.getVendor().getId())
                            && alertType.equals(alert.getAlertType())
                            && message.equals(alert.getMessage()));
            if (!alreadyExists) {
                Alert alert = new Alert();
                alert.setAlertType(alertType);
                alert.setSeverity(severity);
                alert.setVendor(document.getVendor());
                alert.setMessage(message);
                alertRepository.save(alert);
            }
        }
        return alertRepository.findByStatus("OPEN")
                .stream()
                .filter(alert -> alert.getAlertType() != null
                        && alert.getAlertType().startsWith("COMPLIANCE_"))
                .toList();
    }    // Get all open alerts.
    public List<Alert> getOpenAlerts() {
        return alertRepository.findByStatus("OPEN");
    }

    // Get all alerts belonging to a vendor.
    public List<Alert> getVendorAlerts(Long vendorId) {
        return alertRepository.findByVendorId(vendorId);
    }

    // Resolve an existing alert.
    public Alert resolveAlert(Long alertId) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() ->
                        new RuntimeException("Alert not found"));

        alert.setStatus("RESOLVED");

        return alertRepository.save(alert);
    }
}