package com.uniovi.estimacion.services.effortconversions;

import com.uniovi.estimacion.entities.analysis.SizeAnalysis;
import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EffortResultsInvalidationCoordinator {

    private final DelphiEstimationService delphiEstimationService;

    public void deleteForFunctionPointAnalysis(FunctionPointAnalysis analysis) {
        delphiEstimationService.deleteAllBySourceAnalysis(analysis);
    }

    public void deleteForSizeAnalysis(SizeAnalysis analysis) {
        delphiEstimationService.deleteAllBySourceAnalysis(analysis);
    }
}