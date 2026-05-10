package com.uniovi.estimacion.unit.effortconversions.delphi;

import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiEstimation;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiExpertEstimate;
import com.uniovi.estimacion.entities.effortconversions.delphi.DelphiIteration;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.SizeAnalysis;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    private SizeAnalysis validSourceAnalysis() {
        EstimationProject project = new EstimationProject();
        project.setId(1L);
        project.setName("Test project");

        SizeAnalysis sourceAnalysis = mock(SizeAnalysis.class);
        lenient().when(sourceAnalysis.getId()).thenReturn(10L);
        lenient().when(sourceAnalysis.getEstimationProject()).thenReturn(project);
        lenient().when(sourceAnalysis.getCalculatedSizeValue()).thenReturn(40.0);
        lenient().when(sourceAnalysis.getTechniqueCode()).thenReturn("FP");
        lenient().when(sourceAnalysis.getSizeUnitCode()).thenReturn("FP");

        return sourceAnalysis;
    }

    private List<SizeAnalysisModuleResult> validModuleResults() {
        return List.of(
                new SizeAnalysisModuleResult(1L, "min-module", 10.0),
                new SizeAnalysisModuleResult(2L, "max-module", 30.0)
        );
    }

    // -------------------------------------------------------------------------
    // A. Initial configuration validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Initial configuration validation")
    class InitialConfigurationValidation {

        @Test
        @DisplayName("createInitialEstimation accepts two experts")
        void createInitialEstimationAcceptsTwoExperts() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();
            List<SizeAnalysisModuleResult> modules = validModuleResults();

            when(delphiEstimationRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            DelphiEstimation result = service.createInitialEstimation(
                    sourceAnalysis,
                    modules,
                    10.0,
                    3,
                    2
            );

            // then
            assertNotNull(result);
            assertEquals(2, result.getExpertCount());
            assertEquals(10.0, result.getAcceptableDeviationPercentage(), 0.001);
            assertEquals(3, result.getMaximumIterations());
            assertEquals(1L, result.getMinimumModuleId());
            assertEquals(2L, result.getMaximumModuleId());
            assertEquals(10.0, result.getMinimumModuleSizeSnapshot(), 0.001);
            assertEquals(30.0, result.getMaximumModuleSizeSnapshot(), 0.001);
            assertTrue(result.getActive());
            assertFalse(result.getOutdated());
        }

        @Test
        @DisplayName("createInitialEstimation rejects one expert")
        void createInitialEstimationRejectsOneExpert() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            10.0,
                            3,
                            1
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects null expert count")
        void createInitialEstimationRejectsNullExpertCount() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            10.0,
                            3,
                            null
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects null acceptable deviation")
        void createInitialEstimationRejectsNullAcceptableDeviation() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            null,
                            3,
                            2
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects zero acceptable deviation")
        void createInitialEstimationRejectsZeroAcceptableDeviation() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            0.0,
                            3,
                            2
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects negative acceptable deviation")
        void createInitialEstimationRejectsNegativeAcceptableDeviation() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            -1.0,
                            3,
                            2
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects null maximum iterations")
        void createInitialEstimationRejectsNullMaximumIterations() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            10.0,
                            null,
                            2
                    ));
        }

        @Test
        @DisplayName("createInitialEstimation rejects zero maximum iterations")
        void createInitialEstimationRejectsZeroMaximumIterations() {
            // given
            SizeAnalysis sourceAnalysis = validSourceAnalysis();

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.createInitialEstimation(
                            sourceAnalysis,
                            validModuleResults(),
                            10.0,
                            0,
                            2
                    ));
        }
    }

    // -------------------------------------------------------------------------
    // B. Regression line calculation (LinearEffortModel)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Regression line calculation")
    class RegressionLine {

        @Test
        @DisplayName("calculates slope = (effortMax - effortMin) / (sizeMax - sizeMin)")
        void calculatesSlopeCorrectly() {
            // given
            LinearEffortModel model = LinearEffortModel.fromTwoPoints(10.0, 100.0, 30.0, 300.0);

            // then
            assertEquals(10.0, model.slope(), 0.001);
        }

        @Test
        @DisplayName("calculates intercept = 0 when regression passes through origin")
        void calculatesInterceptZeroWhenRegressionPassesThroughOrigin() {
            // given
            LinearEffortModel model = LinearEffortModel.fromTwoPoints(10.0, 100.0, 30.0, 300.0);

            // then
            assertEquals(0.0, model.intercept(), 0.001);
        }

        @Test
        @DisplayName("calculates non-zero intercept: sizeMin=10,effortMin=150, sizeMax=30,effortMax=350 -> slope=10, intercept=50")
        void calculatesNonZeroIntercept() {
            // given
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
            // given
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
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 10.0);

            // then
            assertThrows(IllegalStateException.class, () ->
                    service.applyFinalCalibration(estimation, 100.0, 200.0));
        }
    }

    // -------------------------------------------------------------------------
    // C. Module effort calculation
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
    // D. Total effort calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Total effort calculation")
    class TotalEffort {

        @Test
        @DisplayName("calculates total effort as sum of individual module efforts (map overload)")
        void calculatesTotalEffortAsSumViaMap() {
            // given
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
            // given
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
    // E. State: isFinished
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
    // F. Convergence via registerIteration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Convergence detection via registerIteration")
    class ConvergenceDetection {

        @Test
        @DisplayName("convergent iteration (deviation=0%) sets finalIteration=true and computes regression")
        void convergentIterationSetsFinalIterationAndRegression() {
            // given
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

            // then
            DelphiIteration iteration = estimation.getIterations().get(0);
            assertTrue(iteration.getFinalIteration());
            assertTrue(iteration.getAcceptedByDeviation());

            assertTrue(estimation.isFinished());
            assertEquals(10.0, estimation.getRegressionSlope(), 0.001);
            assertEquals(0.0, estimation.getRegressionIntercept(), 0.001);
        }

        @Test
        @DisplayName("non-convergent iteration with remaining iterations sets finalIteration=false")
        void nonConvergentIterationDoesNotFinalizeWhenIterationsRemain() {
            // given
            DelphiEstimation estimation = unfinishedEstimation(10.0, 30.0);
            when(delphiEstimationRepository.findById(1L)).thenReturn(Optional.of(estimation));
            when(delphiEstimationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<DelphiExpertEstimate> estimates = List.of(
                    expertEstimate("expert1", 50.0, 100.0),
                    expertEstimate("expert2", 300.0, 600.0),
                    expertEstimate("expert3", 100.0, 200.0)
            );

            // when
            service.registerIteration(1L, estimates);

            // then
            DelphiIteration iteration = estimation.getIterations().get(0);
            assertFalse(iteration.getFinalIteration());
            assertFalse(iteration.getAcceptedByDeviation());
            assertFalse(estimation.isFinished());
        }
    }

    // -------------------------------------------------------------------------
    // G. Expert count validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Expert count validation")
    class ExpertCountValidation {

        @Test
        @DisplayName("registerIteration rejects fewer expert estimates than the configured count")
        void rejectsFewerExpertsThanConfigured() {
            // given
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
    // H. Boundary cases: canStartCalibration
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