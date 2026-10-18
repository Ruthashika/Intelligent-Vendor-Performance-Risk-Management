package com.vendorai.backend.repository;
import com.vendorai.backend.entity.VendorCompliance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VendorComplianceRepository
        extends JpaRepository<VendorCompliance, Long> {
    List<VendorCompliance> findByVendorId(Long vendorId);
}