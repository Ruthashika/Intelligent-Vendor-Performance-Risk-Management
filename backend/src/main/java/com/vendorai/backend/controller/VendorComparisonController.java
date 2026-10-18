package com.vendorai.backend.controller;

import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.service.VendorComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-comparison")
@CrossOrigin(origins = "*")
public class VendorComparisonController {

    private final VendorComparisonService vendorComparisonService;

    public VendorComparisonController(
            VendorComparisonService vendorComparisonService) {

        this.vendorComparisonService =
                vendorComparisonService;
    }

    @GetMapping
    public ResponseEntity<List<VendorPerformanceScore>>
    getLatestVendorPerformance() {

        return ResponseEntity.ok(
                vendorComparisonService
                        .getLatestVendorPerformance()
        );
    }

    @GetMapping("/compare")
    public ResponseEntity<List<VendorPerformanceScore>>
    compareVendors(
            @RequestParam List<Long> vendorIds) {

        return ResponseEntity.ok(
                vendorComparisonService
                        .compareVendors(vendorIds)
        );
    }
}