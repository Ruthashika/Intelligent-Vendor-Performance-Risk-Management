package com.vendorai.backend.service;

import com.vendorai.backend.entity.Vendor;
import com.vendorai.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    public Vendor updateVendor(Long id, Vendor updatedVendor) {

        Vendor existingVendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        existingVendor.setVendorName(updatedVendor.getVendorName());
        existingVendor.setCategory(updatedVendor.getCategory());
        existingVendor.setContactPerson(updatedVendor.getContactPerson());
        existingVendor.setEmail(updatedVendor.getEmail());
        existingVendor.setPhone(updatedVendor.getPhone());
        existingVendor.setAddress(updatedVendor.getAddress());
        existingVendor.setStatus(updatedVendor.getStatus());

        return vendorRepository.save(existingVendor);
    }

    public void deleteVendor(Long id) {

        if (!vendorRepository.existsById(id)) {
            throw new RuntimeException("Vendor not found");
        }

        vendorRepository.deleteById(id);
    }
}