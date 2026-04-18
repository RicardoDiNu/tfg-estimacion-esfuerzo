package com.uniovi.estimacion.repositories;

import com.uniovi.estimacion.entities.functionpoints.TransactionalFunction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionalFunctionRepository extends CrudRepository<TransactionalFunction, Long> {
}