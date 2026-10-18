package com.vendorai.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "vendor_compliance")
public class VendorCompliance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    @Column(nullable = false)
    private String documentName;
    private String referenceNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    @Column(length = 1000)
    private String notes;
    private LocalDateTime createdAt;
    public VendorCompliance() {}
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    @Transient
    public String getComplianceStatus() {
        if (expiryDate == null) {
            return "VALIDITY_NOT_SPECIFIED";
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        if (!expiryDate.isAfter(today.plusDays(30))) {
            return "EXPIRING";
        }
        return "VALID";
    }
}