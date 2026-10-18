package com.vendorai.backend.controller;

import com.vendorai.backend.entity.Alert;
import com.vendorai.backend.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/generate-delivery")
    public ResponseEntity<List<Alert>> generateDeliveryAlerts() {

        return ResponseEntity.ok(
                alertService.generateDeliveryAlerts()
        );
    }

    @PostMapping("/generate-compliance")
    public ResponseEntity<List<Alert>> generateComplianceAlerts() {
        return ResponseEntity.ok(
                alertService.generateComplianceAlerts()
        );
    }    @GetMapping("/open")
    public ResponseEntity<List<Alert>> getOpenAlerts() {

        return ResponseEntity.ok(
                alertService.getOpenAlerts()
        );
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<Alert>> getVendorAlerts(
            @PathVariable Long vendorId) {

        return ResponseEntity.ok(
                alertService.getVendorAlerts(vendorId)
        );
    }

    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<?> resolveAlert(
            @PathVariable Long alertId) {

        try {
            return ResponseEntity.ok(
                    alertService.resolveAlert(alertId)
            );

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}