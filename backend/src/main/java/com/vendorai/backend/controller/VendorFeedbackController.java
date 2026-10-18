package com.vendorai.backend.controller;

import com.vendorai.backend.entity.VendorFeedback;
import com.vendorai.backend.service.VendorFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class VendorFeedbackController {

    private final VendorFeedbackService feedbackService;

    public VendorFeedbackController(
            VendorFeedbackService feedbackService) {

        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<?> createFeedback(
            @RequestParam Long vendorId,
            @RequestParam Long purchaseOrderId,
            @RequestBody VendorFeedback feedback) {

        try {
            return ResponseEntity.ok(
                    feedbackService.createFeedback(
                            feedback,
                            vendorId,
                            purchaseOrderId
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VendorFeedback>>
    getAllFeedback() {

        return ResponseEntity.ok(
                feedbackService.getAllFeedback()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorFeedback>
    getFeedbackById(@PathVariable Long id) {

        return feedbackService
                .getFeedbackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<VendorFeedback>>
    getVendorFeedback(@PathVariable Long vendorId) {

        return ResponseEntity.ok(
                feedbackService.getVendorFeedback(vendorId)
        );
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    public ResponseEntity<List<VendorFeedback>>
    getPurchaseOrderFeedback(
            @PathVariable Long purchaseOrderId) {

        return ResponseEntity.ok(
                feedbackService
                        .getPurchaseOrderFeedback(purchaseOrderId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long id) {

        try {
            feedbackService.deleteFeedback(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}