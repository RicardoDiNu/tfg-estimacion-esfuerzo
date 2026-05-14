package com.uniovi.estimacion.integration.xml.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointModuleService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointXmlExportService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointXmlImportService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.UserRequirementService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Function Point XML export service — integration tests")
class FunctionPointXmlExportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FunctionPointXmlExportService fpXmlExportService;

    @Autowired
    private FunctionPointXmlImportService fpXmlImportService;

    @Autowired
    private FunctionPointAnalysisService fpAnalysisService;

    @Autowired
    private FunctionPointModuleService fpModuleService;

    @Autowired
    private UserRequirementService userRequirementService;

    @Autowired
    private FunctionPointAnalysisRepository fpAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private EstimationProject project;

    @BeforeEach
    void setUp() {
        User pm = createUser("pm_fp_xml_export_test", "pm_fp_xml_export@test.com");
        project = createProject("Proyecto FP XML Export", pm);

        // Build a minimal FP analysis with one module, one requirement and two functions
        fpAnalysisService.createInitialAnalysis(project, "Límite del sistema");
        FunctionPointModule module = createModule("Módulo exportación");
        UserRequirement req = createRequirement(module, "RU-001", "Gestión de usuarios");

        DataFunction df = new DataFunction();
        df.setName("ILF Usuarios");
        df.setType(DataFunctionType.ILF);
        df.setComplexity(FunctionPointComplexity.HIGH);
        df.setWeight(0);
        fpAnalysisService.addDataFunctionToRequirement(project.getId(), req.getId(), df);

        TransactionalFunction tf = new TransactionalFunction();
        tf.setName("EI Alta usuario");
        tf.setType(TransactionalFunctionType.EI);
        tf.setComplexity(FunctionPointComplexity.LOW);
        tf.setWeight(0);
        fpAnalysisService.addTransactionalFunctionToRequirement(project.getId(), req.getId(), tf);
    }

    // =========================================================
    // ExportContent
    // =========================================================

    @Nested
    @DisplayName("Contenido del XML exportado")
    class ExportContent {

        @Test
        @DisplayName("Proyecto con análisis PF produce XML no vacío")
        void exportProducesNonEmptyXml() {
            Optional<byte[]> result = fpXmlExportService.exportToXml(project.getId());
            assertThat(result).isPresent();
            assertThat(result.get()).isNotEmpty();
        }

        @Test
        @DisplayName("Proyecto sin análisis PF devuelve Optional vacío")
        void projectWithNoAnalysisReturnsEmpty() {
            User pm2 = createUser("pm_fp_xml_noanalysis", "pm_fp_xml_noanalysis@test.com");
            EstimationProject empty = createProject("Sin análisis FP", pm2);

            Optional<byte[]> result = fpXmlExportService.exportToXml(empty.getId());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("El XML exportado contiene las secciones esperadas")
        void exportedXmlContainsExpectedSections() {
            String xml = exportAsString();

            assertThat(xml).contains("systemBoundaryDescription");
            assertThat(xml).contains("modules");
            assertThat(xml).contains("requirements");
            assertThat(xml).contains("dataFunctions");
            assertThat(xml).contains("transactionalFunctions");
            assertThat(xml).contains("gscs");
            assertThat(xml).contains("weightMatrix");
        }

        @Test
        @DisplayName("El XML exportado no contiene IDs numéricos reales de BD")
        void exportedXmlContainsNoRealDatabaseIds() {
            String xml = exportAsString();

            // Database IDs appear as standalone numbers in attributes like id="123"
            // The export should only contain semantic refs (M1, RU1, etc.)
            assertThat(xml).doesNotContainPattern("\\bid=\"\\d+\"");
            assertThat(xml).doesNotContainPattern("\\bmoduleId=\"\\d+\"");
            assertThat(xml).doesNotContainPattern("\\brequirementId=\"\\d+\"");
        }

        @Test
        @DisplayName("El XML exportado usa referencias internas (M1, RU1)")
        void exportedXmlUsesInternalRefs() {
            String xml = exportAsString();

            assertThat(xml).contains("ref=\"M1\"");
            assertThat(xml).contains("ref=\"RU1\"");
            assertThat(xml).contains("moduleRef=\"M1\"");
            assertThat(xml).contains("requirementRef=\"RU1\"");
        }

        @Test
        @DisplayName("El XML exportado contiene los nombres de las funciones")
        void exportedXmlContainsFunctionNames() {
            String xml = exportAsString();

            assertThat(xml).contains("ILF Usuarios");
            assertThat(xml).contains("EI Alta usuario");
        }
    }

    // =========================================================
    // RoundTrip
    // =========================================================

    @Nested
    @DisplayName("Round-trip: exportar e importar")
    class RoundTrip {

        @Test
        @DisplayName("Exportar e importar en otro proyecto conserva los datos principales")
        void exportThenImportPreservesMainData() {
            // given — export from original project
            byte[] xmlBytes = fpXmlExportService.exportToXml(project.getId()).orElseThrow();

            // and — a fresh project to import into
            User pm2 = createUser("pm_fp_xml_rt_test", "pm_fp_xml_rt@test.com");
            EstimationProject targetProject = createProject("Proyecto FP RT", pm2);

            // when
            fpXmlImportService.importFromXml(targetProject, xmlBytes);

            // then — the target project now has an analysis with the same boundary and UFP
            FunctionPointAnalysis original =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            FunctionPointAnalysis imported =
                    fpAnalysisRepository.findByEstimationProjectId(targetProject.getId()).orElseThrow();

            assertThat(imported.getSystemBoundaryDescription())
                    .isEqualTo(original.getSystemBoundaryDescription());
            assertThat(imported.getUnadjustedFunctionPoints())
                    .isEqualTo(original.getUnadjustedFunctionPoints());
            assertThat(imported.getValueAdjustmentFactor())
                    .isEqualTo(original.getValueAdjustmentFactor());
        }

        @Test
        @DisplayName("Exportar e importar conserva el texto personalizado de una GSC")
        void exportThenImportPreservesGscCustomText() {
            // given
            String customText = "Comunicación de datos adaptada al proyecto";

            FunctionPointAnalysis originalBeforeExport = fpAnalysisRepository
                    .findByEstimationProjectId(project.getId())
                    .orElseThrow();

            findGsc(originalBeforeExport, GeneralSystemCharacteristicType.DATA_COMMUNICATIONS)
                    .setCustomText(customText);

            byte[] xmlBytes = fpXmlExportService.exportToXml(project.getId()).orElseThrow();
            String xml = new String(xmlBytes, StandardCharsets.UTF_8);

            assertThat(xml).contains("customText");
            assertThat(xml).contains(customText);

            User pm2 = createUser("pm_fp_xml_rt_custom_gsc", "pm_fp_xml_rt_custom_gsc@test.com");
            EstimationProject targetProject = createProject("Proyecto FP RT Custom GSC", pm2);

            // when
            fpXmlImportService.importFromXml(targetProject, xmlBytes);

            // then
            FunctionPointAnalysis imported = fpAnalysisRepository
                    .findByEstimationProjectId(targetProject.getId())
                    .orElseThrow();

            GeneralSystemCharacteristicAssessment importedGsc =
                    findGsc(imported, GeneralSystemCharacteristicType.DATA_COMMUNICATIONS);

            assertThat(importedGsc.getCustomText()).isEqualTo(customText);
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String exportAsString() {
        byte[] bytes = fpXmlExportService.exportToXml(project.getId()).orElseThrow();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private FunctionPointModule createModule(String name) {
        FunctionPointModule module = new FunctionPointModule();
        module.setName(name);
        fpModuleService.createForProject(project, module);
        // flush to get DB-assigned ID
        return fpModuleService.findAllByProjectId(project.getId()).stream()
                .filter(m -> name.equals(m.getName()))
                .findFirst().orElseThrow();
    }

    private UserRequirement createRequirement(FunctionPointModule module, String identifier,
                                              String statement) {
        UserRequirement req = new UserRequirement();
        req.setIdentifier(identifier);
        req.setStatement(statement);
        userRequirementService.createForModule(module, req);
        // flush to get ID
        return userRequirementService.findAllByModuleId(module.getId()).stream()
                .filter(r -> identifier.equals(r.getIdentifier()))
                .findFirst().orElseThrow();
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
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

    private GeneralSystemCharacteristicAssessment findGsc(
            FunctionPointAnalysis analysis,
            GeneralSystemCharacteristicType type
    ) {
        return analysis.getGeneralSystemCharacteristicAssessments()
                .stream()
                .filter(gsc -> gsc.getCharacteristicType() == type)
                .findFirst()
                .orElseThrow();
    }
}
