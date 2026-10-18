package com.vendorai.backend.service;

import com.vendorai.backend.entity.VendorPerformanceScore;
import com.vendorai.backend.repository.VendorPerformanceScoreRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VendorComparisonService {

    private final VendorPerformanceScoreRepository
            performanceScoreRepository;

    public VendorComparisonService(
            VendorPerformanceScoreRepository performanceScoreRepository) {

        this.performanceScoreRepository =
                performanceScoreRepository;
    }

    public List<VendorPerformanceScore> getLatestVendorPerformance() {

        List<VendorPerformanceScore> allScores =
                performanceScoreRepository.findAll();

        /*
         * A vendor may have multiple historical performance
         * snapshots.
         *
         * We only want the latest snapshot of each vendor
         * for the current comparison.
         */
        Map<Long, VendorPerformanceScore> latestScores =
                allScores.stream()
                        .filter(score -> score.getVendor() != null)
                        .collect(Collectors.toMap(
                                score -> score.getVendor().getId(),
                                Function.identity(),
                                (score1, score2) ->
                                        score1.getCalculatedAt()
                                                .isAfter(
                                                        score2.getCalculatedAt()
                                                )
                                                ? score1
                                                : score2
                        ));

        /*
         * Sort vendors by their current performance score.
         */
        return latestScores.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                VendorPerformanceScore::getPerformanceScore
                        ).reversed()
                )
                .toList();
    }

    public List<VendorPerformanceScore> compareVendors(
            List<Long> vendorIds) {

        List<VendorPerformanceScore> latestScores =
                getLatestVendorPerformance();

        return latestScores.stream()
                .filter(score ->
                        vendorIds.contains(
                                score.getVendor().getId()
                        )
                )
                .toList();
    }
}