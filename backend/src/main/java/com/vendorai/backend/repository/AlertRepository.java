package com.vendorai.backend.repository;

import com.vendorai.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByVendorId(Long vendorId);

    List<Alert> findByStatus(String status);
}