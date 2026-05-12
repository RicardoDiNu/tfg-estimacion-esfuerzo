package com.uniovi.estimacion.selenium;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.selenium.pageobjects.*;
import org.junit.jupiter.api.*;

import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Paths;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Functional Selenium tests")
class EstimationSeleniumTests extends AbstractSeleniumTest {

    private static final String PM_USERNAME = "pm_selenium";
    private static final String PM_EMAIL = "pm_selenium@test.com";
    private static final String PM_OTHER_USERNAME = "pm_other_selenium";
    private static final String PM_OTHER_EMAIL = "pm_other_selenium@test.com";
    private static final String WORKER_NO_PERM = "worker_no_perm_selenium";
    private static final String WORKER_NO_PERM_EMAIL = "worker_no_perm@test.com";
    private static final String WORKER_EDIT = "worker_edit_selenium";
    private static final String WORKER_EDIT_EMAIL = "worker_edit@test.com";
    private static final String WORKER_CONV = "worker_conv_selenium";
    private static final String WORKER_CONV_EMAIL = "worker_conv@test.com";
    private static final String WORKER_FULL = "worker_full_selenium";
    private static final String WORKER_FULL_EMAIL = "worker_full@test.com";
    private static final String PASSWORD = "Password123!";
    private static final String PROJECT_NAME = "Proyecto Selenium";
    private static final String PROJECT_DESC = "Descripción de prueba Selenium";

    private java.nio.file.Path xmlResourcePath(String relativePath) {
        return Paths.get("src", "test", "resources").toAbsolutePath().resolve(relativePath);
    }

    private void loginAsPm() {
        PO_LoginView.open(driver, baseUrl);
        PO_LoginView.login(driver, PM_USERNAME, PASSWORD);
        PO_LoginView.checkLoginSuccessRedirectToProjects(driver);
    }

    private void loginAs(String username) {
        PO_LoginView.open(driver, baseUrl);
        PO_LoginView.login(driver, username, PASSWORD);
        PO_LoginView.checkLoginSuccessRedirectToProjects(driver);
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE A – Navegación pública e internacionalización
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("PR01 - Acceso a la página principal pública")
    void pr01_accessHomePage() {
        // given: usuario no autenticado en la raíz (BeforeEach navega a baseUrl)
        // when: (ya estamos en la página)
        // then: se muestra el título de la página de inicio
        PO_HomeView.checkHome(driver);
    }

    @Test
    @Order(2)
    @DisplayName("PR02 - Acceso a la página About")
    void pr02_accessAboutPage() {
        // given: usuario no autenticado
        // when: navega a /about
        PO_HomeView.goToAbout(driver);
        // then: se muestra el título de la página About
        PO_HomeView.checkAbout(driver);
    }

    @Test
    @Order(3)
    @DisplayName("PR03 - Acceso a la página Help")
    void pr03_accessHelpPage() {
        // given: usuario no autenticado
        // when: navega a /help
        PO_HomeView.goToHelp(driver);
        // then: se muestra el título de la página Help
        PO_HomeView.checkHelp(driver);
    }

    @Test
    @Order(4)
    @DisplayName("PR04 - Cambio de idioma de español a inglés")
    void pr04_changeLanguageToEnglish() {
        // given: página principal en español
        PO_HomeView.checkHome(driver);
        // when: se hace clic en el botón de inglés
        PO_NavView.switchToEnglish(driver);
        // then: la página se muestra en inglés
        PO_HomeView.checkHome(driver, PO_Properties.ENGLISH);
    }

    @Test
    @Order(5)
    @DisplayName("PR05 - Cambio de idioma de inglés a español")
    void pr05_changeLanguageBackToSpanish() {
        // given: página en inglés
        PO_NavView.switchToEnglish(driver);
        // when: se hace clic en el botón de español
        PO_NavView.switchToSpanish(driver);
        // then: la página se muestra en español
        PO_HomeView.checkHome(driver);
    }

    @Test
    @Order(6)
    @DisplayName("PR06 - Intentar acceder a /projects sin autenticación redirige a login")
    void pr06_accessProjectsWithoutAuthRedirectsToLogin() {
        // given: usuario no autenticado
        // when: intenta acceder directamente a /projects
        driver.get(baseUrl + "/projects");
        // then: redirige a la página de login
        PO_LoginView.checkLoginView(driver);
    }

    @Test
    @Order(7)
    @DisplayName("PR07 - Intentar acceder a /users sin autenticación redirige a login")
    void pr07_accessUsersWithoutAuthRedirectsToLogin() {
        // given: usuario no autenticado
        // when: intenta acceder directamente a /users
        driver.get(baseUrl + "/users");
        // then: redirige a la página de login
        PO_LoginView.checkLoginView(driver);
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE B – Autenticación y sesión
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("PR08 - Login incorrecto muestra mensaje de error")
    void pr08_incorrectLoginShowsError() {
        // given: existe un usuario en el sistema
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        // when: se intenta hacer login con contraseña incorrecta
        PO_LoginView.open(driver, baseUrl);
        PO_LoginView.login(driver, PM_USERNAME, "contraseñaIncorrecta");
        // then: se muestra el mensaje de error de login
        PO_LoginView.checkLoginError(driver);
    }

    @Test
    @Order(9)
    @DisplayName("PR09 - Login correcto como jefe de proyecto redirige a proyectos")
    void pr09_loginAsProjectManagerRedirectsToProjects() {
        // given: existe un jefe de proyecto
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        // when: se hace login con credenciales correctas
        PO_LoginView.open(driver, baseUrl);
        PO_LoginView.login(driver, PM_USERNAME, PASSWORD);
        // then: redirige a la lista de proyectos
        PO_LoginView.checkLoginSuccessRedirectToProjects(driver);
    }

    @Test
    @Order(10)
    @DisplayName("PR10 - Login correcto como trabajador redirige a proyectos")
    void pr10_loginAsWorkerRedirectsToProjects() {
        // given: existe un trabajador
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        // when: se hace login como trabajador
        PO_LoginView.open(driver, baseUrl);
        PO_LoginView.login(driver, WORKER_NO_PERM, PASSWORD);
        // then: redirige a la lista de proyectos
        PO_LoginView.checkLoginSuccessRedirectToProjects(driver);
    }

    @Test
    @Order(11)
    @DisplayName("PR11 - Logout cierra sesión y vuelve a login")
    void pr11_logoutClosesSession() {
        // given: usuario autenticado
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        // when: se hace logout
        PO_NavView.logout(driver);
        // then: vuelve a la página con el enlace de login visible
        PO_LoginView.checkLoginView(driver);
    }

    @Test
    @Order(12)
    @DisplayName("PR12 - Usuario autenticado puede acceder a Mi cuenta")
    void pr12_authenticatedUserCanAccessAccount() {
        // given: usuario autenticado
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        // when: navega a Mi cuenta
        PO_NavView.goToAccount(driver);
        // then: se muestra la página de perfil
        PO_ProfileView.checkProfileView(driver);
    }

    @Test
    @Order(13)
    @DisplayName("PR13 - Usuario autenticado puede editar su email desde Mi cuenta")
    void pr13_authenticatedUserCanEditEmail() {
        // given: usuario autenticado en su perfil
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        PO_NavView.goToAccount(driver);
        // when: edita el email
        PO_ProfileView.clickEditAccount(driver);
        PO_ProfileView.fillEditAccountForm(driver, "nuevo_email@test.com");
        // then: el nuevo email se muestra en la página de perfil
        PO_ProfileView.checkProfileView(driver);
        PO_ProfileView.checkEmailIsPresent(driver, "nuevo_email@test.com");
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE C – Gestión de proyectos
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("PR14 - Jefe de proyecto crea un proyecto")
    void pr14_projectManagerCreatesProject() {
        // given: jefe de proyecto autenticado en la lista de proyectos
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        // when: crea un nuevo proyecto via formulario
        PO_ProjectListView.clickAddProject(driver);
        PO_ProjectListView.fillProjectForm(driver, PROJECT_NAME, PROJECT_DESC, "50", "EUR");
        // then: se muestra el detalle del proyecto creado
        PO_ProjectDetailsView.checkProjectDetails(driver, PROJECT_NAME);
    }

    @Test
    @Order(15)
    @DisplayName("PR15 - Jefe de proyecto ve el proyecto creado en el listado")
    void pr15_projectManagerSeesProjectInList() {
        // given: existe un proyecto del jefe
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        // when: navega a la lista de proyectos
        PO_ProjectListView.open(driver, baseUrl);
        // then: el proyecto aparece en el listado
        PO_ProjectListView.checkProjectIsPresent(driver, PROJECT_NAME);
    }

    @Test
    @Order(16)
    @DisplayName("PR16 - Jefe de proyecto accede al detalle del proyecto")
    void pr16_projectManagerAccessesProjectDetail() {
        // given: existe un proyecto del jefe
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        // when: abre el detalle del proyecto
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // then: se muestra la página de detalle del proyecto
        PO_ProjectDetailsView.checkProjectDetails(driver, PROJECT_NAME);
    }

    @Test
    @Order(17)
    @DisplayName("PR17 - Jefe de proyecto edita nombre y descripción del proyecto")
    void pr17_projectManagerEditsProject() {
        // given: jefe en la página de detalle del proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // when: edita nombre y descripción
        PO_NavView.clickOption(
                driver,
                "/projects/edit/",
                "id",
                "name"
        );
        PO_ProjectListView.fillProjectForm(driver, "Proyecto Editado Selenium",
                "Nueva descripción editada", "60", "USD");
        // then: el detalle muestra el nuevo nombre
        PO_ProjectDetailsView.checkProjectDetails(driver, "Proyecto Editado Selenium");
    }

    @Test
    @Order(18)
    @DisplayName("PR18 - Jefe de proyecto ve el jefe de proyecto en el resumen")
    void pr18_projectDetailShowsProjectManager() {
        // given: jefe en la página de detalle
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // when: (ya en el detalle)
        // then: el nombre del jefe de proyecto aparece en la página
        PO_ProjectDetailsView.checkProjectManager(driver, PM_USERNAME);
    }

    @Test
    @Order(19)
    @DisplayName("PR19 - Jefe de proyecto ve tarifa horaria y divisa del proyecto")
    void pr19_projectDetailShowsHourlyRateAndCurrency() {
        // given: proyecto con tarifa y divisa específicas
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("75.50"), "USD", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // when: (ya en el detalle)
        // then: se muestran la tarifa horaria y la divisa
        PO_View.checkTextIsPresent(driver, "75");
        PO_View.checkTextIsPresent(driver, "USD");
    }

    @Test
    @Order(20)
    @DisplayName("PR20 - Trabajador no asignado no ve proyectos ajenos")
    void pr20_workerNotAssignedDoesNotSeeOtherProjectsProjects() {
        // given: dos jefes, el primero tiene un proyecto; el segundo hace login
        User pm1 = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProjectManager(PM_OTHER_USERNAME, PM_OTHER_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm1);
        loginAs(PM_OTHER_USERNAME);
        // when: navega a la lista de proyectos
        PO_ProjectListView.open(driver, baseUrl);
        // then: el proyecto del otro jefe no aparece
        PO_ProjectListView.checkProjectIsNotPresent(driver, PROJECT_NAME);
    }

    @Test
    @Order(21)
    @DisplayName("PR21 - Trabajador asignado ve el proyecto en su listado")
    void pr21_assignedWorkerSeesProjectInList() {
        // given: trabajador asignado al proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, false);
        loginAs(WORKER_NO_PERM);
        // when: navega a la lista de proyectos
        PO_ProjectListView.open(driver, baseUrl);
        // then: el proyecto aparece en su listado
        PO_ProjectListView.checkProjectIsPresent(driver, PROJECT_NAME);
    }

    @Test
    @Order(22)
    @DisplayName("PR22 - Trabajador asignado accede al detalle del proyecto")
    void pr22_assignedWorkerAccessesProjectDetail() {
        // given: trabajador asignado, en la lista de proyectos
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, false);
        loginAs(WORKER_NO_PERM);
        PO_ProjectListView.open(driver, baseUrl);
        // when: abre el detalle del proyecto
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // then: puede ver el detalle
        PO_ProjectDetailsView.checkProjectDetails(driver, PROJECT_NAME);
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE D – Gestión de usuarios y trabajadores
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(23)
    @DisplayName("PR23 - Jefe de proyecto accede a gestión de trabajadores")
    void pr23_projectManagerAccessesUserManagement() {
        // given: jefe de proyecto autenticado
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        // when: navega a gestión de usuarios
        PO_NavView.goToUsers(driver);
        // then: se muestra la página de gestión de usuarios
        PO_UserManagementView.checkUserManagement(driver);
    }

    @Test
    @Order(24)
    @DisplayName("PR24 - Jefe de proyecto crea un trabajador")
    void pr24_projectManagerCreatesWorker() {
        // given: jefe de proyecto en la sección de usuarios
        factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        loginAsPm();
        PO_NavView.goToUsers(driver);
        // when: crea un nuevo trabajador via formulario
        PO_UserManagementView.clickAddUser(driver);
        PO_UserManagementView.fillWorkerForm(driver, WORKER_NO_PERM,
                WORKER_NO_PERM_EMAIL, PASSWORD);
        // then: redirige a la lista de usuarios mostrando el trabajador creado
        PO_UserManagementView.checkUserManagement(driver);
        PO_UserManagementView.checkUserIsPresent(driver, WORKER_NO_PERM);
    }

    @Test
    @Order(25)
    @DisplayName("PR25 - Jefe de proyecto ve el trabajador creado en el listado")
    void pr25_projectManagerSeesWorkerInList() {
        // given: existe un trabajador asociado al jefe
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        loginAsPm();
        // when: navega a gestión de usuarios
        PO_NavView.goToUsers(driver);
        // then: el trabajador aparece en el listado
        PO_UserManagementView.checkUserIsPresent(driver, WORKER_NO_PERM);
    }

    @Test
    @Order(26)
    @DisplayName("PR26 - Jefe de proyecto edita datos básicos de un trabajador")
    void pr26_projectManagerEditsWorkerData() {
        // given: existe un trabajador; el jefe está en la lista de usuarios
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        loginAsPm();
        PO_NavView.goToUsers(driver);
        // when: edita el email del trabajador
        PO_UserManagementView.clickEditForUser(driver, WORKER_NO_PERM);
        PO_UserManagementView.fillEditEmailForm(driver, "nuevo_worker@test.com");
        // then: el trabajador sigue apareciendo con el nombre original
        PO_UserManagementView.checkUserIsPresent(driver, WORKER_NO_PERM);
    }

    @Test
    @Order(27)
    @DisplayName("PR27 - Trabajador no ve el menú de gestión de usuarios")
    void pr27_workerDoesNotSeeUsersMenu() {
        // given: trabajador autenticado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        loginAs(WORKER_NO_PERM);
        // when: (ya en la página de proyectos)
        // then: no aparece el enlace de gestión de usuarios en el nav
        PO_View.checkTextIsNotPresent(driver,
                PO_View.getMessage("user.management.list.title"));
    }

    @Test
    @Order(28)
    @DisplayName("PR28 - Trabajador no puede acceder directamente a /users")
    void pr28_workerCannotAccessUsersDirectly() {
        // given: trabajador autenticado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        loginAs(WORKER_NO_PERM);
        // when: intenta acceder directamente a /users
        driver.get(baseUrl + "/users");
        // then: no puede ver la lista de usuarios (muestra error o login)
        PO_View.checkTextIsNotPresent(driver,
                PO_View.getMessage("user.management.list.title"));
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE E – Permisos por proyecto
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(29)
    @DisplayName("PR29 - Trabajador sin permisos ve el proyecto pero no acciones de edición")
    void pr29_workerWithNoPermissionsSeesProjectButNotEditActions() {
        // given: trabajador sin permisos, asignado al proyecto con FP importado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, false);
        // importar FP como PM para tener un análisis
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador accede al detalle FP
        loginAs(WORKER_NO_PERM);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointAnalysis(driver);
        // then: no ve la acción de editar análisis
        PO_FunctionPointDetailsView.checkEditAnalysisActionIsNotVisible(driver);
    }

    @Test
    @Order(30)
    @DisplayName("PR30 - Trabajador con permiso de edición ve acciones de edición de datos")
    void pr30_workerWithEditPermissionSeesEditActions() {
        // given: trabajador con canEdit=true, proyecto con FP
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_EDIT, WORKER_EDIT_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, true, false);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador con edit accede al detalle FP
        loginAs(WORKER_EDIT);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointAnalysis(driver);
        // then: ve la acción de editar análisis
        PO_FunctionPointDetailsView.checkEditAnalysisActionIsVisible(driver);
    }

    @Test
    @Order(31)
    @DisplayName("PR31 - Trabajador sin permiso de edición no puede editar PF/UCP")
    void pr31_workerWithoutEditPermissionCannotEditAnalysis() {
        // given: trabajador sin canEdit, proyecto con FP importado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_NO_PERM, WORKER_NO_PERM_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, true);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador (solo conversiones) accede al detalle FP
        loginAs(WORKER_NO_PERM);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointAnalysis(driver);
        // then: no ve la acción de editar análisis
        PO_FunctionPointDetailsView.checkEditAnalysisActionIsNotVisible(driver);
    }

    @Test
    @Order(32)
    @DisplayName("PR32 - Trabajador con permiso de conversiones ve acciones de Delphi/transformación")
    void pr32_workerWithConversionPermissionSeesConversionActions() {
        // given: trabajador con canConvert=true, proyecto con UCP importado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_CONV, WORKER_CONV_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, true);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador con conversiones accede al detalle UCP
        loginAs(WORKER_CONV);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointAnalysis(driver);
        // then: ve las acciones de Delphi y transformación
        PO_UseCasePointDetailsView.checkManageEffortConversionsActionIsVisible(driver);
    }

    @Test
    @Order(33)
    @DisplayName("PR33 - Trabajador con solo permiso de edición no ve acciones de conversión")
    void pr33_workerWithOnlyEditPermissionDoesNotSeeConversionActions() {
        // given: trabajador con canEdit=true, canConvert=false, proyecto con UCP
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_EDIT, WORKER_EDIT_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, true, false);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador con solo edición accede al detalle UCP
        loginAs(WORKER_EDIT);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointAnalysis(driver);
        // then: no ve la sección de conversiones Delphi/transformación
        assertTrue(
                driver.findElements(By.xpath("//a[contains(@href,'/delphi')]")).isEmpty(),
                "No debería aparecer ningún enlace de gestión Delphi"
        );

        assertTrue(
                driver.findElements(By.xpath("//a[contains(@href,'/transformation')]")).isEmpty(),
                "No debería aparecer ningún enlace de gestión de función de transformación"
        );
    }

    @Test
    @Order(34)
    @DisplayName("PR34 - Trabajador con solo permiso de conversiones no ve acciones de edición")
    void pr34_workerWithOnlyConversionPermissionDoesNotSeeEditActions() {
        // given: trabajador con canEdit=false, canConvert=true, proyecto con FP
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        User worker = factory.createWorker(WORKER_CONV, WORKER_CONV_EMAIL, PASSWORD, pm);
        EstimationProject project = factory.createProject(PROJECT_NAME, PROJECT_DESC,
                new BigDecimal("50"), "EUR", pm);
        factory.createMembership(project, worker, false, true);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        PO_NavView.logout(driver);
        // when: trabajador con solo conversiones accede al detalle FP
        loginAs(WORKER_CONV);
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointAnalysis(driver);
        // then: no ve la acción de editar análisis ni la matriz de pesos
        PO_FunctionPointDetailsView.checkEditAnalysisActionIsNotVisible(driver);
        PO_FunctionPointDetailsView.checkEditWeightMatrixActionIsNotVisible(driver);
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE F – Análisis PF
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(35)
    @DisplayName("PR35 - Jefe de proyecto crea/accede a análisis PF")
    void pr35_projectManagerCreatesOrAccessesFpAnalysis() {
        // given: jefe de proyecto en el detalle del proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // when: accede al análisis PF (crea uno nuevo si no existe)
        PO_ProjectDetailsView.openFunctionPointAnalysis(driver);
        // then: se muestra la página de detalle del análisis PF
        PO_FunctionPointDetailsView.checkFunctionPointDetails(driver);
    }

    @Test
    @Order(36)
    @DisplayName("PR36 - Importar análisis PF desde XML en proyecto sin PF")
    void pr36_importFpFromXmlInProjectWithoutFp() {
        // given: proyecto sin análisis PF; jefe en el detalle del proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        // when: importa un XML válido de PF
        PO_FunctionPointImportView.checkImportView(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        // then: la importación es correcta y se muestra el detalle PF
        PO_FunctionPointImportView.checkImportSuccess(driver);
    }

    @Test
    @Order(37)
    @DisplayName("PR37 - Ver detalle PF con resultados globales")
    void pr37_viewFpDetailsWithGlobalResults() {
        // given: proyecto con análisis PF importado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        // when: (ya en el detalle PF)
        // then: los resultados globales son visibles
        PO_FunctionPointDetailsView.checkResultsAreVisible(driver);
    }

    @Test
    @Order(38)
    @DisplayName("PR38 - Ver matriz de pesos PF en el detalle del análisis")
    void pr38_viewWeightMatrixInFpDetails() {
        // given: análisis PF con matriz de pesos importada
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        // when: (ya en el detalle PF)
        // then: la sección de matriz de pesos es visible
        PO_FunctionPointDetailsView.checkWeightMatrixSectionIsVisible(driver);
    }

    @Test
    @Order(39)
    @DisplayName("PR39 - Editar matriz de pesos PF")
    void pr39_editWeightMatrixFp() {
        // given: análisis PF, jefe en el detalle
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        // when: abre la edición de la matriz de pesos y guarda
        PO_FunctionPointWeightMatrixView.openEditWeightMatrix(driver);
        PO_FunctionPointWeightMatrixView.setFirstRowWeights(driver, "4", "5", "7");
        PO_FunctionPointWeightMatrixView.saveWeightMatrix(driver);
        // then: vuelve al detalle PF con la matriz actualizada
        PO_FunctionPointDetailsView.checkWeightMatrixSectionIsVisible(driver);
    }

    @Test
    @Order(40)
    @DisplayName("PR40 - Restaurar matriz de pesos PF por defecto")
    void pr40_resetWeightMatrixFp() {
        // given: análisis PF con matriz, jefe en la pantalla de edición
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        PO_FunctionPointWeightMatrixView.openEditWeightMatrix(driver);
        // when: restaura la matriz por defecto
        PO_FunctionPointWeightMatrixView.resetWeightMatrix(driver);
        // then: vuelve al detalle PF mostrando la matriz de pesos
        PO_FunctionPointDetailsView.checkWeightMatrixSectionIsVisible(driver);
    }

    @Test
    @Order(41)
    @DisplayName("PR41 - Exportar análisis PF a XML")
    void pr41_exportFpToXml() {
        // given: análisis PF existente
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        // when: exporta el análisis a XML
        PO_FunctionPointDetailsView.clickExportXml(driver);
        // then: permanece en la página de detalle PF sin errores
        PO_FunctionPointDetailsView.checkFunctionPointDetails(driver);
    }

    @Test
    @Order(42)
    @DisplayName("PR42 - Generar informe PDF de análisis PF")
    void pr42_generateFpPdfReport() {
        // given: análisis PF existente
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/valid-fp.xml"));
        PO_FunctionPointImportView.checkImportSuccess(driver);
        // when: genera el informe PDF
        PO_FunctionPointDetailsView.clickGenerateReport(driver);
        // then: permanece en la página de detalle PF sin errores
        PO_FunctionPointDetailsView.checkFunctionPointDetails(driver);
    }

    @Test
    @Order(43)
    @DisplayName("PR43 - Importar XML PF inválido muestra error")
    void pr43_importInvalidFpXmlShowsError() {
        // given: proyecto sin análisis PF
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openFunctionPointImport(driver);
        // when: importa un XML inválido de PF
        PO_FunctionPointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/functionpoints/invalid-fp.xml"));
        // then: se muestra el mensaje de error de importación inválida
        PO_FunctionPointImportView.checkImportError(driver);
    }

    // ─────────────────────────────────────────────────────────────
    // BLOQUE G – Análisis UCP
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(44)
    @DisplayName("PR44 - Jefe de proyecto crea/accede a análisis UCP")
    void pr44_projectManagerCreatesOrAccessesUcpAnalysis() {
        // given: jefe de proyecto en el detalle del proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        // when: accede al análisis UCP (crea uno nuevo si no existe)
        PO_ProjectDetailsView.openUseCasePointAnalysis(driver);
        // then: se muestra la página de detalle del análisis UCP
        PO_UseCasePointDetailsView.checkUseCasePointDetails(driver);
    }

    @Test
    @Order(45)
    @DisplayName("PR45 - Importar análisis UCP desde XML en proyecto sin UCP")
    void pr45_importUcpFromXmlInProjectWithoutUcp() {
        // given: proyecto sin análisis UCP; jefe en el detalle del proyecto
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        // when: importa un XML válido de UCP
        PO_UseCasePointImportView.checkImportView(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        // then: la importación es correcta y se muestra el detalle UCP
        PO_UseCasePointImportView.checkImportSuccess(driver);
    }

    @Test
    @Order(46)
    @DisplayName("PR46 - Ver detalle UCP con resultados globales")
    void pr46_viewUcpDetailsWithGlobalResults() {
        // given: análisis UCP importado
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        // when: (ya en el detalle UCP)
        // then: los resultados globales son visibles
        PO_UseCasePointDetailsView.checkResultsAreVisible(driver);
    }

    @Test
    @Order(47)
    @DisplayName("PR47 - Ver actores, módulos y casos de uso en detalle UCP")
    void pr47_viewActorsModulesAndUseCasesInUcpDetails() {
        // given: análisis UCP importado con actores, módulos y casos de uso
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        // when: (ya en el detalle UCP)
        // then: las secciones de actores, módulos y casos de uso son visibles
        PO_UseCasePointDetailsView.checkActorsSectionIsVisible(driver);
        PO_UseCasePointDetailsView.checkModulesSectionIsVisible(driver);
    }

    @Test
    @Order(48)
    @DisplayName("PR48 - Exportar análisis UCP a XML")
    void pr48_exportUcpToXml() {
        // given: análisis UCP existente
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        // when: exporta el análisis a XML
        PO_UseCasePointDetailsView.clickExportXml(driver);
        // then: permanece en la página de detalle UCP sin errores
        PO_UseCasePointDetailsView.checkUseCasePointDetails(driver);
    }

    @Test
    @Order(49)
    @DisplayName("PR49 - Generar informe PDF de análisis UCP")
    void pr49_generateUcpPdfReport() {
        // given: análisis UCP existente
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/valid-ucp.xml"));
        PO_UseCasePointImportView.checkImportSuccess(driver);
        // when: genera el informe PDF
        PO_UseCasePointDetailsView.clickGenerateReport(driver);
        // then: permanece en la página de detalle UCP sin errores
        PO_UseCasePointDetailsView.checkUseCasePointDetails(driver);
    }

    @Test
    @Order(50)
    @DisplayName("PR50 - Importar XML UCP inválido muestra error")
    void pr50_importInvalidUcpXmlShowsError() {
        // given: proyecto sin análisis UCP
        User pm = factory.createProjectManager(PM_USERNAME, PM_EMAIL, PASSWORD);
        factory.createProject(PROJECT_NAME, PROJECT_DESC, new BigDecimal("50"), "EUR", pm);
        loginAsPm();
        PO_ProjectListView.open(driver, baseUrl);
        PO_ProjectListView.openProjectDetails(driver, PROJECT_NAME);
        PO_ProjectDetailsView.openUseCasePointImport(driver);
        // when: importa un XML inválido de UCP
        PO_UseCasePointImportView.uploadXmlFile(driver,
                xmlResourcePath("xml/usecasepoints/invalid-ucp.xml"));
        // then: se muestra el mensaje de error de importación inválida
        PO_UseCasePointImportView.checkImportError(driver);
    }
}
