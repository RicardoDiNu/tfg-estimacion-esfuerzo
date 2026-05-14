package com.uniovi.estimacion.integration.xml.functionpoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.FunctionPointXmlImportService;
import com.uniovi.estimacion.services.sizeanalyses.functionpoints.InvalidFunctionPointXmlException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Function Point XML import service — integration tests")
class FunctionPointXmlImportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FunctionPointXmlImportService fpXmlImportService;

    @Autowired
    private FunctionPointAnalysisRepository fpAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private EstimationProject project;

    // -------------------------------------------------------------------------
    // Minimal valid XML that exercises all sections
    // -------------------------------------------------------------------------
    private static final String VALID_XML = """
            <functionPointAnalysisExport version="1.0">
              <systemBoundaryDescription>Sistema de prueba</systemBoundaryDescription>
              <modules>
                <module ref="M1">
                  <name>Módulo principal</name>
                </module>
              </modules>
              <requirements>
                <requirement ref="RU1" moduleRef="M1">
                  <identifier>RU-001</identifier>
                  <statement>Gestión de clientes</statement>
                </requirement>
              </requirements>
              <dataFunctions>
                <dataFunction requirementRef="RU1">
                  <name>ILF Clientes</name>
                  <type>ILF</type>
                  <complexity>AVERAGE</complexity>
                </dataFunction>
              </dataFunctions>
              <transactionalFunctions>
                <transactionalFunction requirementRef="RU1">
                  <name>EO Informe</name>
                  <type>EO</type>
                  <complexity>HIGH</complexity>
                </transactionalFunction>
              </transactionalFunctions>
              <gscs>
                <gsc type="DATA_COMMUNICATIONS" degreeOfInfluence="2"/>
              </gscs>
              <weightMatrix>
                <entry functionType="ILF" complexity="AVERAGE" weight="11"/>
              </weightMatrix>
            </functionPointAnalysisExport>
            """;

    @BeforeEach
    void setUp() {
        User pm = createUser("pm_fp_xml_import_test", "pm_fp_xml_import@test.com");
        project = createProject("Proyecto FP XML Import", pm);
    }

    // =========================================================
    // ValidImport
    // =========================================================

    @Nested
    @DisplayName("Importación válida")
    class ValidImport {

        @Test
        @DisplayName("XML válido crea análisis con módulo, requisito, funciones y GSC")
        void validXmlCreatesFullAnalysis() {
            // when
            fpXmlImportService.importFromXml(project, bytes(VALID_XML));

            // then
            Optional<FunctionPointAnalysis> opt =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId());
            assertThat(opt).isPresent();

            FunctionPointAnalysis analysis = opt.get();
            assertThat(analysis.getSystemBoundaryDescription()).isEqualTo("Sistema de prueba");
            // ILF AVERAGE weight overridden to 11, EO HIGH default=7 → UFP=18
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(18);
            // Only DATA_COMMUNICATIONS=2, rest default to 0 → TDI=2, VAF=0.65+0.02=0.67
            assertThat(analysis.getValueAdjustmentFactor()).isEqualTo(0.67);
            assertThat(analysis.getAdjustedFunctionPoints()).isEqualTo(12.06);
        }

        @Test
        @DisplayName("XML sin weightMatrix usa pesos por defecto")
        void xmlWithoutWeightMatrixUsesDefaults() {
            String xmlNoMatrix = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                      <dataFunctions>
                        <dataFunction requirementRef="RU1">
                          <name>ILF X</name>
                          <type>ILF</type>
                          <complexity>AVERAGE</complexity>
                        </dataFunction>
                      </dataFunctions>
                    </functionPointAnalysisExport>
                    """;

            // when
            fpXmlImportService.importFromXml(project, bytes(xmlNoMatrix));

            // then — ILF AVERAGE default weight = 10
            FunctionPointAnalysis analysis =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isEqualTo(10);
        }

        @Test
        @DisplayName("XML sin gscs crea análisis con todos los GSC a grado 0")
        void xmlWithoutGscsDefaultsToZeroDegree() {
            String xmlNoGsc = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                    </functionPointAnalysisExport>
                    """;

            // when
            fpXmlImportService.importFromXml(project, bytes(xmlNoGsc));

            // then — TDI=0, VAF=0.65
            FunctionPointAnalysis analysis =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getTotalDegreeOfInfluence()).isZero();
            assertThat(analysis.getValueAdjustmentFactor()).isEqualTo(0.65);
        }

        @Test
        @DisplayName("XML sin funciones crea análisis con UFP=0")
        void xmlWithNoFunctionsProducesZeroUfp() {
            String xmlNoFunctions = """
                    <functionPointAnalysisExport version="1.0">
                      <systemBoundaryDescription>Sin funciones</systemBoundaryDescription>
                      <modules>
                        <module ref="M1"><name>Módulo vacío</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req sin funciones</statement>
                        </requirement>
                      </requirements>
                    </functionPointAnalysisExport>
                    """;

            // when
            fpXmlImportService.importFromXml(project, bytes(xmlNoFunctions));

            // then
            FunctionPointAnalysis analysis =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedFunctionPoints()).isZero();
        }
    }

    // =========================================================
    // RejectedInputs
    // =========================================================

    @Nested
    @DisplayName("Rechaza entradas inválidas")
    class RejectedInputs {

        @Test
        @DisplayName("XML vacío lanza InvalidFunctionPointXmlException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, new byte[0]))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("XML null lanza InvalidFunctionPointXmlException")
        void nullXmlThrows() {
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, null))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("XML malformado lanza InvalidFunctionPointXmlException")
        void malformedXmlThrows() {
            byte[] garbage = "<<NOT-XML&&".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, garbage))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de módulo inexistente en requisito lanza excepción")
        void brokenModuleRefInRequirementThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M999">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de requisito inexistente en función de datos lanza excepción")
        void brokenRequirementRefInDataFunctionThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                      <dataFunctions>
                        <dataFunction requirementRef="RU999">
                          <name>ILF X</name>
                          <type>ILF</type>
                          <complexity>AVERAGE</complexity>
                        </dataFunction>
                      </dataFunctions>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de requisito inexistente en función transaccional lanza excepción")
        void brokenRequirementRefInTransactionalFunctionThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                      <transactionalFunctions>
                        <transactionalFunction requirementRef="RU_NO_EXISTE">
                          <name>EI Login</name>
                          <type>EI</type>
                          <complexity>LOW</complexity>
                        </transactionalFunction>
                      </transactionalFunctions>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Tipo de función de datos inválido lanza excepción")
        void invalidDataFunctionTypeThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                      <dataFunctions>
                        <dataFunction requirementRef="RU1">
                          <name>DF X</name>
                          <type>PATATA</type>
                          <complexity>AVERAGE</complexity>
                        </dataFunction>
                      </dataFunctions>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Complejidad inválida en función de datos lanza excepción")
        void invalidComplexityInDataFunctionThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <requirements>
                        <requirement ref="RU1" moduleRef="M1">
                          <identifier>RU-001</identifier>
                          <statement>Req</statement>
                        </requirement>
                      </requirements>
                      <dataFunctions>
                        <dataFunction requirementRef="RU1">
                          <name>ILF X</name>
                          <type>ILF</type>
                          <complexity>MEDIUM</complexity>
                        </dataFunction>
                      </dataFunctions>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Tipo de GSC inválido lanza excepción")
        void invalidGscTypeThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <gscs>
                        <gsc type="NO_EXISTE_GSC" degreeOfInfluence="2"/>
                      </gscs>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Grado de influencia GSC fuera de rango (>5) lanza excepción")
        void gscDegreeOutOfRangeThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <gscs>
                        <gsc type="DATA_COMMUNICATIONS" degreeOfInfluence="9"/>
                      </gscs>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Peso de la matriz fuera de rango (0) lanza excepción")
        void weightOutOfRangeLowThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <weightMatrix>
                        <entry functionType="EI" complexity="LOW" weight="0"/>
                      </weightMatrix>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Peso de la matriz fuera de rango (1000) lanza excepción")
        void weightOutOfRangeHighThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <weightMatrix>
                        <entry functionType="EI" complexity="LOW" weight="1000"/>
                      </weightMatrix>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Combinación de peso duplicada en la matriz lanza excepción")
        void duplicateWeightCombinationThrows() {
            String xml = """
                    <functionPointAnalysisExport version="1.0">
                      <weightMatrix>
                        <entry functionType="EI" complexity="LOW" weight="3"/>
                        <entry functionType="EI" complexity="LOW" weight="5"/>
                      </weightMatrix>
                    </functionPointAnalysisExport>
                    """;
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }

        @Test
        @DisplayName("Proyecto que ya tiene análisis PF lanza excepción")
        void projectWithExistingAnalysisThrows() {
            // given — first import creates the analysis
            fpXmlImportService.importFromXml(project, bytes(VALID_XML));
            fpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();

            // when / then — second import must be rejected
            // Use a second project with same XML to confirm the check is project-scoped
            User pm2 = createUser("pm_fp_xml_dup_test", "pm_fp_xml_dup@test.com");
            EstimationProject project2 = createProject("Proyecto FP XML Dup", pm2);
            // second import on project2 should succeed (different project)
            assertThatCode(() -> fpXmlImportService.importFromXml(project2, bytes(VALID_XML)))
                    .doesNotThrowAnyException();
            // but a second import on project1 must fail
            assertThatThrownBy(() -> fpXmlImportService.importFromXml(project, bytes(VALID_XML)))
                    .isInstanceOf(InvalidFunctionPointXmlException.class);
        }
    }

    // =========================================================
    // RollbackVerification
    // =========================================================

    @Nested
    @DisplayName("Verificación de rollback")
    class RollbackVerification {

        @Test
        @DisplayName("Un XML inválido no deja datos huérfanos: no se crea análisis")
        void invalidXmlLeavesNoAnalysis() {
            // when — invalid import
            assertThatThrownBy(() ->
                    fpXmlImportService.importFromXml(project, bytes("<<BAD_XML"))
            ).isInstanceOf(InvalidFunctionPointXmlException.class);

            // then — no analysis was persisted (validation happens before save)
            Optional<FunctionPointAnalysis> opt =
                    fpAnalysisRepository.findByEstimationProjectId(project.getId());
            assertThat(opt).isEmpty();
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

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

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
