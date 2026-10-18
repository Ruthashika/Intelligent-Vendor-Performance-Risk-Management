package com.vendorai.backend.service;

import com.vendorai.backend.dto.VendorPortalResponse;
import com.vendorai.backend.entity.Alert;
import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.repository.AlertRepository;
import com.vendorai.backend.repository.PurchaseOrderRepository;
import com.vendorai.backend.repository.VendorPerformanceScoreRepository;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorPortalService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorPerformanceScoreRepository performanceScoreRepository;
    private final AlertRepository alertRepository;

    public VendorPortalService(
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            VendorPerformanceScoreRepository performanceScoreRepository,
            AlertRepository alertRepository) {

        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.performanceScoreRepository = performanceScoreRepository;
        this.alertRepository = alertRepository;
    }

    public VendorPortalResponse getVendorPortal(Long vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));

        List<PurchaseOrder> orders =
                purchaseOrderRepository.findAll()
                        .stream()
                        .filter(order ->
                                order.getVendor() != null &&
                                order.getVendor()
                                        .getId()
                                        .equals(vendorId))
                        .toList();

        List<VendorPerformanceScore> performanceHistory =
                performanceScoreRepository
                        .findByVendorId(vendorId);

        VendorPerformanceScore latestPerformance = null;

        if (!performanceHistory.isEmpty()) {

            latestPerformance =
                    performanceHistory.stream()
                            .max((score1, score2) ->
                                    score1.getCalculatedAt()
                                            .compareTo(
                                                    score2.getCalculatedAt()
                                            ))
                            .orElse(null);
        }

        List<Alert> alerts =
                alertRepository.findByVendorId(vendorId);

        VendorPortalResponse response =
                new VendorPortalResponse();

        response.setVendor(vendor);
        response.setOrders(orders);
        response.setPerformance(latestPerformance);
        response.setAlerts(alerts);

        return response;
    }
}