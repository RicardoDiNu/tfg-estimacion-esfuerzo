package com.uniovi.estimacion.services.sizeanalyses.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorType;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UseCasePointXmlImportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final UseCasePointAnalysisRepository useCasePointAnalysisRepository;
    private final UseCasePointCalculationService useCasePointCalculationService;
    private final MessageSource messageSource;

    @Transactional
    public void importFromXml(EstimationProject project, byte[] xmlBytes) {
        if (useCasePointAnalysisRepository.findByEstimationProjectId(project.getId()).isPresent()) {
            throw invalidXml("ucp.import.validation.existingAnalysis");
        }

        UseCasePointAnalysisXmlDto dto = parseXml(xmlBytes);
        validateDto(dto);

        UseCasePointAnalysis analysis = buildAnalysis(project, dto);

        useCasePointCalculationService.recalculateAnalysis(analysis);
        useCasePointAnalysisRepository.save(analysis);
    }

    private UseCasePointAnalysisXmlDto parseXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw invalidXml("ucp.import.validation.emptyFile");
        }

        try {
            UseCasePointAnalysisXmlDto dto =
                    XML_MAPPER.readValue(xmlBytes, UseCasePointAnalysisXmlDto.class);

            if (dto == null) {
                throw invalidXml("ucp.import.validation.emptyRoot");
            }

            return dto;
        } catch (IOException exception) {
            throw invalidXml("ucp.import.validation.parseError", exception);
        }
    }

    private void validateDto(UseCasePointAnalysisXmlDto dto) {
        Set<String> actorRefs = validateActors(dto.getActors());
        validateModules(dto.getModules());
        Set<String> moduleRefs = buildModuleRefSet(dto.getModules());
        validateUseCases(dto.getUseCases(), moduleRefs, actorRefs);
        validateTechnicalFactors(dto.getTechnicalFactors());
        validateEnvironmentalFactors(dto.getEnvironmentalFactors());
    }

    private Set<String> validateActors(List<UseCaseActorXmlDto> actors) {
        Set<String> refs = new HashSet<>();

        if (actors == null) {
            return refs;
        }

        for (UseCaseActorXmlDto actor : actors) {
            if (actor.getRef() == null || actor.getRef().isBlank()) {
                throw invalidXml("ucp.import.validation.actor.missingRef");
            }

            if (!refs.add(actor.getRef())) {
                throw invalidXml("ucp.import.validation.actor.duplicateRef", actor.getRef());
            }

            if (actor.getName() == null || actor.getName().isBlank()) {
                throw invalidXml("ucp.import.validation.actor.missingName", actor.getRef());
            }

            if (actor.getComplexity() == null || actor.getComplexity().isBlank()) {
                throw invalidXml("ucp.import.validation.actor.missingComplexity", actor.getRef());
            }

            try {
                UseCaseActorComplexity.valueOf(actor.getComplexity());
            } catch (IllegalArgumentException exception) {
                throw invalidXml(
                        "ucp.import.validation.actor.invalidComplexity",
                        actor.getComplexity()
                );
            }
        }

        return refs;
    }

    private void validateModules(List<UseCasePointModuleXmlDto> modules) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        Set<String> refs = new HashSet<>();

        for (UseCasePointModuleXmlDto module : modules) {
            if (module.getRef() == null || module.getRef().isBlank()) {
                throw invalidXml("ucp.import.validation.module.missingRef");
            }

            if (!refs.add(module.getRef())) {
                throw invalidXml("ucp.import.validation.module.duplicateRef", module.getRef());
            }

            if (module.getName() == null || module.getName().isBlank()) {
                throw invalidXml("ucp.import.validation.module.missingName", module.getRef());
            }
        }
    }

    private Set<String> buildModuleRefSet(List<UseCasePointModuleXmlDto> modules) {
        Set<String> refs = new HashSet<>();

        if (modules != null) {
            for (UseCasePointModuleXmlDto module : modules) {
                if (module.getRef() != null) {
                    refs.add(module.getRef());
                }
            }
        }

        return refs;
    }

    private void validateUseCases(List<UseCaseEntryXmlDto> useCases,
                                  Set<String> moduleRefs,
                                  Set<String> actorRefs) {
        if (useCases == null) {
            return;
        }

        Set<String> refs = new HashSet<>();

        for (UseCaseEntryXmlDto useCase : useCases) {
            if (useCase.getRef() == null || useCase.getRef().isBlank()) {
                throw invalidXml("ucp.import.validation.useCase.missingRef");
            }

            if (!refs.add(useCase.getRef())) {
                throw invalidXml("ucp.import.validation.useCase.duplicateRef", useCase.getRef());
            }

            if (useCase.getName() == null || useCase.getName().isBlank()) {
                throw invalidXml("ucp.import.validation.useCase.missingName", useCase.getRef());
            }

            if (useCase.getModuleRef() == null
                    || useCase.getModuleRef().isBlank()
                    || !moduleRefs.contains(useCase.getModuleRef())) {
                throw invalidXml(
                        "ucp.import.validation.useCase.invalidModuleRef",
                        useCase.getRef(),
                        useCase.getModuleRef()
                );
            }

            if (useCase.getTransactionCount() == null || useCase.getTransactionCount() <= 0) {
                throw invalidXml(
                        "ucp.import.validation.useCase.invalidTransactionCount",
                        useCase.getRef()
                );
            }

            if (useCase.getActorRefs() != null) {
                Set<String> seenActorRefsInUseCase = new HashSet<>();

                for (String actorRef : useCase.getActorRefs()) {
                    if (actorRef == null || actorRef.isBlank()) {
                        throw invalidXml(
                                "ucp.import.validation.useCase.emptyActorRef",
                                useCase.getRef()
                        );
                    }

                    if (!seenActorRefsInUseCase.add(actorRef)) {
                        throw invalidXml(
                                "ucp.import.validation.useCase.duplicateActorRef",
                                useCase.getRef(),
                                actorRef
                        );
                    }

                    if (!actorRefs.contains(actorRef)) {
                        throw invalidXml(
                                "ucp.import.validation.useCase.invalidActorRef",
                                useCase.getRef(),
                                actorRef
                        );
                    }
                }
            }
        }
    }

    private void validateTechnicalFactors(List<UseCaseTechnicalFactorXmlDto> factors) {
        if (factors == null || factors.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (UseCaseTechnicalFactorXmlDto factor : factors) {
            if (factor.getType() == null || factor.getType().isBlank()) {
                throw invalidXml("ucp.import.validation.technicalFactor.missingType");
            }

            try {
                TechnicalFactorType.valueOf(factor.getType());
            } catch (IllegalArgumentException exception) {
                throw invalidXml(
                        "ucp.import.validation.technicalFactor.invalidType",
                        factor.getType()
                );
            }

            if (!seen.add(factor.getType())) {
                throw invalidXml(
                        "ucp.import.validation.technicalFactor.duplicate",
                        factor.getType()
                );
            }

            if (factor.getDegreeOfInfluence() == null
                    || factor.getDegreeOfInfluence() < 0
                    || factor.getDegreeOfInfluence() > 5) {
                throw invalidXml(
                        "ucp.import.validation.technicalFactor.degreeOutOfRange",
                        factor.getType()
                );
            }
        }
    }

    private void validateEnvironmentalFactors(List<UseCaseEnvironmentalFactorXmlDto> factors) {
        if (factors == null || factors.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (UseCaseEnvironmentalFactorXmlDto factor : factors) {
            if (factor.getType() == null || factor.getType().isBlank()) {
                throw invalidXml("ucp.import.validation.environmentalFactor.missingType");
            }

            try {
                EnvironmentalFactorType.valueOf(factor.getType());
            } catch (IllegalArgumentException exception) {
                throw invalidXml(
                        "ucp.import.validation.environmentalFactor.invalidType",
                        factor.getType()
                );
            }

            if (!seen.add(factor.getType())) {
                throw invalidXml(
                        "ucp.import.validation.environmentalFactor.duplicate",
                        factor.getType()
                );
            }

            if (factor.getDegreeOfInfluence() == null
                    || factor.getDegreeOfInfluence() < 0
                    || factor.getDegreeOfInfluence() > 5) {
                throw invalidXml(
                        "ucp.import.validation.environmentalFactor.degreeOutOfRange",
                        factor.getType()
                );
            }
        }
    }

    private UseCasePointAnalysis buildAnalysis(EstimationProject project,
                                               UseCasePointAnalysisXmlDto dto) {
        String boundary = dto.getSystemBoundaryDescription();

        if (boundary != null) {
            boundary = boundary.trim();
        }

        if (boundary == null || boundary.isBlank()) {
            boundary = "-";
        }

        UseCasePointAnalysis analysis = new UseCasePointAnalysis(project, boundary);

        Map<String, UseCaseActor> actorMap = buildActors(analysis, dto.getActors());
        Map<String, UseCasePointModule> moduleMap = buildModules(analysis, dto.getModules());

        buildUseCases(analysis, moduleMap, actorMap, dto.getUseCases());
        buildTechnicalFactors(analysis, dto.getTechnicalFactors());
        buildEnvironmentalFactors(analysis, dto.getEnvironmentalFactors());

        return analysis;
    }

    private Map<String, UseCaseActor> buildActors(UseCasePointAnalysis analysis,
                                                  List<UseCaseActorXmlDto> actorDtos) {
        Map<String, UseCaseActor> actorMap = new HashMap<>();

        if (actorDtos == null) {
            return actorMap;
        }

        for (UseCaseActorXmlDto actorDto : actorDtos) {
            UseCaseActor actor = new UseCaseActor();
            actor.setName(actorDto.getName().trim());

            String description = actorDto.getDescription();
            actor.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            actor.setComplexity(UseCaseActorComplexity.valueOf(actorDto.getComplexity()));
            actor.setWeight(0);
            actor.setUseCasePointAnalysis(analysis);

            analysis.getActors().add(actor);
            actorMap.put(actorDto.getRef(), actor);
        }

        return actorMap;
    }

    private Map<String, UseCasePointModule> buildModules(UseCasePointAnalysis analysis,
                                                         List<UseCasePointModuleXmlDto> moduleDtos) {
        Map<String, UseCasePointModule> moduleMap = new HashMap<>();

        if (moduleDtos == null) {
            return moduleMap;
        }

        for (UseCasePointModuleXmlDto moduleDto : moduleDtos) {
            UseCasePointModule module = new UseCasePointModule();
            module.setName(moduleDto.getName().trim());

            String description = moduleDto.getDescription();
            module.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            module.setUseCasePointAnalysis(analysis);
            analysis.getModules().add(module);
            moduleMap.put(moduleDto.getRef(), module);
        }

        return moduleMap;
    }

    private void buildUseCases(UseCasePointAnalysis analysis,
                               Map<String, UseCasePointModule> moduleMap,
                               Map<String, UseCaseActor> actorMap,
                               List<UseCaseEntryXmlDto> useCaseDtos) {
        if (useCaseDtos == null) {
            return;
        }

        for (UseCaseEntryXmlDto useCaseDto : useCaseDtos) {
            UseCaseEntry useCase = new UseCaseEntry();
            useCase.setName(useCaseDto.getName().trim());

            String description = useCaseDto.getDescription();
            useCase.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            setNullableText(useCase, useCaseDto);

            useCase.setTransactionCount(useCaseDto.getTransactionCount());
            useCase.setComplexity(deriveComplexity(useCaseDto.getTransactionCount()));
            useCase.setWeight(0);

            UseCasePointModule module = moduleMap.get(useCaseDto.getModuleRef());
            useCase.setUseCasePointModule(module);
            useCase.setUseCasePointAnalysis(analysis);

            module.getUseCases().add(useCase);
            analysis.getUseCases().add(useCase);

            if (useCaseDto.getActorRefs() != null) {
                for (String actorRef : useCaseDto.getActorRefs()) {
                    UseCaseActor actor = actorMap.get(actorRef);

                    if (actor != null) {
                        useCase.addActor(actor);
                    }
                }
            }
        }
    }

    private void setNullableText(UseCaseEntry useCase, UseCaseEntryXmlDto useCaseDto) {
        String trigger = useCaseDto.getTriggerCondition();
        useCase.setTriggerCondition(trigger != null && !trigger.isBlank() ? trigger.trim() : null);

        String preconditions = useCaseDto.getPreconditions();
        useCase.setPreconditions(
                preconditions != null && !preconditions.isBlank()
                        ? preconditions.trim()
                        : null
        );

        String postconditions = useCaseDto.getPostconditions();
        useCase.setPostconditions(
                postconditions != null && !postconditions.isBlank()
                        ? postconditions.trim()
                        : null
        );

        String normalFlow = useCaseDto.getNormalFlow();
        useCase.setNormalFlow(
                normalFlow != null && !normalFlow.isBlank()
                        ? normalFlow.trim()
                        : null
        );

        String alternativeFlows = useCaseDto.getAlternativeFlows();
        useCase.setAlternativeFlows(
                alternativeFlows != null && !alternativeFlows.isBlank()
                        ? alternativeFlows.trim()
                        : null
        );

        String exceptionFlows = useCaseDto.getExceptionFlows();
        useCase.setExceptionFlows(
                exceptionFlows != null && !exceptionFlows.isBlank()
                        ? exceptionFlows.trim()
                        : null
        );
    }

    private UseCaseComplexity deriveComplexity(Integer transactionCount) {
        if (transactionCount <= 3) {
            return UseCaseComplexity.SIMPLE;
        }

        if (transactionCount <= 7) {
            return UseCaseComplexity.AVERAGE;
        }

        return UseCaseComplexity.COMPLEX;
    }

    private void buildTechnicalFactors(UseCasePointAnalysis analysis,
                                       List<UseCaseTechnicalFactorXmlDto> factorDtos) {
        Map<TechnicalFactorType, Integer> providedDegrees = new HashMap<>();

        if (factorDtos != null) {
            for (UseCaseTechnicalFactorXmlDto factorDto : factorDtos) {
                TechnicalFactorType type = TechnicalFactorType.valueOf(factorDto.getType());
                providedDegrees.put(type, factorDto.getDegreeOfInfluence());
            }
        }

        for (TechnicalFactorType type : TechnicalFactorType.values()) {
            int degree = providedDegrees.getOrDefault(type, 0);

            TechnicalFactorAssessment assessment = new TechnicalFactorAssessment();
            assessment.setUseCasePointAnalysis(analysis);
            assessment.setFactorType(type);
            assessment.setDegreeOfInfluence(degree);
            analysis.getTechnicalFactorAssessments().add(assessment);
        }
    }

    private void buildEnvironmentalFactors(UseCasePointAnalysis analysis,
                                           List<UseCaseEnvironmentalFactorXmlDto> factorDtos) {
        Map<EnvironmentalFactorType, Integer> providedDegrees = new HashMap<>();

        if (factorDtos != null) {
            for (UseCaseEnvironmentalFactorXmlDto factorDto : factorDtos) {
                EnvironmentalFactorType type = EnvironmentalFactorType.valueOf(factorDto.getType());
                providedDegrees.put(type, factorDto.getDegreeOfInfluence());
            }
        }

        for (EnvironmentalFactorType type : EnvironmentalFactorType.values()) {
            int degree = providedDegrees.getOrDefault(type, 0);

            EnvironmentalFactorAssessment assessment = new EnvironmentalFactorAssessment();
            assessment.setUseCasePointAnalysis(analysis);
            assessment.setFactorType(type);
            assessment.setDegreeOfInfluence(degree);
            analysis.getEnvironmentalFactorAssessments().add(assessment);
        }
    }

    private InvalidUseCasePointXmlException invalidXml(String messageKey, Object... args) {
        return new InvalidUseCasePointXmlException(resolveMessage(messageKey, args));
    }

    private InvalidUseCasePointXmlException invalidXml(String messageKey,
                                                       Throwable cause,
                                                       Object... args) {
        return new InvalidUseCasePointXmlException(resolveMessage(messageKey, args), cause);
    }

    private String resolveMessage(String messageKey, Object... args) {
        return messageSource.getMessage(
                messageKey,
                args,
                LocaleContextHolder.getLocale()
        );
    }
}