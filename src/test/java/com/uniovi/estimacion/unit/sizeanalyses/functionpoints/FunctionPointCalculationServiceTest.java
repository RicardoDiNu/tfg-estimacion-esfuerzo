package com.uniovi.estimacion.unit.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Function Point calculation service unit tests")
class FunctionPointCalculationServiceTest {

    private FunctionPointCalculationService service;

    @BeforeEach
    void setUp() {
        service = new FunctionPointCalculationService();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FunctionPointAnalysis emptyAnalysis() {
        return new FunctionPointAnalysis();
    }

    private DataFunction dataFunction(DataFunctionType type, FunctionPointComplexity complexity) {
        DataFunction f = new DataFunction();
        f.setType(type);
        f.setComplexity(complexity);
        f.setName("df-" + type + "-" + complexity);
        f.setWeight(0);
        return f;
    }

    private TransactionalFunction transactionalFunction(TransactionalFunctionType type,
                                                        FunctionPointComplexity complexity) {
        TransactionalFunction f = new TransactionalFunction();
        f.setType(type);
        f.setComplexity(complexity);
        f.setName("tf-" + type + "-" + complexity);
        f.setWeight(0);
        return f;
    }

    private GeneralSystemCharacteristicAssessment gsc(int degree) {
        GeneralSystemCharacteristicAssessment g = new GeneralSystemCharacteristicAssessment();
        g.setDegreeOfInfluence(degree);
        return g;
    }

    private FunctionPointWeightMatrixEntry matrixEntry(FunctionPointFunctionType type,
                                                        FunctionPointComplexity complexity,
                                                        int weight) {
        FunctionPointWeightMatrixEntry e = new FunctionPointWeightMatrixEntry();
        e.setFunctionType(type);
        e.setComplexity(complexity);
        e.setWeight(weight);
        e.setDisplayOrder(0);
        return e;
    }

    private void addDataFunction(FunctionPointAnalysis analysis,
                                 DataFunctionType type,
                                 FunctionPointComplexity complexity) {
        DataFunction df = dataFunction(type, complexity);
        df.setFunctionPointAnalysis(analysis);
        analysis.getDataFunctions().add(df);
    }

    private void addTransactionalFunction(FunctionPointAnalysis analysis,
                                          TransactionalFunctionType type,
                                          FunctionPointComplexity complexity) {
        TransactionalFunction tf = transactionalFunction(type, complexity);
        tf.setFunctionPointAnalysis(analysis);
        analysis.getTransactionalFunctions().add(tf);
    }

    private void addMatrixEntry(FunctionPointAnalysis analysis,
                                FunctionPointFunctionType type,
                                FunctionPointComplexity complexity,
                                int weight) {
        FunctionPointWeightMatrixEntry entry = matrixEntry(type, complexity, weight);
        entry.setFunctionPointAnalysis(analysis);
        analysis.getWeightMatrixEntries().add(entry);
    }

    // -------------------------------------------------------------------------
    // A. Default weight calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Default weights")
    class DefaultWeights {

        @Test
        @DisplayName("ILF AVERAGE default weight is 10")
        void ilfAverageDefaultWeight() {
            // when
            int weight = service.calculateDataFunctionWeight(DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);

            // then
            assertEquals(10, weight);
        }

        @Test
        @DisplayName("EIF LOW default weight is 5")
        void eifLowDefaultWeight() {
            // when
            int weight = service.calculateDataFunctionWeight(DataFunctionType.EIF, FunctionPointComplexity.LOW);

            // then
            assertEquals(5, weight);
        }

        @Test
        @DisplayName("EI LOW default weight is 3")
        void eiLowDefaultWeight() {
            // when
            int weight = service.calculateTransactionalFunctionWeight(TransactionalFunctionType.EI, FunctionPointComplexity.LOW);

            // then
            assertEquals(3, weight);
        }

        @Test
        @DisplayName("EO HIGH default weight is 7")
        void eoHighDefaultWeight() {
            // when
            int weight = service.calculateTransactionalFunctionWeight(TransactionalFunctionType.EO, FunctionPointComplexity.HIGH);

            // then
            assertEquals(7, weight);
        }

        @Test
        @DisplayName("EQ AVERAGE default weight is 4")
        void eqAverageDefaultWeight() {
            // when
            int weight = service.calculateTransactionalFunctionWeight(TransactionalFunctionType.EQ, FunctionPointComplexity.AVERAGE);

            // then
            assertEquals(4, weight);
        }

        @Test
        @DisplayName("ILF HIGH default weight is 15")
        void ilfHighDefaultWeight() {
            // when
            int weight = service.calculateDataFunctionWeight(DataFunctionType.ILF, FunctionPointComplexity.HIGH);

            // then
            assertEquals(15, weight);
        }
    }

    // -------------------------------------------------------------------------
    // A. Custom weight matrix
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Custom weight matrix")
    class CustomWeightMatrix {

        @Test
        @DisplayName("uses custom matrix weight for EI LOW when overridden to 99")
        void usesCustomWeightForEiLow() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            addMatrixEntry(analysis, FunctionPointFunctionType.EI, FunctionPointComplexity.LOW, 99);

            // when
            int weight = service.calculateTransactionalFunctionWeight(
                    analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW);

            // then
            assertEquals(99, weight);
        }

        @Test
        @DisplayName("falls back to default when matrix entry is absent for that combination")
        void fallsBackToDefaultWeightWhenMatrixEntryMissing() {
            // given: only EI LOW in matrix; EI AVERAGE is not -> should use default = 4
            FunctionPointAnalysis analysis = emptyAnalysis();
            addMatrixEntry(analysis, FunctionPointFunctionType.EI, FunctionPointComplexity.LOW, 99);

            // when
            int weight = service.calculateTransactionalFunctionWeight(
                    analysis, TransactionalFunctionType.EI, FunctionPointComplexity.AVERAGE);

            // then
            assertEquals(4, weight);
        }

        @Test
        @DisplayName("recalculateAnalysis applies custom weights to all matching functions")
        void recalculatesAllFunctionsWithCustomMatrix() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            addMatrixEntry(analysis, FunctionPointFunctionType.ILF, FunctionPointComplexity.AVERAGE, 50);
            addMatrixEntry(analysis, FunctionPointFunctionType.EI, FunctionPointComplexity.LOW, 99);

            DataFunction df = dataFunction(DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);
            df.setFunctionPointAnalysis(analysis);
            analysis.getDataFunctions().add(df);

            TransactionalFunction tf = transactionalFunction(TransactionalFunctionType.EI, FunctionPointComplexity.LOW);
            tf.setFunctionPointAnalysis(analysis);
            analysis.getTransactionalFunctions().add(tf);

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(50, df.getWeight());
            assertEquals(99, tf.getWeight());
        }
    }

    // -------------------------------------------------------------------------
    // B. UFP calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("UFP calculation")
    class UfpCalculation {

        @Test
        @DisplayName("calculates UFP = ILF_AVERAGE(10) + EI_LOW(3) = 13")
        void calculatesUfpWithOneDataAndOneTransactional() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            addDataFunction(analysis, DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);       // 10
            addTransactionalFunction(analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW); // 3

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(13, analysis.getUnadjustedFunctionPoints());
        }

        @Test
        @DisplayName("calculates UFP = EIF_HIGH(10) + EO_AVERAGE(5) + EQ_HIGH(6) = 21")
        void calculatesUfpWithMultipleFunctions() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            addDataFunction(analysis, DataFunctionType.EIF, FunctionPointComplexity.HIGH);           // 10
            addTransactionalFunction(analysis, TransactionalFunctionType.EO, FunctionPointComplexity.AVERAGE); // 5
            addTransactionalFunction(analysis, TransactionalFunctionType.EQ, FunctionPointComplexity.HIGH);    // 6

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(21, analysis.getUnadjustedFunctionPoints());
        }

        @Test
        @DisplayName("calculates UFP = 0 when there are no functions")
        void calculatesUfpZeroWithNoFunctions() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0, analysis.getUnadjustedFunctionPoints());
        }

        @Test
        @DisplayName("recalculates UFP after custom matrix entry changes a weight")
        void recalculatesUfpAfterCustomMatrixIsApplied() {
            // given: EI LOW with custom weight 20 (default is 3)
            FunctionPointAnalysis analysis = emptyAnalysis();
            addMatrixEntry(analysis, FunctionPointFunctionType.EI, FunctionPointComplexity.LOW, 20);
            addTransactionalFunction(analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW);

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(20, analysis.getUnadjustedFunctionPoints());
        }
    }

    // -------------------------------------------------------------------------
    // C. GSC / TDI calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("TDI and GSC calculation")
    class TdiCalculation {

        @Test
        @DisplayName("calculates TDI as sum of all GSC degrees of influence")
        void calculatesTdiAsSumOfGscDegrees() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(3));
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(5));
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(2));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(10, analysis.getTotalDegreeOfInfluence());
        }

        @Test
        @DisplayName("calculates TDI = 0 when all GSC degrees are 0")
        void calculatesTdiZeroWhenAllGscAreZero() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(0));
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(0));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0, analysis.getTotalDegreeOfInfluence());
        }

        @Test
        @DisplayName("calculates TDI = 0 when no GSC assessments exist")
        void calculatesTdiZeroWithNoGscAssessments() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0, analysis.getTotalDegreeOfInfluence());
        }
    }

    // -------------------------------------------------------------------------
    // D. VAF calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("VAF calculation")
    class VafCalculation {

        @Test
        @DisplayName("VAF = 0.65 when TDI = 0")
        void vafIs065WhenTdiIsZero() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0.65, analysis.getValueAdjustmentFactor(), 0.001);
        }

        @Test
        @DisplayName("VAF = 1.00 when TDI = 35")
        void vafIs100WhenTdiIs35() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(35));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(1.00, analysis.getValueAdjustmentFactor(), 0.001);
        }

        @Test
        @DisplayName("VAF = 1.35 when TDI = 70")
        void vafIs135WhenTdiIs70() {
            // given
            FunctionPointAnalysis analysis = emptyAnalysis();
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(70));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(1.35, analysis.getValueAdjustmentFactor(), 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // E. AFP calculation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("AFP calculation")
    class AfpCalculation {

        @Test
        @DisplayName("AFP = 100.0 when UFP=100 and TDI=35 (VAF=1.00)")
        void afpWithUfp100AndTdi35() {
            // given: 10 x ILF AVERAGE (weight=10) -> UFP=100; TDI=35 -> VAF=1.00
            FunctionPointAnalysis analysis = emptyAnalysis();
            for (int i = 0; i < 10; i++) {
                addDataFunction(analysis, DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);
            }
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(35));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(100, analysis.getUnadjustedFunctionPoints());
            assertEquals(1.00, analysis.getValueAdjustmentFactor(), 0.001);
            assertEquals(100.0, analysis.getAdjustedFunctionPoints(), 0.001);
        }

        @Test
        @DisplayName("AFP = 65.0 when UFP=100 and TDI=0 (VAF=0.65)")
        void afpWithUfp100AndTdiZero() {
            // given: 10 x ILF AVERAGE -> UFP=100; no GSC -> TDI=0, VAF=0.65
            FunctionPointAnalysis analysis = emptyAnalysis();
            for (int i = 0; i < 10; i++) {
                addDataFunction(analysis, DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);
            }

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(65.0, analysis.getAdjustedFunctionPoints(), 0.001);
        }

        @Test
        @DisplayName("AFP = 0.0 when UFP = 0")
        void afpIsZeroWhenUfpIsZero() {
            // given: no functions, TDI=35
            FunctionPointAnalysis analysis = emptyAnalysis();
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(35));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0.0, analysis.getAdjustedFunctionPoints(), 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // F. Summary results
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Summary results")
    class SummaryResults {

        @Test
        @DisplayName("buildSummary returns coherent UFP, TDI, VAF and AFP")
        void buildSummaryReturnsCoherentValues() {
            // given: ILF_AVERAGE(10) + EI_LOW(3) -> UFP=13; TDI=20 -> VAF=0.85; AFP=11.05
            FunctionPointAnalysis analysis = emptyAnalysis();
            addDataFunction(analysis, DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);
            addTransactionalFunction(analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW);
            analysis.getGeneralSystemCharacteristicAssessments().add(gsc(20));

            // when
            FunctionPointAnalysisSummary summary = service.buildSummary(analysis);

            // then
            assertEquals(13, summary.getUnadjustedFunctionPoints());
            assertEquals(20, summary.getTotalDegreeOfInfluence());
            assertEquals(0.85, summary.getValueAdjustmentFactor(), 0.001);
            assertEquals(11.05, summary.getAdjustedFunctionPoints(), 0.001);
        }

        @Test
        @DisplayName("buildSummary breakdown totals match function count and UFP contribution")
        void buildSummaryBreakdownTotalsMatchFunctions() {
            // given: 2 x EI LOW (weight=3 each) -> UFP=6; 2 simple functions
            FunctionPointAnalysis analysis = emptyAnalysis();
            addTransactionalFunction(analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW);
            addTransactionalFunction(analysis, TransactionalFunctionType.EI, FunctionPointComplexity.LOW);

            // when
            FunctionPointAnalysisSummary summary = service.buildSummary(analysis);

            // then
            assertEquals(6, summary.getUnadjustedFunctionPoints());
            assertEquals(6, summary.getBreakdownTotalUfp());
            assertEquals(2, summary.getBreakdownTotalFunctions());
            assertEquals(2, summary.getBreakdownTotalSimple());
            assertEquals(0, summary.getBreakdownTotalAverage());
            assertEquals(0, summary.getBreakdownTotalHigh());
        }
    }
}
