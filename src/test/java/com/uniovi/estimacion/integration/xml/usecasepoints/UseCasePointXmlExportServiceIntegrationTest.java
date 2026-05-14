package com.uniovi.estimacion.integration.xml.usecasepoints;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointAnalysisService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointXmlExportService;
import com.uniovi.estimacion.services.sizeanalyses.usecasepoints.UseCasePointXmlImportService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Use Case Point XML export service — integration tests")
class UseCasePointXmlExportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UseCasePointXmlExportService ucpXmlExportService;

    @Autowired
    private UseCasePointXmlImportService ucpXmlImportService;

    @Autowired
    private UseCasePointAnalysisService ucpAnalysisService;

    @Autowired
    private UseCasePointAnalysisRepository ucpAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private EstimationProject project;

    @BeforeEach
    void setUp() {
        User pm = createUser("pm_ucp_xml_export_test", "pm_ucp_xml_export@test.com");
        project = createProject("Proyecto UCP XML Export", pm);

        // Build a minimal UCP analysis: 1 actor, 1 module, 1 use case
        ucpAnalysisService.createInitialAnalysis(project, "Límite del sistema");

        UseCaseActor actor = new UseCaseActor();
        actor.setName("Usuario web");
        actor.setComplexity(UseCaseActorComplexity.AVERAGE);
        ucpAnalysisService.addActor(project.getId(), actor);
        // flush so actor gets a DB-assigned ID
        UseCaseActor persistedActor = ucpAnalysisService.findAllActorsByProjectId(project.getId())
                .stream().filter(a -> "Usuario web".equals(a.getName())).findFirst().orElseThrow();

        UseCasePointModule module = new UseCasePointModule();
        module.setName("Módulo gestión");
        ucpAnalysisService.addModule(project.getId(), module);
        // flush so module gets a DB-assigned ID
        UseCasePointModule persistedModule =
                ucpAnalysisService.findAllModulesByProjectId(project.getId())
                        .stream().filter(m -> "Módulo gestión".equals(m.getName()))
                        .findFirst().orElseThrow();

        UseCaseEntry useCase = new UseCaseEntry();
        useCase.setName("Registrar usuario");
        useCase.setTransactionCount(4);
        ucpAnalysisService.addUseCaseToModule(
                project.getId(),
                persistedModule.getId(),
                useCase,
                List.of(persistedActor.getId())
        );
    }

    // =========================================================
    // ExportContent
    // =========================================================

    @Nested
    @DisplayName("Contenido del XML exportado")
    class ExportContent {

        @Test
        @DisplayName("Proyecto con análisis UCP produce XML no vacío")
        void exportProducesNonEmptyXml() {
            Optional<byte[]> result = ucpXmlExportService.exportToXml(project.getId());
            assertThat(result).isPresent();
            assertThat(result.get()).isNotEmpty();
        }

        @Test
        @DisplayName("Proyecto sin análisis UCP devuelve Optional vacío")
        void projectWithNoAnalysisReturnsEmpty() {
            User pm2 = createUser("pm_ucp_xml_noanalysis", "pm_ucp_xml_noanalysis@test.com");
            EstimationProject empty = createProject("Sin análisis UCP", pm2);

            Optional<byte[]> result = ucpXmlExportService.exportToXml(empty.getId());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("El XML exportado contiene las secciones esperadas")
        void exportedXmlContainsExpectedSections() {
            String xml = exportAsString();

            assertThat(xml).contains("systemBoundaryDescription");
            assertThat(xml).contains("actors");
            assertThat(xml).contains("modules");
            assertThat(xml).contains("useCases");
            assertThat(xml).contains("technicalFactors");
            assertThat(xml).contains("environmentalFactors");
        }

        @Test
        @DisplayName("El XML exportado no contiene IDs numéricos reales de BD")
        void exportedXmlContainsNoRealDatabaseIds() {
            String xml = exportAsString();

            assertThat(xml).doesNotContainPattern("\\bid=\"\\d+\"");
            assertThat(xml).doesNotContainPattern("\\bactorId=\"\\d+\"");
            assertThat(xml).doesNotContainPattern("\\bmoduleId=\"\\d+\"");
        }

        @Test
        @DisplayName("El XML exportado usa referencias internas (A1, M1, UC1)")
        void exportedXmlUsesInternalRefs() {
            String xml = exportAsString();

            assertThat(xml).contains("ref=\"A1\"");
            assertThat(xml).contains("ref=\"M1\"");
            assertThat(xml).contains("ref=\"UC1\"");
            assertThat(xml).contains("moduleRef=\"M1\"");
        }

        @Test
        @DisplayName("El XML exportado contiene los nombres del actor, módulo y caso de uso")
        void exportedXmlContainsEntityNames() {
            String xml = exportAsString();

            assertThat(xml).contains("Usuario web");
            assertThat(xml).contains("Módulo gestión");
            assertThat(xml).contains("Registrar usuario");
        }

        @Test
        @DisplayName("El XML exportado contiene la referencia al actor dentro del caso de uso")
        void exportedXmlContainsActorRefInUseCase() {
            String xml = exportAsString();

            assertThat(xml).contains("actorRef");
            assertThat(xml).contains("A1");
        }
    }

    // =========================================================
    // RoundTrip
    // =========================================================

    @Nested
    @DisplayName("Round-trip: exportar e importar")
    class RoundTrip {

        @Test
        @DisplayName("Exportar e importar en otro proyecto conserva UUCP y descripción")
        void exportThenImportPreservesMainData() {
            // given — export from original project
            byte[] xmlBytes = ucpXmlExportService.exportToXml(project.getId()).orElseThrow();

            // and — a fresh project to import into
            User pm2 = createUser("pm_ucp_xml_rt_test", "pm_ucp_xml_rt@test.com");
            EstimationProject targetProject = createProject("Proyecto UCP RT", pm2);

            // when
            ucpXmlImportService.importFromXml(targetProject, xmlBytes);

            // then
            UseCasePointAnalysis original =
                    ucpAnalysisRepository.findByEstimationProjectId(project.getId()).orElseThrow();
            UseCasePointAnalysis imported =
                    ucpAnalysisRepository.findByEstimationProjectId(targetProject.getId()).orElseThrow();

            assertThat(imported.getSystemBoundaryDescription())
                    .isEqualTo(original.getSystemBoundaryDescription());
            assertThat(imported.getUnadjustedUseCasePoints())
                    .isEqualTo(original.getUnadjustedUseCasePoints());
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String exportAsString() {
        byte[] bytes = ucpXmlExportService.exportToXml(project.getId()).orElseThrow();
        return new String(bytes, StandardCharsets.UTF_8);
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
}
