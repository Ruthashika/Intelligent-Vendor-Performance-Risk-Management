package com.vendorai.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_performance_scores")
public class VendorPerformanceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private Double onTimeDeliveryRate;

    private Double averageDeliveryDelay;

    private Double qualityIssueRate;

    private Double fulfillmentRate;

    private Double performanceScore;

    private LocalDateTime calculatedAt;

    public VendorPerformanceScore() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Double getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }

    public void setOnTimeDeliveryRate(Double onTimeDeliveryRate) {
        this.onTimeDeliveryRate = onTimeDeliveryRate;
    }

    public Double getAverageDeliveryDelay() {
        return averageDeliveryDelay;
    }

    public void setAverageDeliveryDelay(Double averageDeliveryDelay) {
        this.averageDeliveryDelay = averageDeliveryDelay;
    }

    public Double getQualityIssueRate() {
        return qualityIssueRate;
    }

    public void setQualityIssueRate(Double qualityIssueRate) {
        this.qualityIssueRate = qualityIssueRate;
    }

    public Double getFulfillmentRate() {
        return fulfillmentRate;
    }

    public void setFulfillmentRate(Double fulfillmentRate) {
        this.fulfillmentRate = fulfillmentRate;
    }

    public Double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(Double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}