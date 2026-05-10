package com.uniovi.estimacion.unit.effortconversions.delphi;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiIteration;
import com.uniovi.estimacion.repositories.effortconversions.delphi.DelphiEstimationRepository;
import com.uniovi.estimacion.services.effortconversions.LinearEffortModel;
import com.uniovi.estimacion.services.effortconversions.delphi.DelphiEstimationService;
import com.uniovi.estimacion.services.sizeanalyses.SizeAnalysisModuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Delphi estimation service unit tests")
class DelphiEstimationServiceTest {

    @Mock
    private DelphiEstimationRepository delphiEstimationRepository;

    @InjectMocks
    private DelphiEstimationService service;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates an unfinished estimation: regression slope and intercept are null.
     */
    private DelphiEstimation unfinishedEstimation(double minSize, double maxSize) {
        DelphiEstimation e = new DelphiEstimation();
        e.setId(1L);
        e.setMinimumModuleId(1L);
        e.setMinimumModuleNameSnapshot("min-module");
        e.setMinimumModuleSizeSnapshot(minSize);
        e.setMaximumModuleId(2L);
        e.setMaximumModuleNameSnapshot("max-module");
        e.setMaximumModuleSizeSnapshot(maxSize);
        e.setExpertCount(3);
        e.setAcceptableDeviationPercentage(10.0);
        e.setMaximumIterations(3);
        e.setActive(true);
        e.setOutdated(false);
        // regressionSlope and regressionIntercept remain null -> isFinished() == false
        return e;
    }

    /**
     * Creates a finished estimation with slope and intercept already set.
     */
    private DelphiEstimation finishedEstimation(double intercept, double slope) {
        DelphiEstimation e = new DelphiEstimation();
        e.setMinimumModuleSizeSnapshot(10.0);
        e.setMaximumModuleSizeSnapshot(30.0);
        e.setRegressionIntercept(intercept);
        e.setRegressionSlope(slope);
        e.setActive(true);
        e.setOutdated(false);
        return e;
    }

    private DelphiExpertEstimate expertEstimate(String alias, double minEffort, double maxEffort) {
        DelphiExpertEstimate e = new DelphiExpertEstimate();
        e.setEvaluatorAlias(alias);
        e.setMinimumModuleEstimatedEffortHours(minEffort);
        e.setMaximumModuleEstimatedEffortHours(maxEffort);
        return e;
    }

    // -------------------------------------------------------------------------
    // A. Regression line calculation (LinearEffortModel)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Regression line calculation")
    class RegressionLine {

        @Test
        @DisplayName("calculates slope = (effortMax - effortMin) / (sizeMax - sizeMin)")
        void calculatesSlopeCorrectly() {
            // given: sizeMin=10,effortMin=100, sizeMax=30,effortMax=300 -> slope=(300-100)/(30-10)=10
            LinearEffortModel model = LinearEffortModel.fromTwoPoints(10.0, 100.0, 30.0, 300.0);

            // then
            assertEquals(10.0, model.slope(), 0.001);
        }

        @Test
        @DisplayName("calculates intercept = 0 when regression passes through origin")
        void calculatesInterceptZeroWhenRegressionPassesThroughOrigin() {
            // given: slope=10, intercept = 100 - 10*10 = 0
            LinearEffortModel model = LinearEffortModel.fromTwoPoints(10.0, 100.0, 30.0, 300.0);

            // then
            assertEquals(0.0, model.intercept(), 0.001);
        }

        @Test
        @DisplayName("calculates non-zero intercept: sizeMin=10,effortMin=150, sizeMax=30,effortMax=350 -> slope=10, intercept=50")
        void calculatesNonZeroIntercept() {
            // given: slope=(350-150)/(30-10)=10; intercept=150-10*10=50
            LinearEffortModel model = LinearEffortModel.fromTwoPoints(10.0, 150.0, 30.0, 350.0);

            // then
            assertEquals(10.0, model.slope(), 0.001);
            assertEquals(50.0, model.intercept(), 0.001);
        }

        @Test
        @DisplayName("LinearEffortModel.fromTwoPoints throws when sizes are equal (division by zero)")
        void fromTwoPointsThrowsWhenSizesAreEqual() {
            assertThrows(IllegalArgumentException.class, () ->
                    LinearEffortModel.fromTwoPoints(10.0, 100.0, 10.0, 200.0));
        }

        @Test
        @DisplayName("applyFinalCalibration sets slope and intercept on estimation via regression")
        void applyFinalCalibrationSetsSlopeAndIntercept() {
            // given: sizeMin=10, sizeMax=30; effortMin=100, effortMax=300 -> slope=10, intercept=0
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            service.applyFinalCalibration(estimation, 100.0, 300.0);

            // then
            assertEquals(10.0, estimation.getRegressionSlope(), 0.001);
            assertEquals(0.0, estimation.getRegressionIntercept(), 0.001);
        }

        @Test
        @DisplayName("applyFinalCalibration with intercept=50: effortMin=150, effortMax=350 -> slope=10, intercept=50")
        void applyFinalCalibrationWithNonZeroIntercept() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            service.applyFinalCalibration(estimation, 150.0, 350.0);

            // then
            assertEquals(10.0, estimation.getRegressionSlope(), 0.001);
            assertEquals(50.0, estimation.getRegressionIntercept(), 0.001);
        }

        @Test
        @DisplayName("applyFinalCalibration throws IllegalStateException when min and max sizes are equal")
        void applyFinalCalibrationThrowsWhenSizesAreEqual() {
            // given: same size for min and max module -> regression impossible
            DelphiEstimation estimation = unfinishedEstimation(10.0, 10.0);

            // then
            assertThrows(IllegalStateException.class, () ->
                    service.applyFinalCalibration(estimation, 100.0, 200.0));
        }
    }

    // -------------------------------------------------------------------------
    // B. Module effort calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Module effort calculation")
    class ModuleEffort {

        @Test
        @DisplayName("intercept=0, slope=10, size=5 -> effort=50")
        void calculatesEffortWithZeroIntercept() {
            // given
            DelphiEstimation estimation = finishedEstimation(0.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(estimation, 5.0);

            // then
            assertEquals(50.0, result, 0.001);
        }

        @Test
        @DisplayName("intercept=50, slope=10, size=5 -> effort=100")
        void calculatesEffortWithNonZeroIntercept() {
            // given
            DelphiEstimation estimation = finishedEstimation(50.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(estimation, 5.0);

            // then
            assertEquals(100.0, result, 0.001);
        }

        @Test
        @DisplayName("throws IllegalStateException when estimation is not finished (no regression)")
        void throwsWhenEstimationIsNotFinished() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);

            // then
            assertThrows(IllegalStateException.class, () ->
                    service.calculateEstimatedEffortHours(estimation, 15.0));
        }

        @Test
        @DisplayName("throws IllegalStateException when estimation is null")
        void throwsWhenEstimationIsNull() {
            assertThrows(IllegalStateException.class, () ->
                    service.calculateEstimatedEffortHours(null, 15.0));
        }
    }

    // -------------------------------------------------------------------------
    // C. Total effort calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Total effort calculation")
    class TotalEffort {

        @Test
        @DisplayName("calculates total effort as sum of individual module efforts (map overload)")
        void calculatesTotalEffortAsSumViaMap() {
            // given: a=0, b=10 -> effort(10)=100, effort(20)=200, effort(5)=50 -> total=350
            DelphiEstimation estimation = finishedEstimation(0.0, 10.0);

            // when
            double result = service.calculateTotalEstimatedEffortHours(estimation,
                    Map.of(1L, 10.0, 2L, 20.0, 3L, 5.0));

            // then
            assertEquals(350.0, result, 0.001);
        }

        @Test
        @DisplayName("calculates total effort via SizeAnalysisModuleResult list: a=50,b=10, sizes=[5,10] -> 250")
        void calculatesTotalEffortViaModuleResultList() {
            // given: a=50, b=10 -> effort(5)=100, effort(10)=150 -> total=250
            DelphiEstimation estimation = finishedEstimation(50.0, 10.0);

            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 5.0),
                    new SizeAnalysisModuleResult(2L, "mod-b", 10.0)
            );

            // when
            Double result = service.calculateTotalEstimatedEffortHours(estimation, modules);

            // then
            assertNotNull(result);
            assertEquals(250.0, result, 0.001);
        }

        @Test
        @DisplayName("returns null when estimation has no regression (list overload)")
        void returnsNullWhenRegressionMissing() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 5.0)
            );

            // when
            Double result = service.calculateTotalEstimatedEffortHours(estimation, modules);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("returns null when module list is null (list overload)")
        void returnsNullWhenModuleListIsNull() {
            // given
            DelphiEstimation estimation = finishedEstimation(0.0, 10.0);

            // when
            Double result = service.calculateTotalEstimatedEffortHours(estimation,
                    (List<SizeAnalysisModuleResult>) null);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("returns null when estimation is null (list overload)")
        void returnsNullWhenEstimationIsNull() {
            // given
            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 5.0)
            );

            // when
            Double result = service.calculateTotalEstimatedEffortHours(null, modules);

            // then
            assertNull(result);
        }
    }

    // -------------------------------------------------------------------------
    // D. State: isFinished
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Estimation state")
    class EstimationState {

        @Test
        @DisplayName("isFinished returns true when slope and intercept are both set")
        void isFinishedWhenRegressionIsSet() {
            // given
            DelphiEstimation estimation = finishedEstimation(0.0, 10.0);

            // then
            assertTrue(service.isFinished(estimation));
        }

        @Test
        @DisplayName("isFinished returns false when slope is null")
        void isNotFinishedWhenSlopeIsNull() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);

            // then
            assertFalse(service.isFinished(estimation));
        }

        @Test
        @DisplayName("isFinished returns false when estimation is null")
        void isNotFinishedWhenEstimationIsNull() {
            assertFalse(service.isFinished(null));
        }
    }

    // -------------------------------------------------------------------------
    // E. Convergence via registerIteration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Convergence detection via registerIteration")
    class ConvergenceDetection {

        @Test
        @DisplayName("convergent iteration (deviation=0%) sets finalIteration=true and computes regression")
        void convergentIterationSetsFinalIterationAndRegression() {
            // given: 3 experts with identical estimates -> deviation = 0% < 10%
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.findById(1L)).thenReturn(Optional.of(estimation));
            when(delphiEstimationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<DelphiExpertEstimate> estimates = List.of(
                    expertEstimate("expert1", 100.0, 300.0),
                    expertEstimate("expert2", 100.0, 300.0),
                    expertEstimate("expert3", 100.0, 300.0)
            );

            // when
            service.registerIteration(1L, estimates);

            // then: iteration is final due to convergence
            DelphiIteration iteration = estimation.getIterations().get(0);
            assertTrue(iteration.getFinalIteration());
            assertTrue(iteration.getAcceptedByDeviation());

            // regression is now set: sizeMin=10, sizeMax=30, effortMin=100, effortMax=300
            assertTrue(estimation.isFinished());
            assertEquals(10.0, estimation.getRegressionSlope(), 0.001);
            assertEquals(0.0, estimation.getRegressionIntercept(), 0.001);
        }

        @Test
        @DisplayName("non-convergent iteration with remaining iterations sets finalIteration=false")
        void nonConvergentIterationDoesNotFinalizeWhenIterationsRemain() {
            // given: 3 experts with highly divergent estimates -> deviation >> 10%
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.findById(1L)).thenReturn(Optional.of(estimation));
            when(delphiEstimationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Divergent min estimates: [50, 300, 100] -> avg=150, dev=(300-50)/150*100=166%
            List<DelphiExpertEstimate> estimates = List.of(
                    expertEstimate("expert1", 50.0, 100.0),
                    expertEstimate("expert2", 300.0, 600.0),
                    expertEstimate("expert3", 100.0, 200.0)
            );

            // when
            service.registerIteration(1L, estimates);

            // then: iteration is NOT final (1 < 3 max iterations; divergent)
            DelphiIteration iteration = estimation.getIterations().get(0);
            assertFalse(iteration.getFinalIteration());
            assertFalse(iteration.getAcceptedByDeviation());
            assertFalse(estimation.isFinished());
        }
    }

    // -------------------------------------------------------------------------
    // E. Expert count validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Expert count validation")
    class ExpertCountValidation {

        @Test
        @DisplayName("registerIteration rejects fewer expert estimates than the configured count")
        void rejectsFewerExpertsThanConfigured() {
            // given: estimation expects 3 experts but only 1 is provided
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.findById(1L)).thenReturn(Optional.of(estimation));

            List<DelphiExpertEstimate> estimates = List.of(
                    expertEstimate("expert1", 100.0, 200.0)
            );

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.registerIteration(1L, estimates));
        }

        @Test
        @DisplayName("registerIteration rejects empty expert estimate list")
        void rejectsEmptyExpertList() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.findById(1L)).thenReturn(Optional.of(estimation));

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.registerIteration(1L, List.of()));
        }
    }

    // -------------------------------------------------------------------------
    // F. Boundary cases: canStartCalibration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Calibration prerequisite check (canStartCalibration)")
    class CalibrationCheck {

        @Test
        @DisplayName("returns true with 2 modules having distinct positive sizes (map)")
        void canStartWithTwoDistinctPositiveSizesMap() {
            assertTrue(service.canStartCalibration(Map.of(1L, 10.0, 2L, 30.0)));
        }

        @Test
        @DisplayName("returns false when both modules share the same size (map)")
        void cannotStartWhenSizesAreEqualMap() {
            assertFalse(service.canStartCalibration(Map.of(1L, 10.0, 2L, 10.0)));
        }

        @Test
        @DisplayName("returns false with only 1 module (map)")
        void cannotStartWithOnlyOneModuleMap() {
            assertFalse(service.canStartCalibration(Map.of(1L, 15.0)));
        }

        @Test
        @DisplayName("returns true with 2 modules having distinct positive sizes (list)")
        void canStartWithTwoDistinctPositiveSizesList() {
            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 10.0),
                    new SizeAnalysisModuleResult(2L, "mod-b", 30.0)
            );
            assertTrue(service.canStartCalibration(modules));
        }

        @Test
        @DisplayName("returns false when min and max sizes are equal (list)")
        void cannotStartWhenSizesAreEqualList() {
            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 10.0),
                    new SizeAnalysisModuleResult(2L, "mod-b", 10.0)
            );
            assertFalse(service.canStartCalibration(modules));
        }

        @Test
        @DisplayName("returns false with only 1 module (list)")
        void cannotStartWithOnlyOneModuleList() {
            List<SizeAnalysisModuleResult> modules = List.of(
                    new SizeAnalysisModuleResult(1L, "mod-a", 10.0)
            );
            assertFalse(service.canStartCalibration(modules));
        }
    }
}
