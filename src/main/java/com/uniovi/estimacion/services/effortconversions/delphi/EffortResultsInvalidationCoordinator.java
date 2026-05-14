package com.uniovi.estimacion.services.effortconversions.delphi;

import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EffortResultsInvalidationCoordinator {

    private final DelphiEstimationService delphiEstimationService;

    public void invalidateForSizeAnalysis(SizeAnalysis sourceAnalysis) {
        if (sourceAnalysis == null || sourceAnalysis.getId() == null) {
            return;
        }

        delphiEstimationService.deleteAllBySourceAnalysis(sourceAnalysis);
    }
}