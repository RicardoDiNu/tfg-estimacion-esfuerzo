package com.uniovi.estimacion.services.requirements;

import com.uniovi.estimacion.entities.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.requirements.UserRequirement;
import com.uniovi.estimacion.repositories.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.repositories.UserRequirementRepository;
import com.uniovi.estimacion.services.functionpoints.FunctionPointCalculationService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserRequirementService {

    private final UserRequirementRepository userRequirementRepository;
    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointCalculationService functionPointCalculationService;

    public List<UserRequirement> getByProjectId(Long projectId) {
        return userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<UserRequirement> getDetailedByProjectId(Long projectId) {
        List<UserRequirement> requirements = userRequirementRepository.findByEstimationProjectIdOrderByIdAsc(projectId);

        requirements.forEach(requirement -> {
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return requirements;
    }

    public Optional<UserRequirement> getById(Long id) {
        return userRequirementRepository.findById(id);
    }

    public Optional<UserRequirement> getByIdAndProjectId(Long id, Long projectId) {
        return userRequirementRepository.findByIdAndEstimationProjectId(id, projectId);
    }

    @Transactional(readOnly = true)
    public Optional<UserRequirement> getDetailedByIdAndProjectId(Long id, Long projectId) {
        Optional<UserRequirement> optionalRequirement =
                userRequirementRepository.findByIdAndEstimationProjectId(id, projectId);

        optionalRequirement.ifPresent(requirement -> {
            Hibernate.initialize(requirement.getDataFunctions());
            Hibernate.initialize(requirement.getTransactionalFunctions());
        });

        return optionalRequirement;
    }

    public void save(UserRequirement userRequirement) {
        userRequirementRepository.save(userRequirement);
    }

    @Transactional
    public boolean deleteRequirementAndDerivedFunctions(Long projectId, Long requirementId) {
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

            analysis.getDataFunctions().removeIf(df ->
                    df.getUserRequirement() != null && df.getUserRequirement().getId().equals(requirementId));

            analysis.getTransactionalFunctions().removeIf(tf ->
                    tf.getUserRequirement() != null && tf.getUserRequirement().getId().equals(requirementId));

            functionPointCalculationService.recalculateAnalysis(analysis);
            functionPointAnalysisRepository.save(analysis);
        }

        userRequirementRepository.delete(requirement);
        return true;
    }

    public UserRequirement saveRequirement(UserRequirement requirement) {
        return userRequirementRepository.save(requirement);
    }
}