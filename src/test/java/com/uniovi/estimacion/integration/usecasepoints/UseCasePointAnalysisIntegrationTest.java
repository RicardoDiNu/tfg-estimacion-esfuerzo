package com.uniovi.estimacion.integration.usecasepoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointCalculationService;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseEnvironmentalFactorsForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseFactorAssessmentForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.usecasepoints.UseCaseTechnicalFactorsForm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Use Case Point analysis — integration tests")
class UseCasePointAnalysisIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UseCasePointAnalysisService ucpAnalysisService;
    @Autowired private UseCasePointCalculationService ucpCalculationService;
    @Autowired private UseCasePointAnalysisRepository ucpAnalysisRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EstimationProjectRepository estimationProjectRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User pm;
    private EstimationProject project;

    @BeforeEach
    void setUp() {
        pm = createProjectManager("pm_ucp_test");
        project = createProject("UCP Test Project", pm);
    }

    // =========================================================
    // Test 1: Crear análisis UCP
    // =========================================================

    @Nested
    @DisplayName("Crear análisis UCP")
    class CreateAnalysis {

        @Test
        @DisplayName("el análisis queda asociado al proyecto con factores técnicos y ambientales inicializados")
        void createInitialAnalysisPopulatesFactors() {
            // when
            ucpAnalysisService.createInitialAnalysis(project, "Límite del sistema");

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            assertThat(analysis.getEstimationProject().getId()).isEqualTo(project.getId());
            assertThat(analysis.getTechnicalFactorAssessments())
                    .hasSize(TechnicalFactorType.values().length);
            assertThat(analysis.getEnvironmentalFactorAssessments())
                    .hasSize(EnvironmentalFactorType.values().length);
        }

        @Test
        @DisplayName("análisis inicial tiene UAW=0, UUCW=0, TCF=0.6, ECF=1.4, UCP=0")
        void initialAnalysisHasZeroResults() {
            // when
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            assertThat(analysis.getUnadjustedActorWeight()).isZero();
            assertThat(analysis.getUnadjustedUseCaseWeight()).isZero();
            assertThat(analysis.getTechnicalComplexityFactor()).isEqualTo(0.6);
            assertThat(analysis.getEnvironmentalComplexityFactor()).isEqualTo(1.4);
            assertThat(analysis.getAdjustedUseCasePoints()).isZero();
        }
    }

    // =========================================================
    // Test 2: Crear actor
    // =========================================================

    @Nested
    @DisplayName("Crear actor UCP")
    class CreateActor {

        @Test
        @DisplayName("actor SIMPLE persiste con peso 1")
        void simpleActorHasWeight1() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // when
            boolean added = ucpAnalysisService.addActor(project.getId(), buildActor("Actor Simple", UseCaseActorComplexity.SIMPLE));

            // then
            assertThat(added).isTrue();
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedActorWeight()).isEqualTo(1);
        }

        @Test
        @DisplayName("actor AVERAGE persiste con peso 2")
        void averageActorHasWeight2() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // when
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor Medio", UseCaseActorComplexity.AVERAGE));

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedActorWeight()).isEqualTo(2);
        }

        @Test
        @DisplayName("actor COMPLEX persiste con peso 3")
        void complexActorHasWeight3() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // when
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor Complejo", UseCaseActorComplexity.COMPLEX));

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedActorWeight()).isEqualTo(3);
        }
    }

    // =========================================================
    // Test 3: Crear módulo UCP
    // =========================================================

    @Nested
    @DisplayName("Crear módulo UCP")
    class CreateModule {

        @Test
        @DisplayName("módulo persiste nombre y descripción, queda asociado al análisis")
        void moduleIsCreatedAndAssociated() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // when
            UseCasePointModule module = addModule("Módulo Autenticación", "Gestión de acceso");

            // then
            assertThat(module.getId()).isNotNull();
            assertThat(module.getName()).isEqualTo("Módulo Autenticación");
            assertThat(module.getDescription()).isEqualTo("Gestión de acceso");
            assertThat(ucpAnalysisService.findAllModulesByProjectId(project.getId())).hasSize(1);
        }
    }

    // =========================================================
    // Test 4: Crear caso de uso
    // =========================================================

    @Nested
    @DisplayName("Crear caso de uso UCP")
    class CreateUseCase {

        @Test
        @DisplayName("caso de uso persiste campos ricos y queda asociado al módulo con complejidad y peso calculados")
        void useCasePersistsRichFields() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.SIMPLE));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            Long actorId = actors.get(0).getId();

            UseCasePointModule module = addModule("Módulo UC");
            Long moduleId = module.getId();

            UseCaseEntry useCase = buildUseCase("Iniciar Sesión",
                    "Descripción login",
                    "Usuario accede al sistema",
                    "Usuario no autenticado",
                    "Usuario autenticado",
                    "1. Introducir credenciales\n2. Validar\n3. Redirigir",
                    null, null,
                    3); // SIMPLE, weight=5

            // when
            boolean added = ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId, useCase, List.of(actorId));

            // then
            assertThat(added).isTrue();

            List<UseCaseEntry> useCases = ucpAnalysisService.findAllUseCasesByModuleId(moduleId);
            assertThat(useCases).hasSize(1);

            UseCaseEntry saved = useCases.get(0);
            assertThat(saved.getName()).isEqualTo("Iniciar Sesión");
            assertThat(saved.getDescription()).isEqualTo("Descripción login");
            assertThat(saved.getTriggerCondition()).isEqualTo("Usuario accede al sistema");
            assertThat(saved.getPreconditions()).isEqualTo("Usuario no autenticado");
            assertThat(saved.getPostconditions()).isEqualTo("Usuario autenticado");
            assertThat(saved.getTransactionCount()).isEqualTo(3);
            assertThat(saved.getComplexity()).isEqualTo(UseCaseComplexity.SIMPLE);
            assertThat(saved.getWeight()).isEqualTo(5);
            assertThat(saved.getActors()).hasSize(1);
        }

        @Test
        @DisplayName("transactionCount=2 → SIMPLE, transactionCount=5 → AVERAGE, transactionCount=8 → COMPLEX")
        void complexityDerivedFromTransactionCount() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.SIMPLE));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            Long actorId = actors.get(0).getId();
            UseCasePointModule module = addModule("Módulo");
            Long moduleId = module.getId();

            // when
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId,
                    buildUseCase("UC Simple", null, null, null, null, null, null, null, 2), List.of(actorId));
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId,
                    buildUseCase("UC Average", null, null, null, null, null, null, null, 5), List.of(actorId));
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId,
                    buildUseCase("UC Complex", null, null, null, null, null, null, null, 8), List.of(actorId));

            // then
            List<UseCaseEntry> useCases = ucpAnalysisService.findAllUseCasesByModuleId(moduleId);
            assertThat(useCases).hasSize(3);

            assertThat(useCases.stream().filter(uc -> "UC Simple".equals(uc.getName())).findFirst().orElseThrow().getComplexity())
                    .isEqualTo(UseCaseComplexity.SIMPLE);
            assertThat(useCases.stream().filter(uc -> "UC Average".equals(uc.getName())).findFirst().orElseThrow().getComplexity())
                    .isEqualTo(UseCaseComplexity.AVERAGE);
            assertThat(useCases.stream().filter(uc -> "UC Complex".equals(uc.getName())).findFirst().orElseThrow().getComplexity())
                    .isEqualTo(UseCaseComplexity.COMPLEX);
        }
    }

    // =========================================================
    // Test 5: Recalcular resultados globales
    // =========================================================

    @Nested
    @DisplayName("Recalcular resultados globales UCP")
    class RecalculateGlobalResults {

        @Test
        @DisplayName("SIMPLE+COMPLEX actores y SIMPLE+AVERAGE CU → UCP = 19 * 0.6 * 1.4 = 15.96")
        void recalculateProducesExpectedResults() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // Actors: SIMPLE (1) + COMPLEX (3) → UAW = 4
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor Simple", UseCaseActorComplexity.SIMPLE));
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor Complejo", UseCaseActorComplexity.COMPLEX));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            Long simpleActorId = actors.stream().filter(a -> "Actor Simple".equals(a.getName()))
                    .findFirst().orElseThrow().getId();

            UseCasePointModule module = addModule("Módulo");
            Long moduleId = module.getId();

            // Use cases: SIMPLE (5) + AVERAGE (10) → UUCW = 15
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId,
                    buildUseCase("UC Simple", null, null, null, null, null, null, null, 2),
                    List.of(simpleActorId));
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleId,
                    buildUseCase("UC Average", null, null, null, null, null, null, null, 5),
                    List.of(simpleActorId));

            // when
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // then
            // UUCP = UAW + UUCW = 4 + 15 = 19
            // All factors at 0 → TCF = 0.6 + 0.01*0 = 0.6, ECF = 1.4 - 0.03*0 = 1.4
            // UCP = 19 * 0.6 * 1.4 = 15.96
            assertThat(analysis.getUnadjustedActorWeight()).isEqualTo(4);
            assertThat(analysis.getUnadjustedUseCaseWeight()).isEqualTo(15);
            assertThat(analysis.getUnadjustedUseCasePoints()).isEqualTo(19);
            assertThat(analysis.getTechnicalComplexityFactor()).isCloseTo(0.6, within(0.001));
            assertThat(analysis.getEnvironmentalComplexityFactor()).isCloseTo(1.4, within(0.001));
            assertThat(analysis.getAdjustedUseCasePoints()).isCloseTo(15.96, within(0.01));
        }
    }

    // =========================================================
    // Test 6: Editar factores técnicos
    // =========================================================

    @Nested
    @DisplayName("Editar factores técnicos UCP")
    class EditTechnicalFactors {

        @Test
        @DisplayName("modificar factor técnico recalcula TCF y UCP")
        void editTechnicalFactorRecalculatesTcfAndUcp() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.SIMPLE));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            UseCasePointModule module = addModule("Módulo");
            ucpAnalysisService.addUseCaseToModule(project.getId(), module.getId(),
                    buildUseCase("UC", null, null, null, null, null, null, null, 2), List.of(actors.get(0).getId()));

            double ucpBefore = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow().getAdjustedUseCasePoints();

            // when - set DISTRIBUTED_SYSTEM to degree 5 (weight 2.0 → contributes 10.0 to TFactor)
            UseCaseTechnicalFactorsForm form = new UseCaseTechnicalFactorsForm();
            UseCaseFactorAssessmentForm assessment = new UseCaseFactorAssessmentForm();
            assessment.setFactorCode(TechnicalFactorType.DISTRIBUTED_SYSTEM.name());
            assessment.setDegreeOfInfluence(5);
            form.getAssessments().add(assessment);
            ucpAnalysisService.updateTechnicalFactors(project.getId(), form);

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // TFactor += 5 * 2.0 = 10; TCF = 0.6 + 0.01 * 10 = 0.70
            assertThat(analysis.getTechnicalComplexityFactor()).isCloseTo(0.70, within(0.001));
            assertThat(analysis.getAdjustedUseCasePoints()).isGreaterThan(ucpBefore);
        }
    }

    // =========================================================
    // Test 7: Editar factores ambientales
    // =========================================================

    @Nested
    @DisplayName("Editar factores ambientales UCP")
    class EditEnvironmentalFactors {

        @Test
        @DisplayName("modificar factor ambiental recalcula ECF y UCP")
        void editEnvironmentalFactorRecalculatesEcfAndUcp() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.COMPLEX));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            UseCasePointModule module = addModule("Módulo");
            ucpAnalysisService.addUseCaseToModule(project.getId(), module.getId(),
                    buildUseCase("UC", null, null, null, null, null, null, null, 5), List.of(actors.get(0).getId()));

            double ucpBefore = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow().getAdjustedUseCasePoints();

            // when - set STABLE_REQUIREMENTS to degree 5 (weight 2.0 → contributes 10.0 to EFactor)
            UseCaseEnvironmentalFactorsForm form = new UseCaseEnvironmentalFactorsForm();
            UseCaseFactorAssessmentForm assessment = new UseCaseFactorAssessmentForm();
            assessment.setFactorCode(EnvironmentalFactorType.STABLE_REQUIREMENTS.name());
            assessment.setDegreeOfInfluence(5);
            form.getAssessments().add(assessment);
            ucpAnalysisService.updateEnvironmentalFactors(project.getId(), form);

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // EFactor += 5 * 2.0 = 10; ECF = 1.4 - 0.03 * 10 = 1.10
            assertThat(analysis.getEnvironmentalComplexityFactor()).isCloseTo(1.10, within(0.001));
            assertThat(analysis.getAdjustedUseCasePoints()).isNotEqualTo(ucpBefore);
        }
    }

    // =========================================================
    // Test 8: Resultados por módulo
    // =========================================================

    @Nested
    @DisplayName("Resultados por módulo UCP")
    class ModuleResults {

        @Test
        @DisplayName("dos módulos con mismo UUCW reciben el mismo UAW asignado")
        void twoModulesWithEqualUucwGetEqualAllocatedUaw() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.COMPLEX));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            Long actorId = actors.get(0).getId();

            UseCasePointModule moduleA = addModule("Módulo A");
            UseCasePointModule moduleB = addModule("Módulo B");

            // Each module gets one SIMPLE use case (weight 5) → equal UUCW per module
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleA.getId(),
                    buildUseCase("UC A1", null, null, null, null, null, null, null, 2), List.of(actorId));
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleB.getId(),
                    buildUseCase("UC B1", null, null, null, null, null, null, null, 2), List.of(actorId));

            // when
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            List<UseCaseEntry> useCasesA = ucpAnalysisService.findAllUseCasesByModuleId(moduleA.getId());
            List<UseCaseEntry> useCasesB = ucpAnalysisService.findAllUseCasesByModuleId(moduleB.getId());

            double ucpA = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, useCasesA);
            double ucpB = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, useCasesB);

            // then — symmetric modules should get the same UCP
            assertThat(ucpA).isCloseTo(ucpB, within(0.001));
        }

        @Test
        @DisplayName("módulo sin casos de uso tiene tamaño UCP 0")
        void emptyModuleHasZeroUcp() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.SIMPLE));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            UseCasePointModule moduleWithUc = addModule("Módulo Con CU");
            UseCasePointModule emptyModule = addModule("Módulo Vacío");

            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleWithUc.getId(),
                    buildUseCase("UC1", null, null, null, null, null, null, null, 3),
                    List.of(actors.get(0).getId()));

            // when
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            List<UseCaseEntry> emptyUseCases = ucpAnalysisService.findAllUseCasesByModuleId(emptyModule.getId());

            double ucpEmpty = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, emptyUseCases);

            // then
            assertThat(ucpEmpty).isZero();
        }

        @Test
        @DisplayName("suma de UCP por módulos se aproxima al UCP global")
        void moduleSumApproximatesGlobalUcp() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.AVERAGE));
            List<UseCaseActor> actors = ucpAnalysisService.findAllActorsByProjectId(project.getId());
            Long actorId = actors.get(0).getId();

            UseCasePointModule moduleA = addModule("Módulo A");
            UseCasePointModule moduleB = addModule("Módulo B");

            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleA.getId(),
                    buildUseCase("UC A1", null, null, null, null, null, null, null, 5), List.of(actorId));
            ucpAnalysisService.addUseCaseToModule(project.getId(), moduleB.getId(),
                    buildUseCase("UC B1", null, null, null, null, null, null, null, 8), List.of(actorId));

            // when
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            double globalUcp = analysis.getAdjustedUseCasePoints();

            List<UseCaseEntry> useCasesA = ucpAnalysisService.findAllUseCasesByModuleId(moduleA.getId());
            List<UseCaseEntry> useCasesB = ucpAnalysisService.findAllUseCasesByModuleId(moduleB.getId());

            double ucpA = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, useCasesA);
            double ucpB = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, useCasesB);

            // then
            assertThat(ucpA + ucpB).isCloseTo(globalUcp, within(0.01));
        }
    }

    // =========================================================
    // Test 9: Casos límite
    // =========================================================

    @Nested
    @DisplayName("Casos límite UCP")
    class EdgeCases {

        @Test
        @DisplayName("sin actores, UAW = 0")
        void noActorsYieldsZeroUaw() {
            // when
            ucpAnalysisService.createInitialAnalysis(project, "Límite");

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedActorWeight()).isZero();
        }

        @Test
        @DisplayName("sin casos de uso, UUCW = 0")
        void noUseCasesYieldsZeroUucw() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            ucpAnalysisService.addActor(project.getId(), buildActor("Actor", UseCaseActorComplexity.COMPLEX));

            // when
            ucpAnalysisService.findAllActorsByProjectId(project.getId()); // trigger flush

            // then
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedUseCaseWeight()).isZero();
        }

        @Test
        @DisplayName("módulo sin casos no lanza excepción y devuelve 0.0")
        void moduleWithoutUseCasesDoesNotThrow() {
            // given
            ucpAnalysisService.createInitialAnalysis(project, "Límite");
            UseCasePointModule emptyModule = addModule("Módulo Vacío");
            UseCasePointAnalysis analysis = ucpAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // when / then — no exception
            List<UseCaseEntry> empty = ucpAnalysisService.findAllUseCasesByModuleId(emptyModule.getId());
            double result = ucpCalculationService.calculateAdjustedUseCasePointsForModule(analysis, empty);
            assertThat(result).isZero();
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private User createProjectManager(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(UserRole.ROLE_PROJECT_MANAGER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private EstimationProject createProject(String name, User owner) {
        EstimationProject project = new EstimationProject(name, null);
        project.setOwner(owner);
        return estimationProjectRepository.save(project);
    }

    /** Adds a module and returns it with a DB-assigned ID (triggers flush via query). */
    private UseCasePointModule addModule(String name) {
        return addModule(name, null);
    }

    private UseCasePointModule addModule(String name, String description) {
        UseCasePointModule module = new UseCasePointModule();
        module.setName(name);
        module.setDescription(description);
        ucpAnalysisService.addModule(project.getId(), module);
        // trigger flush so the module gets its DB-assigned ID
        return ucpAnalysisService.findAllModulesByProjectId(project.getId()).stream()
                .filter(m -> name.equals(m.getName()))
                .findFirst()
                .orElseThrow();
    }

    private UseCaseActor buildActor(String name, UseCaseActorComplexity complexity) {
        UseCaseActor actor = new UseCaseActor();
        actor.setName(name);
        actor.setComplexity(complexity);
        actor.setWeight(0);
        return actor;
    }

    private UseCaseEntry buildUseCase(String name, String description, String triggerCondition,
                                      String preconditions, String postconditions,
                                      String normalFlow, String alternativeFlows, String exceptionFlows,
                                      int transactionCount) {
        UseCaseEntry uc = new UseCaseEntry();
        uc.setName(name);
        uc.setDescription(description);
        uc.setTriggerCondition(triggerCondition);
        uc.setPreconditions(preconditions);
        uc.setPostconditions(postconditions);
        uc.setNormalFlow(normalFlow);
        uc.setAlternativeFlows(alternativeFlows);
        uc.setExceptionFlows(exceptionFlows);
        uc.setTransactionCount(transactionCount);
        uc.setComplexity(UseCaseComplexity.SIMPLE); // will be overridden by service
        uc.setWeight(0);
        return uc;
    }
}
