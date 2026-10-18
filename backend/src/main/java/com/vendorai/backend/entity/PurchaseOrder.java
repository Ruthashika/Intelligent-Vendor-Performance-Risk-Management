package com.vendorai.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String poNumber;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    private LocalDate actualDeliveryDate;
private Integer quantity;

private Integer actualDeliveredQuantity;

private Double unitPrice;

    private Double totalAmount;

    private String priority;

    private String status;
    @Column(name = "predicted_delivery_risk")
private String predictedDeliveryRisk;

@Column(name = "risk_probability")
private Double riskProbability;

@Column(name = "risk_explanation", length = 1000)
private String riskExplanation;

public String getPredictedDeliveryRisk() {
    return predictedDeliveryRisk;
}

public void setPredictedDeliveryRisk(String predictedDeliveryRisk) {
    this.predictedDeliveryRisk = predictedDeliveryRisk;
}

public Double getRiskProbability() {
    return riskProbability;
}

public void setRiskProbability(Double riskProbability) {
    this.riskProbability = riskProbability;
}

public String getRiskExplanation() {
    return riskExplanation;
}

public void setRiskExplanation(String riskExplanation) {
    this.riskExplanation = riskExplanation;
}

    private LocalDateTime createdAt;

    public PurchaseOrder() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Integer getActualDeliveredQuantity() {
    return actualDeliveredQuantity;
}

public void setActualDeliveredQuantity(Integer actualDeliveredQuantity) {
    this.actualDeliveredQuantity = actualDeliveredQuantity;
}

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null || status.isBlank()) {
            status = "CREATED";
        }
    }
}