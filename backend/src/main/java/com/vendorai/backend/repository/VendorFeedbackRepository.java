package com.vendorai.backend.repository;

import com.vendorai.backend.entity.VendorFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorFeedbackRepository
        extends JpaRepository<VendorFeedback, Long> {

    List<VendorFeedback> findByVendorId(Long vendorId);

    List<VendorFeedback> findByPurchaseOrderId(Long purchaseOrderId);
}
