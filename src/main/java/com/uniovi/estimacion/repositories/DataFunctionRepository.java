package com.uniovi.estimacion.repositories;

import com.uniovi.estimacion.entities.functionpoints.DataFunction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DataFunctionRepository extends CrudRepository<DataFunction, Long> {

    List<DataFunction> findByFunctionPointAnalysisId(Long functionPointAnalysisId);

    Optional<DataFunction> findByIdAndFunctionPointAnalysisId(Long id, Long functionPointAnalysisId);
}