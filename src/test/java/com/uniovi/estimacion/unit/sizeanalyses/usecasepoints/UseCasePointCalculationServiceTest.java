package com.uniovi.estimacion.unit.sizeanalyses.usecasepoints;

import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisSummary;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Use Case Point calculation service unit tests")
class UseCasePointCalculationServiceTest {

    private UseCasePointCalculationService service;

    @BeforeEach
    void setUp() {
        service = new UseCasePointCalculationService();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UseCasePointAnalysis emptyAnalysis() {
        return new UseCasePointAnalysis();
    }

    private UseCaseActor actor(UseCaseActorComplexity complexity) {
        UseCaseActor a = new UseCaseActor();
        a.setComplexity(complexity);
        a.setName("actor-" + complexity);
        a.setWeight(complexity.getWeight());
        return a;
    }

    private UseCaseEntry useCase(UseCaseComplexity complexity) {
        UseCaseEntry uc = new UseCaseEntry();
        uc.setComplexity(complexity);
        uc.setName("uc-" + complexity);
        uc.setWeight(complexity.getWeight());
        return uc;
    }

    private TechnicalFactorAssessment techFactor(TechnicalFactorType type, int degree) {
        TechnicalFactorAssessment tf = new TechnicalFactorAssessment();
        tf.setFactorType(type);
        tf.setDegreeOfInfluence(degree);
        return tf;
    }

    private EnvironmentalFactorAssessment envFactor(EnvironmentalFactorType type, int degree) {
        EnvironmentalFactorAssessment ef = new EnvironmentalFactorAssessment();
        ef.setFactorType(type);
        ef.setDegreeOfInfluence(degree);
        return ef;
    }

    // -------------------------------------------------------------------------
    // A. Actor weights
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Actor weight calculation")
    class ActorWeights {

        @Test
        @DisplayName("SIMPLE actor weight is 1")
        void simpleActorWeight() {
            assertEquals(1, service.calculateActorWeight(UseCaseActorComplexity.SIMPLE));
        }

        @Test
        @DisplayName("AVERAGE actor weight is 2")
        void averageActorWeight() {
            assertEquals(2, service.calculateActorWeight(UseCaseActorComplexity.AVERAGE));
        }

        @Test
        @DisplayName("COMPLEX actor weight is 3")
        void complexActorWeight() {
            assertEquals(3, service.calculateActorWeight(UseCaseActorComplexity.COMPLEX));
        }

        @Test
        @DisplayName("null actor complexity returns weight 0")
        void nullActorComplexityReturnsZero() {
            assertEquals(0, service.calculateActorWeight(null));
        }

        @Test
        @DisplayName("UAW = 0 when no actors")
        void uawIsZeroWhenNoActors() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0, analysis.getUnadjustedActorWeight());
        }

        @Test
        @DisplayName("UAW is sum of all actor weights: SIMPLE(1)+AVERAGE(2)+COMPLEX(3)=6")
        void uawIsSumOfActorWeights() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.SIMPLE));   // 1
            analysis.getActors().add(actor(UseCaseActorComplexity.AVERAGE));  // 2
            analysis.getActors().add(actor(UseCaseActorComplexity.COMPLEX));  // 3

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(6, analysis.getUnadjustedActorWeight());
        }
    }

    // -------------------------------------------------------------------------
    // B. Use case complexity and weights
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Use case complexity and weight calculation")
    class UseCaseWeights {

        @Test
        @DisplayName("SIMPLE use case weight is 5")
        void simpleUseCaseWeight() {
            assertEquals(5, service.calculateUseCaseWeight(UseCaseComplexity.SIMPLE));
        }

        @Test
        @DisplayName("AVERAGE use case weight is 10")
        void averageUseCaseWeight() {
            assertEquals(10, service.calculateUseCaseWeight(UseCaseComplexity.AVERAGE));
        }

        @Test
        @DisplayName("COMPLEX use case weight is 15")
        void complexUseCaseWeight() {
            assertEquals(15, service.calculateUseCaseWeight(UseCaseComplexity.COMPLEX));
        }

        @Test
        @DisplayName("transactionCount 1 -> SIMPLE")
        void transactionCount1IsSimple() {
            assertEquals(UseCaseComplexity.SIMPLE, service.determineUseCaseComplexity(1));
        }

        @Test
        @DisplayName("transactionCount 3 -> SIMPLE")
        void transactionCount3IsSimple() {
            assertEquals(UseCaseComplexity.SIMPLE, service.determineUseCaseComplexity(3));
        }

        @Test
        @DisplayName("transactionCount 4 -> AVERAGE")
        void transactionCount4IsAverage() {
            assertEquals(UseCaseComplexity.AVERAGE, service.determineUseCaseComplexity(4));
        }

        @Test
        @DisplayName("transactionCount 7 -> AVERAGE")
        void transactionCount7IsAverage() {
            assertEquals(UseCaseComplexity.AVERAGE, service.determineUseCaseComplexity(7));
        }

        @Test
        @DisplayName("transactionCount 8 -> COMPLEX")
        void transactionCount8IsComplex() {
            assertEquals(UseCaseComplexity.COMPLEX, service.determineUseCaseComplexity(8));
        }

        @Test
        @DisplayName("null transactionCount -> null complexity")
        void nullTransactionCountIsNull() {
            assertNull(service.determineUseCaseComplexity(null));
        }

        @Test
        @DisplayName("transactionCount 0 -> null complexity")
        void zeroTransactionCountIsNull() {
            assertNull(service.determineUseCaseComplexity(0));
        }

        @Test
        @DisplayName("null transactionCount -> null weight from calculateUseCaseWeightFromTransactionCount")
        void nullTransactionCountWeightIsNull() {
            assertNull(service.calculateUseCaseWeightFromTransactionCount(null));
        }

        @Test
        @DisplayName("UUCW = 0 when no use cases")
        void uucwIsZeroWhenNoUseCases() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(0, analysis.getUnadjustedUseCaseWeight());
        }

        @Test
        @DisplayName("UUCW is sum of use case weights: SIMPLE(5)+AVERAGE(10)+COMPLEX(15)=30")
        void uucwIsSumOfUseCaseWeights() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getUseCases().add(useCase(UseCaseComplexity.SIMPLE));   // 5
            analysis.getUseCases().add(useCase(UseCaseComplexity.AVERAGE));  // 10
            analysis.getUseCases().add(useCase(UseCaseComplexity.COMPLEX));  // 15

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(30, analysis.getUnadjustedUseCaseWeight());
        }
    }

    // -------------------------------------------------------------------------
    // C. Technical factors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Technical factor calculation")
    class TechnicalFactors {

        @Test
        @DisplayName("TFactor = sum of (degree * weight): RESPONSE_TIME(w=1)*5 + CONCURRENT(w=1)*3 = 8.0")
        void calculatesTechnicalFactor() {
            // given
            List<TechnicalFactorAssessment> assessments = List.of(
                    techFactor(TechnicalFactorType.RESPONSE_TIME, 5),   // 1.0 * 5 = 5.0
                    techFactor(TechnicalFactorType.CONCURRENT, 3)        // 1.0 * 3 = 3.0
            );

            // when
            double result = service.calculateTechnicalFactor(assessments);

            // then
            assertEquals(8.0, result, 0.001);
        }

        @Test
        @DisplayName("TCF = 0.6 when all technical factor degrees are 0")
        void tcfIs06WhenTechnicalFactorIsZero() {
            // given
            List<TechnicalFactorAssessment> assessments = List.of(
                    techFactor(TechnicalFactorType.RESPONSE_TIME, 0),
                    techFactor(TechnicalFactorType.CONCURRENT, 0)
            );

            // when
            double tf = service.calculateTechnicalFactor(assessments);
            double tcf = service.calculateTechnicalComplexityFactor(tf);

            // then
            assertEquals(0.0, tf, 0.001);
            assertEquals(0.6, tcf, 0.001);
        }

        @Test
        @DisplayName("TCF = 0.6 + 0.01 * TFactor: TFactor=20 -> TCF=0.80")
        void tcfFormula() {
            // when
            double result = service.calculateTechnicalComplexityFactor(20.0);

            // then
            assertEquals(0.80, result, 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // D. Environmental factors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Environmental factor calculation")
    class EnvironmentalFactors {

        @Test
        @DisplayName("EFactor = sum of (degree * weight): PROCESS_FAMILIARITY(w=1.5)*4 + MOTIVATION(w=1.0)*2 = 8.0")
        void calculatesEnvironmentalFactor() {
            // given
            List<EnvironmentalFactorAssessment> assessments = List.of(
                    envFactor(EnvironmentalFactorType.PROCESS_FAMILIARITY, 4),  // 1.5 * 4 = 6.0
                    envFactor(EnvironmentalFactorType.MOTIVATION, 2)             // 1.0 * 2 = 2.0
            );

            // when
            double result = service.calculateEnvironmentalFactor(assessments);

            // then
            assertEquals(8.0, result, 0.001);
        }

        @Test
        @DisplayName("ECF = 1.4 when all environmental factor degrees are 0")
        void ecfIs14WhenEnvironmentalFactorIsZero() {
            // given
            List<EnvironmentalFactorAssessment> assessments = List.of(
                    envFactor(EnvironmentalFactorType.PROCESS_FAMILIARITY, 0)
            );

            // when
            double ef = service.calculateEnvironmentalFactor(assessments);
            double ecf = service.calculateEnvironmentalComplexityFactor(ef);

            // then
            assertEquals(0.0, ef, 0.001);
            assertEquals(1.4, ecf, 0.001);
        }

        @Test
        @DisplayName("ECF = 1.4 - 0.03 * EFactor: EFactor=10 -> ECF=1.10")
        void ecfFormula() {
            // when
            double result = service.calculateEnvironmentalComplexityFactor(10.0);

            // then
            assertEquals(1.10, result, 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // E. Global UCP result
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Global UCP result")
    class GlobalUcpResult {

        @Test
        @DisplayName("UUCP = UAW + UUCW: SIMPLE_actor(1) + AVERAGE_uc(10) = 11")
        void calculatesUucp() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.SIMPLE));   // UAW = 1
            analysis.getUseCases().add(useCase(UseCaseComplexity.AVERAGE));   // UUCW = 10

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(1, analysis.getUnadjustedActorWeight());
            assertEquals(10, analysis.getUnadjustedUseCaseWeight());
            assertEquals(11, analysis.getUnadjustedUseCasePoints());
        }

        @Test
        @DisplayName("UCP = UUCP * TCF * ECF with no factors: 5*SIMPLE_actors + 2*AVERAGE_ucs -> 21.0")
        void calculatesUcpWithControlledData() {
            // given
            // UAW = 5x1 = 5; UUCW = 2x10 = 20; UUCP = 25
            // TCF = 0.6 (no factors); ECF = 1.4 (no factors)
            // UCP = 25 * 0.6 * 1.4 = 21.0
            UseCasePointAnalysis analysis = emptyAnalysis();
            for (int i = 0; i < 5; i++) {
                analysis.getActors().add(actor(UseCaseActorComplexity.SIMPLE));
            }
            analysis.getUseCases().add(useCase(UseCaseComplexity.AVERAGE));
            analysis.getUseCases().add(useCase(UseCaseComplexity.AVERAGE));

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(5, analysis.getUnadjustedActorWeight());
            assertEquals(20, analysis.getUnadjustedUseCaseWeight());
            assertEquals(25, analysis.getUnadjustedUseCasePoints());
            assertEquals(0.6, analysis.getTechnicalComplexityFactor(), 0.001);
            assertEquals(1.4, analysis.getEnvironmentalComplexityFactor(), 0.001);
            assertEquals(21.0, analysis.getAdjustedUseCasePoints(), 0.001);
        }

        @Test
        @DisplayName("buildSummary returns coherent UAW, UUCW, TCF, ECF and UCP")
        void buildSummaryReturnsCoherentValues() {
            // given
            // UAW = 1x2 = 2 (AVERAGE); UUCW = 1x5 = 5 (SIMPLE); UUCP = 7
            // TCF = 0.6; ECF = 1.4; UCP = 7 * 0.6 * 1.4 = 5.88
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.AVERAGE));
            analysis.getUseCases().add(useCase(UseCaseComplexity.SIMPLE));

            // when
            UseCasePointAnalysisSummary summary = service.buildSummary(analysis);

            // then
            assertEquals(2, summary.getUnadjustedActorWeight());
            assertEquals(5, summary.getUnadjustedUseCaseWeight());
            assertEquals(7, summary.getUnadjustedUseCasePoints());
            assertEquals(0.6, summary.getTechnicalComplexityFactor(), 0.001);
            assertEquals(1.4, summary.getEnvironmentalComplexityFactor(), 0.001);
            assertEquals(5.88, summary.getAdjustedUseCasePoints(), 0.001);
        }

        @Test
        @DisplayName("recalculateAnalysis updates actor and use case weights on entities")
        void recalculateAnalysisUpdatesWeightsOnEntities() {
            // given: create entities with weight=0 initially; after recalculate they should be set
            UseCasePointAnalysis analysis = emptyAnalysis();
            UseCaseActor a = new UseCaseActor();
            a.setComplexity(UseCaseActorComplexity.COMPLEX);
            a.setName("test-actor");
            a.setWeight(0);
            analysis.getActors().add(a);

            UseCaseEntry uc = new UseCaseEntry();
            uc.setComplexity(UseCaseComplexity.COMPLEX);
            uc.setName("test-uc");
            uc.setWeight(0);
            analysis.getUseCases().add(uc);

            // when
            service.recalculateAnalysis(analysis);

            // then
            assertEquals(3, a.getWeight());
            assertEquals(15, uc.getWeight());
        }
    }

    // -------------------------------------------------------------------------
    // F. Module results
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Module results")
    class ModuleResults {

        @Test
        @DisplayName("module with all use cases receives full global UCP")
        void moduleWithAllUseCasesReceivesFullUcp() {
            // given
            // 2 SIMPLE actors -> UAW = 2; 2 AVERAGE use cases -> UUCW = 20
            // module has all use cases -> allocatedUAW = 2*(20/20) = 2 -> UUCP = 22 = total
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.SIMPLE));
            analysis.getActors().add(actor(UseCaseActorComplexity.SIMPLE));

            UseCaseEntry uc1 = useCase(UseCaseComplexity.AVERAGE);
            UseCaseEntry uc2 = useCase(UseCaseComplexity.AVERAGE);
            analysis.getUseCases().add(uc1);
            analysis.getUseCases().add(uc2);

            // when
            double moduleUcp = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc1, uc2));
            service.recalculateAnalysis(analysis);
            double totalUcp = analysis.getAdjustedUseCasePoints();

            // then
            assertEquals(totalUcp, moduleUcp, 0.001);
        }

        @Test
        @DisplayName("two modules with equal use case weight receive equal allocated UAW")
        void twoModulesWithEqualUseCaseWeightGetEqualUaw() {
            // given
            // UAW = 4 (2 x AVERAGE); Module1: 1 AVERAGE uc (10); Module2: 1 AVERAGE uc (10)
            // Each module gets UAW = 4 * (10/20) = 2
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.AVERAGE));
            analysis.getActors().add(actor(UseCaseActorComplexity.AVERAGE));

            UseCaseEntry uc1 = useCase(UseCaseComplexity.AVERAGE);
            UseCaseEntry uc2 = useCase(UseCaseComplexity.AVERAGE);
            analysis.getUseCases().add(uc1);
            analysis.getUseCases().add(uc2);

            // when
            double ucp1 = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc1));
            double ucp2 = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc2));

            // then
            assertEquals(ucp1, ucp2, 0.001);
        }

        @Test
        @DisplayName("module without use cases has UCP = 0.0")
        void moduleWithNoUseCasesHasZeroUcp() {
            // given
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.AVERAGE));
            analysis.getUseCases().add(useCase(UseCaseComplexity.SIMPLE));

            // when
            double result = service.calculateAdjustedUseCasePointsForModule(analysis, List.of());

            // then
            assertEquals(0.0, result, 0.001);
        }

        @Test
        @DisplayName("sum of module UCPs equals total UCP (proportional UAW allocation)")
        void sumOfModuleUcpsEqualsTotalUcp() {
            // given
            // 1 COMPLEX actor -> UAW = 3
            // SIMPLE(5) + AVERAGE(10) + COMPLEX(15) -> UUCW = 30; UUCP = 33
            // TCF = 0.6; ECF = 1.4; UCP = 33 * 0.6 * 1.4 = 27.72
            UseCasePointAnalysis analysis = emptyAnalysis();
            analysis.getActors().add(actor(UseCaseActorComplexity.COMPLEX));

            UseCaseEntry uc1 = useCase(UseCaseComplexity.SIMPLE);
            UseCaseEntry uc2 = useCase(UseCaseComplexity.AVERAGE);
            UseCaseEntry uc3 = useCase(UseCaseComplexity.COMPLEX);
            analysis.getUseCases().add(uc1);
            analysis.getUseCases().add(uc2);
            analysis.getUseCases().add(uc3);

            // when
            service.recalculateAnalysis(analysis);
            double totalUcp = analysis.getAdjustedUseCasePoints();

            double moduleUcp1 = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc1));
            double moduleUcp2 = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc2));
            double moduleUcp3 = service.calculateAdjustedUseCasePointsForModule(analysis, List.of(uc3));

            // then
            assertEquals(totalUcp, moduleUcp1 + moduleUcp2 + moduleUcp3, 0.01);
        }
    }
}
