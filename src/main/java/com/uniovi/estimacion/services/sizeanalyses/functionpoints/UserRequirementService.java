package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.UserRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRequirementService {

    private final UserRequirementRepository userRequirementRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointAnalysisService functionPointAnalysisService;

    public List<UserRequirement> findAllByProjectId(Long projectId) {
        List<UserRequirement> requirements =
                userRequirementRepository
                        .findByFunctionPointModuleFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId);

        requirements.forEach(this::initializeRequirementReferences);
        return requirements;
    }

    @Transactional(readOnly = true)
    public List<UserRequirement> findDetailedAllByProjectId(Long projectId) {
        List<UserRequirement> requirements =
                userRequirementRepository
                        .findByFunctionPointModuleFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(projectId);

        requirements.forEach(requirement -> {
            initializeRequirementReferences(requirement);
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return requirements;
    }

    public List<UserRequirement> findAllByModuleId(Long moduleId) {
        List<UserRequirement> requirements =
                userRequirementRepository.findByFunctionPointModuleIdOrderByIdAsc(moduleId);

        requirements.forEach(this::initializeRequirementReferences);
        return requirements;
    }

    @Transactional(readOnly = true)
    public List<UserRequirement> findDetailedAllByModuleId(Long moduleId) {
        List<UserRequirement> requirements =
                userRequirementRepository.findByFunctionPointModuleIdOrderByIdAsc(moduleId);

        requirements.forEach(requirement -> {
            initializeRequirementReferences(requirement);
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return requirements;
    }

    public Optional<UserRequirement> findById(Long requirementId) {
        return userRequirementRepository.findById(requirementId)
                .map(requirement -> {
                    initializeRequirementReferences(requirement);
                    return requirement;
                });
    }

    public Optional<UserRequirement> findByIdAndProjectId(Long requirementId, Long projectId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository
                        .findByIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(
                                requirementId,
                                projectId
                        );

        optionalRequirement.ifPresent(this::initializeRequirementReferences);
        return optionalRequirement;
    }

    public Optional<UserRequirement> findByIdAndModuleId(Long requirementId, Long moduleId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleId(requirementId, moduleId);

        optionalRequirement.ifPresent(this::initializeRequirementReferences);
        return optionalRequirement;
    }

    @Transactional(readOnly = true)
    public Page<UserRequirement> findPageByProjectId(Long projectId, Pageable pageable) {
        Page<UserRequirement> page =
                userRequirementRepository
                        .findByFunctionPointModuleFunctionPointAnalysisEstimationProjectIdOrderByIdAsc(
                                projectId,
                                pageable
                        );

        page.getContent().forEach(this::initializeRequirementReferences);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<UserRequirement> findPageByModuleId(Long moduleId, Pageable pageable) {
        Page<UserRequirement> page =
                userRequirementRepository.findByFunctionPointModuleIdOrderByIdAsc(moduleId, pageable);

        page.getContent().forEach(this::initializeRequirementReferences);
        return page;
    }

    @Transactional(readOnly = true)
    public Optional<UserRequirement> findDetailedByIdAndProjectId(Long requirementId, Long projectId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository
                        .findByIdAndFunctionPointModuleFunctionPointAnalysisEstimationProjectId(
                                requirementId,
                                projectId
                        );

        optionalRequirement.ifPresent(requirement -> {
            initializeRequirementReferences(requirement);
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return optionalRequirement;
    }

    @Transactional(readOnly = true)
    public Optional<UserRequirement> findDetailedByIdAndModuleId(Long requirementId, Long moduleId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleId(requirementId, moduleId);

        optionalRequirement.ifPresent(requirement -> {
            initializeRequirementReferences(requirement);
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return optionalRequirement;
    }

    public boolean hasRequiredData(UserRequirement requirement) {
        return StringUtils.hasText(requirement.getIdentifier())
                && StringUtils.hasText(requirement.getStatement());
    }

    @Transactional
    public UserRequirement createForModule(FunctionPointModule module, UserRequirement requirement) {
        requirement.setIdentifier(normalize(requirement.getIdentifier()));
        requirement.setStatement(normalize(requirement.getStatement()));

        module.addUserRequirement(requirement);

        return userRequirementRepository.save(requirement);
    }

    @Transactional
    public boolean updateBasicData(Long moduleId, Long requirementId, UserRequirement formRequirement) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleId(requirementId, moduleId);

        if (optionalRequirement.isEmpty()) {
            return false;
        }

        UserRequirement existingRequirement = optionalRequirement.get();
        existingRequirement.setIdentifier(normalize(formRequirement.getIdentifier()));
        existingRequirement.setStatement(normalize(formRequirement.getStatement()));

        return true;
    }

    @Transactional
    public boolean deleteByIdWithDerivedFunctions(Long moduleId, Long requirementId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndFunctionPointModuleId(requirementId, moduleId);

        if (optionalRequirement.isEmpty()) {
            return false;
        }

        UserRequirement requirement = optionalRequirement.get();
        FunctionPointAnalysis analysis = requirement.getFunctionPointAnalysis();

        if (analysis != null) {
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
            Hibernate.initialize(analysis.getDataFunctions());
            Hibernate.initialize(analysis.getTransactionalFunctions());

            analysis.getDataFunctions().removeIf(dataFunction ->
                    dataFunction.getUserRequirement() != null
                            && dataFunction.getUserRequirement().getId().equals(requirementId)
            );

            analysis.getTransactionalFunctions().removeIf(transactionalFunction ->
                    transactionalFunction.getUserRequirement() != null
                            && transactionalFunction.getUserRequirement().getId().equals(requirementId)
            );

            functionPointAnalysisService.recalculateAndDeleteDerivedEfforts(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        userRequirementRepository.delete(requirement);
        return true;
    }

    private void initializeRequirementReferences(UserRequirement requirement) {
        if (requirement.getFunctionPointModule() != null) {
            Hibernate.initialize(requirement.getFunctionPointModule());

            if (requirement.getFunctionPointModule().getFunctionPointAnalysis() != null) {
                Hibernate.initialize(requirement.getFunctionPointModule().getFunctionPointAnalysis());
            }
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}