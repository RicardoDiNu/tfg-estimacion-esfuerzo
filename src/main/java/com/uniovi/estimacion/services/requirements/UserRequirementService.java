package com.uniovi.estimacion.services.requirements;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.requirements.UserRequirementRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRequirementService {

    private final UserRequirementRepository userRequirementRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointCalculationService functionPointCalculationService;

    public List<UserRequirement> findAllByProjectId(Long projectId) {
        return userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<UserRequirement> findDetailedAllByProjectId(Long projectId) {
        List<UserRequirement> requirements = userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId);

        requirements.forEach(requirement -> {
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return requirements;
    }

    public Optional<UserRequirement> findById(Long requirementId) {
        return userRequirementRepository.findById(requirementId);
    }

    public Optional<UserRequirement> findByIdAndProjectId(Long requirementId, Long projectId) {
        return userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);
    }

    @Transactional(readOnly = true)
    public Page<UserRequirement> findPageByProjectId(Long projectId, Pageable pageable) {
        return userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<UserRequirement> findDetailedByIdAndProjectId(Long requirementId, Long projectId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);

        optionalRequirement.ifPresent(requirement -> {
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return optionalRequirement;
    }

    public boolean hasRequiredData(UserRequirement requirement) {
        return StringUtils.hasText(requirement.getIdentifier())
                && StringUtils.hasText(requirement.getStatement());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    @Transactional
    public UserRequirement createForProject(EstimationProject project, UserRequirement requirement) {
        requirement.setEstimationProject(project);
        requirement.setIdentifier(normalize(requirement.getIdentifier()));
        requirement.setStatement(normalize(requirement.getStatement()));
        return userRequirementRepository.save(requirement);
    }

    @Transactional
    public boolean updateBasicData(Long projectId, Long requirementId, UserRequirement formRequirement) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);

        if (optionalRequirement.isEmpty()) {
            return false;
        }

        UserRequirement existingRequirement = optionalRequirement.get();
        existingRequirement.setIdentifier(normalize(formRequirement.getIdentifier()));
        existingRequirement.setStatement(normalize(formRequirement.getStatement()));

        userRequirementRepository.save(existingRequirement);
        return true;
    }

    @Transactional
    public boolean deleteByIdWithDerivedFunctions(Long projectId, Long requirementId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(requirementId, projectId);
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalRequirement.isEmpty()) {
            return false;
        }

        UserRequirement requirement = optionalRequirement.get();

        if (optionalAnalysis.isPresent()) {
            FunctionPointAnalysis analysis = optionalAnalysis.get();

            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
            Hibernate.initialize(analysis.getDataFunctions());
            Hibernate.initialize(analysis.getTransactionalFunctions());

            analysis.getDataFunctions().removeIf(dataFunction ->
                    dataFunction.getUserRequirement() != null
                            && dataFunction.getUserRequirement().getId().equals(requirementId));

            analysis.getTransactionalFunctions().removeIf(transactionalFunction ->
                    transactionalFunction.getUserRequirement() != null
                            && transactionalFunction.getUserRequirement().getId().equals(requirementId));

            functionPointCalculationService.recalculateAnalysis(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        userRequirementRepository.delete(requirement);
        return true;
    }
}