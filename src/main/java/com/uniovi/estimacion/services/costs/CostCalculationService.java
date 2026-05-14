package com.uniovi.estimacion.services.costs;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CostCalculationService {

    public BigDecimal calculateCost(Double effortHours, BigDecimal hourlyRate) {
        if (effortHours == null || hourlyRate == null) {
            return null;
        }

        if (effortHours < 0 || hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        return BigDecimal.valueOf(effortHours)
                .multiply(hourlyRate)
                .setScale(2, RoundingMode.HALF_UP);
    }
}