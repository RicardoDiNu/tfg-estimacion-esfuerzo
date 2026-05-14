package com.uniovi.estimacion.repositories.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FunctionPointWeightMatrixEntryRepository
        extends JpaRepository<FunctionPointWeightMatrixEntry, Long> {

    List<FunctionPointWeightMatrixEntry>
    findByFunctionPointAnalysisEstimationProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    Optional<FunctionPointWeightMatrixEntry>
    findByIdAndFunctionPointAnalysisEstimationProjectId(Long id, Long projectId);

    Optional<FunctionPointWeightMatrixEntry>
    findByFunctionPointAnalysisIdAndFunctionTypeAndComplexity(
            Long analysisId,
            FunctionPointFunctionType functionType,
            FunctionPointComplexity complexity
    );
}