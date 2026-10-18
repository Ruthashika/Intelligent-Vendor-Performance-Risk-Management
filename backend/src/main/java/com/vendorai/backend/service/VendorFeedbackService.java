package com.vendorai.backend.service;

import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.entity.VendorFeedback;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.VendorFeedbackRepository;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorFeedbackService {

    private final VendorFeedbackRepository feedbackRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public VendorFeedbackService(
            VendorFeedbackRepository feedbackRepository,
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository) {

        this.feedbackRepository = feedbackRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public VendorFeedback createFeedback(
            VendorFeedback feedback,
            Long vendorId,
            Long purchaseOrderId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findById(purchaseOrderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Purchase order not found"));

        if (!purchaseOrder.getVendor().getId().equals(vendorId)) {
            throw new RuntimeException(
                    "Purchase order does not belong to this vendor");
        }

        if (feedback.getRating() == null ||
                feedback.getRating() < 1 ||
                feedback.getRating() > 5) {

            throw new RuntimeException(
                    "Rating must be between 1 and 5");
        }

        feedback.setVendor(vendor);
        feedback.setPurchaseOrder(purchaseOrder);

        return feedbackRepository.save(feedback);
    }

    public List<VendorFeedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public Optional<VendorFeedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public List<VendorFeedback> getVendorFeedback(Long vendorId) {

        return feedbackRepository.findByVendorId(vendorId);
    }

    public List<VendorFeedback> getPurchaseOrderFeedback(
            Long purchaseOrderId) {

        return feedbackRepository
                .findByPurchaseOrderId(purchaseOrderId);
    }

    public void deleteFeedback(Long id) {

        if (!feedbackRepository.existsById(id)) {
            throw new RuntimeException("Feedback not found");
        }

        feedbackRepository.deleteById(id);
    }
}