package com.vendorai.backend.controller;

import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.service.PurchaseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(
            @RequestBody PurchaseOrder purchaseOrder,
            @RequestParam Long vendorId) {

        PurchaseOrder createdPurchaseOrder =
                purchaseOrderService.createPurchaseOrder(
                        purchaseOrder,
                        vendorId
                );

        return ResponseEntity.ok(createdPurchaseOrder);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {

        return ResponseEntity.ok(
                purchaseOrderService.getAllPurchaseOrders()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(
            @PathVariable Long id) {

        return purchaseOrderService.getPurchaseOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PutMapping("/{id}/delivery")
    public ResponseEntity<PurchaseOrder> updateDelivery(
            @PathVariable Long id,
            @RequestParam LocalDate actualDeliveryDate,
            @RequestParam Integer actualDeliveredQuantity) {

        PurchaseOrder updatedPurchaseOrder =
                purchaseOrderService.updateDelivery(
                        id,
                        actualDeliveryDate,
                        actualDeliveredQuantity
                );

        return ResponseEntity.ok(updatedPurchaseOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(
            @PathVariable Long id) {

        purchaseOrderService.deletePurchaseOrder(id);

        return ResponseEntity.noContent().build();
    }
}
