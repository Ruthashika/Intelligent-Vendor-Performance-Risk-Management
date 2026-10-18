package com.vendorai.backend.controller;

import com.vendorai.backend.dto.VendorPortalResponse;
import com.vendorai.backend.service.VendorPortalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor-portal")
@CrossOrigin(origins = "*")
public class VendorPortalController {

    private final VendorPortalService vendorPortalService;

    public VendorPortalController(
            VendorPortalService vendorPortalService) {

        this.vendorPortalService = vendorPortalService;
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<?> getVendorPortal(
            @PathVariable Long vendorId) {

        try {
            return ResponseEntity.ok(
                    vendorPortalService.getVendorPortal(vendorId)
            );

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}