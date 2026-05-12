package com.uniovi.estimacion.selenium.support;

import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.projects.ProjectMembership;
import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.repositories.effortconversions.delphi.DelphiEstimationRepository;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionConversionRepository;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionRepository;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.projects.ProjectMembershipRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SeleniumTestDataFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstimationProjectRepository estimationProjectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private FunctionPointAnalysisRepository functionPointAnalysisRepository;

    @Autowired
    private UseCasePointAnalysisRepository useCasePointAnalysisRepository;

    @Autowired
    private DelphiEstimationRepository delphiEstimationRepository;

    @Autowired
    private TransformationFunctionConversionRepository transformationFunctionConversionRepository;

    @Autowired
    private TransformationFunctionRepository transformationFunctionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void cleanDatabase() {
        delphiEstimationRepository.deleteAll();
        transformationFunctionConversionRepository.deleteAll();
        transformationFunctionRepository.deleteAll();
        functionPointAnalysisRepository.deleteAll();
        useCasePointAnalysisRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        estimationProjectRepository.deleteAll();
        jdbcTemplate.execute("UPDATE users SET project_manager_id = NULL");
        userRepository.deleteAll();
    }

    public User createProjectManager(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_PROJECT_MANAGER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User createWorker(String username, String email, String password, User manager) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_PROJECT_WORKER);
        user.setEnabled(true);
        user.setProjectManager(manager);
        return userRepository.save(user);
    }

    public EstimationProject createProject(String name, String description,
                                           BigDecimal hourlyRate, String currencyCode,
                                           User owner) {
        EstimationProject project = new EstimationProject();
        project.setName(name);
        project.setDescription(description);
        project.setHourlyRate(hourlyRate);
        project.setCurrencyCode(currencyCode != null ? currencyCode : "EUR");
        project.setOwner(owner);
        return estimationProjectRepository.save(project);
    }

    public ProjectMembership createMembership(EstimationProject project, User worker,
                                              boolean canEdit, boolean canConvert) {
        ProjectMembership membership = new ProjectMembership(project, worker, canEdit, canConvert);
        return projectMembershipRepository.save(membership);
    }
}
