package com.vendorai.backend.service;
import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.entity.VendorCompliance;
import com.vendorai.backend.repository.VendorComplianceRepository;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
@Service
public class VendorComplianceService {
    private final VendorComplianceRepository complianceRepository;
    private final VendorRepository vendorRepository;
    public VendorComplianceService(
            VendorComplianceRepository complianceRepository,
            VendorRepository vendorRepository) {
        this.complianceRepository = complianceRepository;
        this.vendorRepository = vendorRepository;
    }
    @Transactional
    public VendorCompliance create(Long vendorId, VendorCompliance input) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        validate(input);
        input.setId(null);
        input.setVendor(vendor);
        return complianceRepository.save(input);
    }
    public List<VendorCompliance> getAll() {
        return complianceRepository.findAll();
    }
    public List<VendorCompliance> getByVendor(Long vendorId) {
        if (!vendorRepository.existsById(vendorId)) {
            throw new RuntimeException("Vendor not found");
        }
        List<VendorCompliance> records =
                complianceRepository.findByVendorId(vendorId);
        if (records.isEmpty()) {
            throw new RuntimeException(
                    "MISSING: No compliance documents recorded for this vendor");
        }
        return records;
    }
    @Transactional
    public VendorCompliance update(Long id, VendorCompliance input) {
        VendorCompliance existing = complianceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Compliance record not found"));
        validate(input);
        existing.setDocumentName(input.getDocumentName().trim());
        existing.setReferenceNumber(input.getReferenceNumber());
        existing.setIssueDate(input.getIssueDate());
        existing.setExpiryDate(input.getExpiryDate());
        existing.setNotes(input.getNotes());
        return complianceRepository.save(existing);
    }
    public void delete(Long id) {
        if (!complianceRepository.existsById(id)) {
            throw new RuntimeException("Compliance record not found");
        }
        complianceRepository.deleteById(id);
    }
    private void validate(VendorCompliance input) {
        if (input == null || input.getDocumentName() == null
                || input.getDocumentName().isBlank()) {
            throw new RuntimeException("Document name is required");
        }
        if (input.getIssueDate() != null
                && input.getExpiryDate() != null
                && input.getExpiryDate().isBefore(input.getIssueDate())) {
            throw new RuntimeException(
                    "Expiry date cannot be before issue date");
        }
        if (input.getExpiryDate() != null
                && input.getExpiryDate().isBefore(LocalDate.of(1900, 1, 1))) {
            throw new RuntimeException("Invalid expiry date");
        }
    }
}