package com.vendorai.backend.repository;

import com.vendorai.backend.entity.VendorPerformanceScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorPerformanceScoreRepository
        extends JpaRepository<VendorPerformanceScore, Long> {

    List<VendorPerformanceScore> findByVendorId(Long vendorId);
}