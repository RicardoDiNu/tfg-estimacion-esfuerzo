package com.uniovi.estimacion.unit.costs;

import com.uniovi.estimacion.services.costs.CostCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cost calculation service unit tests")
class CostCalculationServiceTest {

    private CostCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CostCalculationService();
    }

    @Nested
    @DisplayName("Cost calculation")
    class CostCalculation {

        @Test
        @DisplayName("calculates cost from effort 10h and rate 50 -> 500.00")
        void calculatesCostFromEffortAndRate() {
            // given
            Double effortHours = 10.0;
            BigDecimal hourlyRate = new BigDecimal("50.00");

            // when
            BigDecimal result = service.calculateCost(effortHours, hourlyRate);

            // then
            assertNotNull(result);
            assertEquals(0, new BigDecimal("500.00").compareTo(result));
        }

        @Test
        @DisplayName("calculates cost from fractional effort 12.5h and rate 40 -> 500.00")
        void calculatesCostWithFractionalEffort() {
            // given
            Double effortHours = 12.5;
            BigDecimal hourlyRate = new BigDecimal("40.00");

            // when
            BigDecimal result = service.calculateCost(effortHours, hourlyRate);

            // then
            assertNotNull(result);
            assertEquals(0, new BigDecimal("500.00").compareTo(result));
        }

        @Test
        @DisplayName("calculates zero cost when effort is 0")
        void calculatesZeroCostWhenEffortIsZero() {
            // given
            Double effortHours = 0.0;
            BigDecimal hourlyRate = new BigDecimal("50.00");

            // when
            BigDecimal result = service.calculateCost(effortHours, hourlyRate);

            // then
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        @DisplayName("rounds result to 2 decimal places using HALF_UP")
        void roundsToTwoDecimalPlacesUsingHalfUp() {
            // given
            // 1.0 * 33.335 = 33.335 -> rounds to 33.34
            Double effortHours = 1.0;
            BigDecimal hourlyRate = new BigDecimal("33.335");

            // when
            BigDecimal result = service.calculateCost(effortHours, hourlyRate);

            // then
            assertNotNull(result);
            assertEquals(2, result.scale());
            assertEquals(0, new BigDecimal("33.34").compareTo(result));
        }
    }

    @Nested
    @DisplayName("Null inputs")
    class NullInputs {

        @Test
        @DisplayName("returns null when effort is null")
        void returnsNullWhenEffortIsNull() {
            // when
            BigDecimal result = service.calculateCost(null, new BigDecimal("50.00"));

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("returns null when hourly rate is null")
        void returnsNullWhenHourlyRateIsNull() {
            // when
            BigDecimal result = service.calculateCost(10.0, null);

            // then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Negative inputs")
    class NegativeInputs {

        @Test
        @DisplayName("returns null when effort is negative")
        void returnsNullWhenEffortIsNegative() {
            // when
            BigDecimal result = service.calculateCost(-1.0, new BigDecimal("50.00"));

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("returns null when hourly rate is negative")
        void returnsNullWhenHourlyRateIsNegative() {
            // when
            BigDecimal result = service.calculateCost(10.0, new BigDecimal("-10.00"));

            // then
            assertNull(result);
        }
    }
}
