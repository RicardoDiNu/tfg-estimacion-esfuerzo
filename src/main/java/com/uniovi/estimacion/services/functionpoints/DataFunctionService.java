package com.uniovi.estimacion.services.functionpoints;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import com.uniovi.estimacion.repositories.DataFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataFunctionService {

    private final DataFunctionRepository dataFunctionRepository;

    public List<DataFunction> getByAnalysisId(Long analysisId) {
        return dataFunctionRepository.findByFunctionPointAnalysisId(analysisId);
    }

    public void save(DataFunction dataFunction) {
        dataFunctionRepository.save(dataFunction);
    }
}