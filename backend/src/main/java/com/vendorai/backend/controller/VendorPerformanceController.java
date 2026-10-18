package com.vendorai.backend.controller;

import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.service.VendorPerformanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
@CrossOrigin(origins = "*")
public class VendorPerformanceController {

    private final VendorPerformanceService vendorPerformanceService;

    public VendorPerformanceController(
            VendorPerformanceService vendorPerformanceService) {

        this.vendorPerformanceService = vendorPerformanceService;
    }

    @PostMapping("/vendors/{vendorId}/calculate")
    public ResponseEntity<?> calculatePerformance(
            @PathVariable Long vendorId) {

        try {
            return ResponseEntity.ok(
                    vendorPerformanceService
                            .calculatePerformance(vendorId)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/vendors/{vendorId}/history")
    public ResponseEntity<List<VendorPerformanceScore>>
    getPerformanceHistory(@PathVariable Long vendorId) {

        return ResponseEntity.ok(
                vendorPerformanceService
                        .getVendorPerformanceHistory(vendorId)
        );
    }
}