package com.vendorai.backend.dto;

import com.vendorai.backend.entity.Alert;
import com.vendorai.backend.entity.PurchaseOrder;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.entity.VendorPerformanceScore;

import java.util.List;

public class VendorPortalResponse {

    private Vendor vendor;

    private List<PurchaseOrder> orders;

    private VendorPerformanceScore performance;

    private List<Alert> alerts;

    public VendorPortalResponse() {
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public List<PurchaseOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<PurchaseOrder> orders) {
        this.orders = orders;
    }

    public VendorPerformanceScore getPerformance() {
        return performance;
    }

    public void setPerformance(VendorPerformanceScore performance) {
        this.performance = performance;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
}