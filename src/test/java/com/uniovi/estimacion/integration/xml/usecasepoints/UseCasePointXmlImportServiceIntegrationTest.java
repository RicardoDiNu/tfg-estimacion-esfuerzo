package com.uniovi.estimacion.integration.xml.usecasepoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.InvalidUseCasePointXmlException;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointXmlImportService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Use Case Point XML import service — integration tests")
class UseCasePointXmlImportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UseCasePointXmlImportService ucpXmlImportService;

    @Autowired
    private UseCasePointAnalysisRepository ucpAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private EstimationProject project;

    // -------------------------------------------------------------------------
    // Minimal valid XML covering actors, modules, use cases and factors
    // -------------------------------------------------------------------------
    private static final String VALID_XML = """
            <useCasePointAnalysisExport version="1.0">
              <systemBoundaryDescription>Sistema de prueba UCP</systemBoundaryDescription>
              <actors>
                <actor ref="A1" complexity="SIMPLE">
                  <name>Usuario final</name>
                </actor>
                <actor ref="A2" complexity="COMPLEX">
                  <name>Servicio externo</name>
                </actor>
              </actors>
              <modules>
                <module ref="M1">
                  <name>Módulo autenticación</name>
                </module>
              </modules>
              <useCases>
                <useCase ref="UC1" moduleRef="M1" transactionCount="2">
                  <name>Login</name>
                  <actorRefs>
                    <actorRef>A1</actorRef>
                  </actorRefs>
                </useCase>
                <useCase ref="UC2" moduleRef="M1" transactionCount="5">
                  <name>Consulta avanzada</name>
                  <actorRefs>
                    <actorRef>A2</actorRef>
                  </actorRefs>
                </useCase>
              </useCases>
              <technicalFactors>
                <technicalFactor type="DISTRIBUTED_SYSTEM" degreeOfInfluence="3"/>
              </technicalFactors>
              <environmentalFactors>
                <environmentalFactor type="PROCESS_FAMILIARITY" degreeOfInfluence="4"/>
              </environmentalFactors>
            </useCasePointAnalysisExport>
            """;

    @BeforeEach
    void setUp() {
        User pm = createUser("pm_ucp_xml_import_test", "pm_ucp_xml_import@test.com");
        project = createProject("Proyecto UCP XML Import", pm);
    }

    // =========================================================
    // ValidImport
    // =========================================================

    @Nested
    @DisplayName("Importación válida")
    class ValidImport {

        @Test
        @DisplayName("XML válido crea análisis con actores, módulos, casos de uso y factores")
        void validXmlCreatesFullAnalysis() {
            // when
            ucpXmlImportService.importFromXml(project, bytes(VALID_XML));

            // then
            Optional<UseCasePointAnalysis> opt =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId());
            assertThat(opt).isPresent();

            UseCasePointAnalysis analysis = opt.get();
            assertThat(analysis.getSystemBoundaryDescription()).isEqualTo("Sistema de prueba UCP");
            // SIMPLE(1) + COMPLEX(3) = UAW=4
            // UC1: transactionCount=2 → SIMPLE(5), UC2: transactionCount=5 → AVERAGE(10) → UUCW=15
            // UUCP = 4 + 15 = 19
            assertThat(analysis.getUnadjustedUseCasePoints()).isEqualTo(19);
        }

        @Test
        @DisplayName("XML sin technicalFactors usa grados 0 por defecto")
        void xmlWithoutTechnicalFactorsDefaultsToZero() {
            String xmlNoTf = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="2">
                          <name>CU1</name>
                          <actorRefs><actorRef>A1</actorRef></actorRefs>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;

            // when
            ucpXmlImportService.importFromXml(project, bytes(xmlNoTf));

            // then — TCF = 0.6 + 0.01 * 0 = 0.6
            UseCasePointAnalysis analysis =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getTechnicalComplexityFactor()).isEqualTo(0.6);
        }

        @Test
        @DisplayName("XML sin environmentalFactors usa grados 0 por defecto")
        void xmlWithoutEnvironmentalFactorsDefaultsToZero() {
            String xmlNoEf = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="AVERAGE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="3">
                          <name>CU1</name>
                          <actorRefs><actorRef>A1</actorRef></actorRefs>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;

            // when
            ucpXmlImportService.importFromXml(project, bytes(xmlNoEf));

            // then — ECF = 1.4 - 0.03 * 0 = 1.4
            UseCasePointAnalysis analysis =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getEnvironmentalComplexityFactor()).isEqualTo(1.4);
        }

        @Test
        @DisplayName("XML sin actores ni casos de uso crea análisis con UUCP=0")
        void xmlWithNoActorsOrUseCasesProducesZeroUucp() {
            String xmlEmpty = """
                    <useCasePointAnalysisExport version="1.0">
                      <systemBoundaryDescription>Vacío</systemBoundaryDescription>
                    </useCasePointAnalysisExport>
                    """;

            // when
            ucpXmlImportService.importFromXml(project, bytes(xmlEmpty));

            // then
            UseCasePointAnalysis analysis =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            assertThat(analysis.getUnadjustedUseCasePoints()).isZero();
        }
    }

    // =========================================================
    // RejectedInputs
    // =========================================================

    @Nested
    @DisplayName("Rechaza entradas inválidas")
    class RejectedInputs {

        @Test
        @DisplayName("XML vacío lanza InvalidUseCasePointXmlException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, new byte[0]))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("XML null lanza InvalidUseCasePointXmlException")
        void nullXmlThrows() {
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, null))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("XML malformado lanza InvalidUseCasePointXmlException")
        void malformedXmlThrows() {
            byte[] garbage = "<<NOT-XML&&".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, garbage))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de actor duplicada lanza excepción")
        void duplicateActorRefThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor 1</name></actor>
                        <actor ref="A1" complexity="AVERAGE"><name>Actor 2 dup</name></actor>
                      </actors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de módulo duplicada lanza excepción")
        void duplicateModuleRefThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <modules>
                        <module ref="M1"><name>Módulo 1</name></module>
                        <module ref="M1"><name>Módulo dup</name></module>
                      </modules>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Referencia de caso de uso duplicada lanza excepción")
        void duplicateUseCaseRefThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="2">
                          <name>CU1</name>
                        </useCase>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="3">
                          <name>CU1 dup</name>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("moduleRef inexistente en caso de uso lanza excepción")
        void invalidModuleRefInUseCaseThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M999" transactionCount="2">
                          <name>CU1</name>
                          <actorRefs><actorRef>A1</actorRef></actorRefs>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("actorRef inexistente en caso de uso lanza excepción")
        void invalidActorRefInUseCaseThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="2">
                          <name>CU1</name>
                          <actorRefs><actorRef>A_NO_EXISTE</actorRef></actorRefs>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Complejidad de actor inválida lanza excepción")
        void invalidActorComplexityThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="MEGA_COMPLEX"><name>Actor</name></actor>
                      </actors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("transactionCount <= 0 lanza excepción")
        void invalidTransactionCountThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <actors>
                        <actor ref="A1" complexity="SIMPLE"><name>Actor</name></actor>
                      </actors>
                      <modules>
                        <module ref="M1"><name>Módulo</name></module>
                      </modules>
                      <useCases>
                        <useCase ref="UC1" moduleRef="M1" transactionCount="0">
                          <name>CU inválido</name>
                          <actorRefs><actorRef>A1</actorRef></actorRefs>
                        </useCase>
                      </useCases>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Tipo de factor técnico inválido lanza excepción")
        void invalidTechnicalFactorTypeThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <technicalFactors>
                        <technicalFactor type="FACTOR_INEXISTENTE" degreeOfInfluence="2"/>
                      </technicalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Tipo de factor ambiental inválido lanza excepción")
        void invalidEnvironmentalFactorTypeThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <environmentalFactors>
                        <environmentalFactor type="FACTOR_INVALIDO" degreeOfInfluence="1"/>
                      </environmentalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Factor técnico duplicado lanza excepción")
        void duplicateTechnicalFactorThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <technicalFactors>
                        <technicalFactor type="DISTRIBUTED_SYSTEM" degreeOfInfluence="2"/>
                        <technicalFactor type="DISTRIBUTED_SYSTEM" degreeOfInfluence="3"/>
                      </technicalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Factor ambiental duplicado lanza excepción")
        void duplicateEnvironmentalFactorThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <environmentalFactors>
                        <environmentalFactor type="PROCESS_FAMILIARITY" degreeOfInfluence="2"/>
                        <environmentalFactor type="PROCESS_FAMILIARITY" degreeOfInfluence="4"/>
                      </environmentalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Grado de factor técnico fuera de rango (>5) lanza excepción")
        void technicalFactorDegreeOutOfRangeThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <technicalFactors>
                        <technicalFactor type="DISTRIBUTED_SYSTEM" degreeOfInfluence="6"/>
                      </technicalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Grado de factor ambiental fuera de rango (negativo) lanza excepción")
        void environmentalFactorDegreeNegativeThrows() {
            String xml = """
                    <useCasePointAnalysisExport version="1.0">
                      <environmentalFactors>
                        <environmentalFactor type="STABLE_REQUIREMENTS" degreeOfInfluence="-1"/>
                      </environmentalFactors>
                    </useCasePointAnalysisExport>
                    """;
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(xml)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }

        @Test
        @DisplayName("Proyecto que ya tiene análisis UCP lanza excepción")
        void projectWithExistingAnalysisThrows() {
            // given — first import
            ucpXmlImportService.importFromXml(project, bytes(VALID_XML));
            ucpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();

            // when / then — second import on same project must fail
            assertThatThrownBy(() -> ucpXmlImportService.importFromXml(project, bytes(VALID_XML)))
                    .isInstanceOf(InvalidUseCasePointXmlException.class);
        }
    }

    // =========================================================
    // RollbackVerification
    // =========================================================

    @Nested
    @DisplayName("Verificación de rollback")
    class RollbackVerification {

        @Test
        @DisplayName("Un XML inválido no persiste ningún análisis UCP")
        void invalidXmlLeavesNoAnalysis() {
            // when
            assertThatThrownBy(() ->
                    ucpXmlImportService.importFromXml(project, bytes("<<BAD_XML"))
            ).isInstanceOf(InvalidUseCasePointXmlException.class);

            // then
            Optional<UseCasePointAnalysis> opt =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId());
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
