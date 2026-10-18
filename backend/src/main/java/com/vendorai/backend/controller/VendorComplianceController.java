package com.vendorai.backend.controller;
import com.vendorai.backend.entity.VendorCompliance;
import com.vendorai.backend.service.VendorComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/compliance")
@CrossOrigin(origins = "*")
public class VendorComplianceController {
    private final VendorComplianceService complianceService;
    public VendorComplianceController(
            VendorComplianceService complianceService) {
        this.complianceService = complianceService;
    }
    @PostMapping("/vendors/{vendorId}")
    public ResponseEntity<?> create(
            @PathVariable Long vendorId,
            @RequestBody VendorCompliance input) {
        try {
            return ResponseEntity.ok(
                    complianceService.create(vendorId, input));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping
    public List<VendorCompliance> getAll() {
        return complianceService.getAll();
    }
    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<?> getByVendor(@PathVariable Long vendorId) {
        try {
            return ResponseEntity.ok(
                    complianceService.getByVendor(vendorId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody VendorCompliance input) {
        try {
            return ResponseEntity.ok(
                    complianceService.update(id, input));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            complianceService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}