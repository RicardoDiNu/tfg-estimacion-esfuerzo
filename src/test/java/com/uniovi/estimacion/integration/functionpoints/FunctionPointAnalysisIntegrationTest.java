package com.uniovi.estimacion.integration.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.*;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.*;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixForm;
import com.uniovi.estimacion.web.forms.sizeanalyses.functionpoints.FunctionPointWeightMatrixRowForm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Function Point analysis — integration tests")
class FunctionPointAnalysisIntegrationTest extends AbstractIntegrationTest {

    @Autowired private FunctionPointAnalysisService functionPointAnalysisService;
    @Autowired private FunctionPointModuleService functionPointModuleService;
    @Autowired private UserRequirementService userRequirementService;
    @Autowired private FunctionPointCalculationService functionPointCalculationService;
    @Autowired private FunctionPointAnalysisRepository functionPointAnalysisRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EstimationProjectRepository estimationProjectRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User pm;
    private EstimationProject project;

    @BeforeEach
    void setUp() {
        pm = createProjectManager("pm_fp_test");
        project = createProject("FP Test Project", pm);
    }

    // =========================================================
    // Test 1: Crear análisis PF
    // =========================================================

    @Nested
    @DisplayName("Crear análisis PF")
    class CreateAnalysis {

        @Test
        @DisplayName("el análisis queda asociado al proyecto y tiene 14 GSC y matriz de pesos completa")
        void createInitialAnalysisPopulatesGscAndWeightMatrix() {
            // given / when
            functionPointAnalysisService.createInitialAnalysis(project, "Límite del sistema");

            // then
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            assertThat(analysis.getEstimationProject().getId()).isEqualTo(project.getId());
            assertThat(analysis.getGeneralSystemCharacteristicAssessments())
                    .hasSize(GeneralSystemCharacteristicType.values().length);
            int expectedMatrixSize =
                    FunctionPointFunctionType.values().length * FunctionPointComplexity.values().length;
            assertThat(analysis.getWeightMatrixEntries()).hasSize(expectedMatrixSize);
        }

        @Test
        @DisplayName("análisis inicial tiene UFP=0, TDI=0, VAF=0.65 y AFP=0.0")
        void initialAnalysisHasZeroResults() {
            // when
            functionPointAnalysisService.createInitialAnalysis(project, "Descripción");

            // then
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            assertThat(analysis.getUnadjustedFunctionPoints()).isZero();
            assertThat(analysis.getTotalDegreeOfInfluence()).isZero();
            assertThat(analysis.getValueAdjustmentFactor()).isEqualTo(0.65);
            assertThat(analysis.getAdjustedFunctionPoints()).isZero();
        }
    }

    // =========================================================
    // Test 2: Añadir módulo PF
    // =========================================================

    @Nested
    @DisplayName("Añadir módulo PF")
    class AddModule {

        @Test
        @DisplayName("módulo persiste nombre y descripción y queda asociado al análisis")
        void moduleIsCreatedAndAssociated() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");

            // when
            FunctionPointModule saved = createModule("Módulo Pagos", "Gestión de pagos");

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Módulo Pagos");
            assertThat(saved.getDescription()).isEqualTo("Gestión de pagos");

            List<FunctionPointModule> modules = functionPointModuleService.findAllByProjectId(project.getId());
            assertThat(modules).hasSize(1);
        }
    }

    // =========================================================
    // Test 3: Añadir requisito de usuario
    // =========================================================

    @Nested
    @DisplayName("Añadir requisito de usuario")
    class AddUserRequirement {

        @Test
        @DisplayName("requisito persiste identifier y statement, queda asociado al módulo")
        void requirementPersistsFields() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo Usuarios");

            // when
            UserRequirement saved = createRequirement(module, "RU-001", "El sistema debe gestionar usuarios");

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getIdentifier()).isEqualTo("RU-001");
            assertThat(saved.getStatement()).isEqualTo("El sistema debe gestionar usuarios");
            assertThat(userRequirementService.findAllByModuleId(module.getId())).hasSize(1);
        }
    }

    // =========================================================
    // Test 4: Añadir función de datos
    // =========================================================

    @Nested
    @DisplayName("Añadir función de datos")
    class AddDataFunction {

        @Test
        @DisplayName("ILF AVERAGE se crea con peso por defecto 10")
        void ilfAverageHasDefaultWeight10() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Gestión de datos");

            DataFunction df = buildDataFunction("Tabla Clientes", DataFunctionType.ILF, FunctionPointComplexity.AVERAGE);

            // when
            boolean added = functionPointAnalysisService.addDataFunctionToRequirement(
                    project.getId(), req.getId(), df);

            // then
            assertThat(added).isTrue();
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(10);
        }

        @Test
        @DisplayName("EIF LOW se crea con peso por defecto 5")
        void eifLowHasDefaultWeight5() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Datos externos");

            DataFunction df = buildDataFunction("Tabla Proveedores", DataFunctionType.EIF, FunctionPointComplexity.LOW);

            // when
            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(), df);

            // then
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(5);
        }
    }

    // =========================================================
    // Test 5: Añadir función transaccional
    // =========================================================

    @Nested
    @DisplayName("Añadir función transaccional")
    class AddTransactionalFunction {

        @Test
        @DisplayName("EO HIGH se crea con peso por defecto 7")
        void eoHighHasDefaultWeight7() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Procesamiento");

            TransactionalFunction tf = buildTransactionalFunction(
                    "Generar Informe", TransactionalFunctionType.EO, FunctionPointComplexity.HIGH);

            // when
            boolean added = functionPointAnalysisService.addTransactionalFunctionToRequirement(
                    project.getId(), req.getId(), tf);

            // then
            assertThat(added).isTrue();
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(7);
        }

        @Test
        @DisplayName("EI AVERAGE se crea con peso por defecto 4")
        void eiAverageHasDefaultWeight4() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Entrada de datos");

            TransactionalFunction tf = buildTransactionalFunction(
                    "Alta de Usuario", TransactionalFunctionType.EI, FunctionPointComplexity.AVERAGE);

            // when
            functionPointAnalysisService.addTransactionalFunctionToRequirement(project.getId(), req.getId(), tf);

            // then
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(4);
        }
    }

    // =========================================================
    // Test 6: Recalcular resultados globales
    // =========================================================

    @Nested
    @DisplayName("Recalcular resultados globales PF")
    class RecalculateGlobalResults {

        @Test
        @DisplayName("ILF AVERAGE (10) + EO HIGH (7) → UFP=17, TDI=0, VAF=0.65, AFP=11.05")
        void recalculateWithKnownFunctionsProducesExpectedResults() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Req principal");

            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(),
                    buildDataFunction("ILF Clientes", DataFunctionType.ILF, FunctionPointComplexity.AVERAGE));

            functionPointAnalysisService.addTransactionalFunctionToRequirement(project.getId(), req.getId(),
                    buildTransactionalFunction("EO Informe", TransactionalFunctionType.EO, FunctionPointComplexity.HIGH));

            // when
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // then
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(17);
            assertThat(analysis.getTotalDegreeOfInfluence()).isZero();
            assertThat(analysis.getValueAdjustmentFactor()).isEqualTo(0.65);
            assertThat(analysis.getAdjustedFunctionPoints()).isEqualTo(11.05);
        }
    }

    // =========================================================
    // Test 7: Editar GSC
    // =========================================================

    @Nested
    @DisplayName("Editar GSC")
    class EditGsc {

        @Test
        @DisplayName("modificar grados de GSC recalcula TDI, VAF y AFP")
        void editGscRecalculatesFactors() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Req");
            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(),
                    buildDataFunction("ILF Test", DataFunctionType.ILF, FunctionPointComplexity.AVERAGE));

            // Set all 14 GSC to degree 5
            FunctionPointAnalysis formAnalysis = buildGscFormAnalysis(5);

            // when
            boolean updated = functionPointAnalysisService.updateGeneralSystemCharacteristics(
                    project.getId(), formAnalysis);

            // then
            assertThat(updated).isTrue();

            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            int expectedTdi = GeneralSystemCharacteristicType.values().length * 5; // 14 * 5 = 70
            assertThat(analysis.getTotalDegreeOfInfluence()).isEqualTo(expectedTdi);

            double expectedVaf = 0.65 + 0.01 * expectedTdi; // 0.65 + 0.70 = 1.35
            assertThat(analysis.getValueAdjustmentFactor()).isEqualTo(expectedVaf);

            double expectedAfp = 10.0 * expectedVaf; // UFP=10 (ILF AVERAGE), AFP = 10 * 1.35 = 13.5
            assertThat(analysis.getAdjustedFunctionPoints()).isCloseTo(expectedAfp, within(0.001));
        }
    }

    // =========================================================
    // Test 8: Editar matriz de pesos
    // =========================================================

    @Nested
    @DisplayName("Editar matriz de pesos")
    class EditWeightMatrix {

        @Test
        @DisplayName("cambiar EI LOW a 99 hace que función EI LOW tenga peso 99 y UFP cambie")
        void customWeightAppliedToFunction() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Req");

            // Update weight matrix: EI LOW = 99
            FunctionPointWeightMatrixForm form = buildWeightMatrixFormWithOverride(
                    FunctionPointFunctionType.EI, 99, 4, 6);
            functionPointAnalysisService.updateWeightMatrix(project.getId(), form);

            // Add EI LOW function
            functionPointAnalysisService.addTransactionalFunctionToRequirement(project.getId(), req.getId(),
                    buildTransactionalFunction("Alta Proveedor", TransactionalFunctionType.EI, FunctionPointComplexity.LOW));

            // when
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            // then
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(99);
        }
    }

    // =========================================================
    // Test 9: Resultados por módulo
    // =========================================================

    @Nested
    @DisplayName("Resultados por módulo PF")
    class ModuleResults {

        @Test
        @DisplayName("suma de UFP por módulos coincide con UFP global")
        void moduleSumsMatchGlobal() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule moduleA = createModule("Módulo A");
            FunctionPointModule moduleB = createModule("Módulo B");
            UserRequirement reqA = createRequirement(moduleA, "RU-001", "Req A");
            UserRequirement reqB = createRequirement(moduleB, "RU-002", "Req B");

            // Module A: ILF LOW = 7
            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), reqA.getId(),
                    buildDataFunction("ILF A", DataFunctionType.ILF, FunctionPointComplexity.LOW));

            // Module B: EO AVERAGE = 5
            functionPointAnalysisService.addTransactionalFunctionToRequirement(project.getId(), reqB.getId(),
                    buildTransactionalFunction("EO B", TransactionalFunctionType.EO, FunctionPointComplexity.AVERAGE));

            // when
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();

            List<DataFunction> dfsA = functionPointAnalysisService.findAllDataFunctionsByModuleId(moduleA.getId());
            List<TransactionalFunction> tfsA = functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(moduleA.getId());
            FunctionPointAnalysisSummary summaryA = functionPointCalculationService.buildModuleSummary(analysis, dfsA, tfsA);

            List<DataFunction> dfsB = functionPointAnalysisService.findAllDataFunctionsByModuleId(moduleB.getId());
            List<TransactionalFunction> tfsB = functionPointAnalysisService.findAllTransactionalFunctionsByModuleId(moduleB.getId());
            FunctionPointAnalysisSummary summaryB = functionPointCalculationService.buildModuleSummary(analysis, dfsB, tfsB);

            // then
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(12); // 7 + 5
            assertThat(summaryA.getUnadjustedFunctionPoints()).isEqualTo(7);
            assertThat(summaryB.getUnadjustedFunctionPoints()).isEqualTo(5);
            assertThat(summaryA.getUnadjustedFunctionPoints() + summaryB.getUnadjustedFunctionPoints())
                    .isEqualTo(analysis.getUnadjustedFunctionPoints());
        }
    }

    // =========================================================
    // Test 10: Eliminación y recálculo
    // =========================================================

    @Nested
    @DisplayName("Eliminación y recálculo PF")
    class DeleteAndRecalculate {

        @Test
        @DisplayName("eliminar función de datos recalcula UFP a 0")
        void deletingDataFunctionRecalculatesUfp() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo");
            UserRequirement req = createRequirement(module, "RU-001", "Req");
            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(),
                    buildDataFunction("ILF Test", DataFunctionType.ILF, FunctionPointComplexity.AVERAGE));

            // Retrieve the data function ID (flush triggered by query)
            Page<DataFunction> page = functionPointAnalysisService.findDataFunctionsPageByProjectId(
                    project.getId(), PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            Long dataFunctionId = page.getContent().get(0).getId();

            // when
            boolean deleted = functionPointAnalysisService.deleteDataFunction(project.getId(), dataFunctionId);

            // then
            assertThat(deleted).isTrue();
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isZero();
        }

        @Test
        @DisplayName("eliminar módulo con funciones recalcula UFP global a 0")
        void deletingModuleRecalculatesUfp() {
            // given
            functionPointAnalysisService.createInitialAnalysis(project, "Límite");
            FunctionPointModule module = createModule("Módulo a eliminar");
            UserRequirement req = createRequirement(module, "RU-001", "Req");
            functionPointAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(),
                    buildDataFunction("ILF Temp", DataFunctionType.ILF, FunctionPointComplexity.LOW));

            // Verify UFP is 7 before deletion
            assertThat(functionPointAnalysisRepository.findByEstimationProjectId(project.getId())
                    .orElseThrow().getUnadjustedFunctionPoints()).isEqualTo(7);

            // when
            boolean deleted = functionPointModuleService.deleteByIdWithContents(project.getId(), module.getId());

            // then
            assertThat(deleted).isTrue();
            FunctionPointAnalysis analysis = functionPointAnalysisRepository
                    .findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isZero();
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

    private FunctionPointModule createModule(String name) {
        return createModule(name, null);
    }

    private FunctionPointModule createModule(String name, String description) {
        FunctionPointModule module = new FunctionPointModule();
        module.setName(name);
        module.setDescription(description);
        return functionPointModuleService.createForProject(project, module);
    }

    private UserRequirement createRequirement(FunctionPointModule module, String identifier, String statement) {
        UserRequirement req = new UserRequirement();
        req.setIdentifier(identifier);
        req.setStatement(statement);
        return userRequirementService.createForModule(module, req);
    }

    private DataFunction buildDataFunction(String name, DataFunctionType type, FunctionPointComplexity complexity) {
        DataFunction df = new DataFunction();
        df.setName(name);
        df.setType(type);
        df.setComplexity(complexity);
        df.setWeight(0);
        return df;
    }

    private TransactionalFunction buildTransactionalFunction(String name, TransactionalFunctionType type,
                                                              FunctionPointComplexity complexity) {
        TransactionalFunction tf = new TransactionalFunction();
        tf.setName(name);
        tf.setType(type);
        tf.setComplexity(complexity);
        tf.setWeight(0);
        return tf;
    }

    private FunctionPointAnalysis buildGscFormAnalysis(int degree) {
        FunctionPointAnalysis formAnalysis = new FunctionPointAnalysis();
        List<GeneralSystemCharacteristicAssessment> assessments = new ArrayList<>();
        for (GeneralSystemCharacteristicType type : GeneralSystemCharacteristicType.values()) {
            GeneralSystemCharacteristicAssessment gsc = new GeneralSystemCharacteristicAssessment();
            gsc.setCharacteristicType(type);
            gsc.setDegreeOfInfluence(degree);
            assessments.add(gsc);
        }
        formAnalysis.setGeneralSystemCharacteristicAssessments(assessments);
        return formAnalysis;
    }

    private FunctionPointWeightMatrixForm buildWeightMatrixFormWithOverride(
            FunctionPointFunctionType targetType, int low, int avg, int high) {
        FunctionPointWeightMatrixForm form = new FunctionPointWeightMatrixForm();
        for (FunctionPointFunctionType type : FunctionPointFunctionType.values()) {
            FunctionPointWeightMatrixRowForm row = new FunctionPointWeightMatrixRowForm();
            row.setFunctionType(type);
            if (type == targetType) {
                row.setLowWeight(low);
                row.setAverageWeight(avg);
                row.setHighWeight(high);
            } else {
                row.setLowWeight(type.getDefaultWeight(FunctionPointComplexity.LOW));
                row.setAverageWeight(type.getDefaultWeight(FunctionPointComplexity.AVERAGE));
                row.setHighWeight(type.getDefaultWeight(FunctionPointComplexity.HIGH));
            }
            form.getRows().add(row);
        }
        return form;
    }
}
