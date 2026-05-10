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
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseActorXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseEntryXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseEnvironmentalFactorXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCasePointAnalysisXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCasePointModuleXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseTechnicalFactorXmlDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UseCasePointXmlImportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final UseCasePointAnalysisRepository useCasePointAnalysisRepository;
    private final UseCasePointCalculationService useCasePointCalculationService;

    @Transactional
    public void importFromXml(EstimationProject project, byte[] xmlBytes) {
        if (useCasePointAnalysisRepository.findByEstimationProjectId(project.getId()).isPresent()) {
            throw new InvalidUseCasePointXmlException("El proyecto ya tiene un análisis UCP creado.");
        }

        UseCasePointAnalysisXmlDto dto = parseXml(xmlBytes);
        validateDto(dto);

        UseCasePointAnalysis analysis = buildAnalysis(project, dto);

        useCasePointCalculationService.recalculateAnalysis(analysis);
        useCasePointAnalysisRepository.save(analysis);
    }

    private UseCasePointAnalysisXmlDto parseXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new InvalidUseCasePointXmlException("El archivo XML está vacío.");
        }
        try {
            return XML_MAPPER.readValue(xmlBytes, UseCasePointAnalysisXmlDto.class);
        } catch (IOException e) {
            throw new InvalidUseCasePointXmlException("El XML no puede ser procesado.", e);
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
                throw new InvalidUseCasePointXmlException("Actor sin ref.");
            }
            if (!refs.add(actor.getRef())) {
                throw new InvalidUseCasePointXmlException("Ref de actor duplicada: " + actor.getRef());
            }
            if (actor.getName() == null || actor.getName().isBlank()) {
                throw new InvalidUseCasePointXmlException("Actor sin nombre: " + actor.getRef());
            }
            if (actor.getComplexity() == null || actor.getComplexity().isBlank()) {
                throw new InvalidUseCasePointXmlException("Actor sin complejidad: " + actor.getRef());
            }
            try {
                UseCaseActorComplexity.valueOf(actor.getComplexity());
            } catch (IllegalArgumentException e) {
                throw new InvalidUseCasePointXmlException(
                        "Complejidad de actor inválida: " + actor.getComplexity());
            }
        }
        return refs;
    }

    private void validateModules(List<UseCasePointModuleXmlDto> modules) {
        if (modules == null || modules.isEmpty()) {
            return;
        }
        Set<String> refs = new HashSet<>();
        for (UseCasePointModuleXmlDto m : modules) {
            if (m.getRef() == null || m.getRef().isBlank()) {
                throw new InvalidUseCasePointXmlException("Módulo sin ref.");
            }
            if (!refs.add(m.getRef())) {
                throw new InvalidUseCasePointXmlException("Ref de módulo duplicada: " + m.getRef());
            }
            if (m.getName() == null || m.getName().isBlank()) {
                throw new InvalidUseCasePointXmlException("Módulo sin nombre: " + m.getRef());
            }
        }
    }

    private Set<String> buildModuleRefSet(List<UseCasePointModuleXmlDto> modules) {
        Set<String> refs = new HashSet<>();
        if (modules != null) {
            for (UseCasePointModuleXmlDto m : modules) {
                if (m.getRef() != null) {
                    refs.add(m.getRef());
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
        for (UseCaseEntryXmlDto uc : useCases) {
            if (uc.getRef() == null || uc.getRef().isBlank()) {
                throw new InvalidUseCasePointXmlException("Caso de uso sin ref.");
            }
            if (!refs.add(uc.getRef())) {
                throw new InvalidUseCasePointXmlException("Ref de caso de uso duplicada: " + uc.getRef());
            }
            if (uc.getName() == null || uc.getName().isBlank()) {
                throw new InvalidUseCasePointXmlException("Caso de uso sin nombre: " + uc.getRef());
            }
            if (uc.getModuleRef() == null || !moduleRefs.contains(uc.getModuleRef())) {
                throw new InvalidUseCasePointXmlException(
                        "Caso de uso '" + uc.getRef() + "' apunta a módulo inexistente: " + uc.getModuleRef());
            }
            if (uc.getTransactionCount() != null && uc.getTransactionCount() < 0) {
                throw new InvalidUseCasePointXmlException(
                        "Caso de uso '" + uc.getRef() + "' tiene transactionCount negativo.");
            }
            if (uc.getActorRefs() != null) {
                for (String actorRef : uc.getActorRefs()) {
                    if (!actorRefs.contains(actorRef)) {
                        throw new InvalidUseCasePointXmlException(
                                "Caso de uso '" + uc.getRef() + "' apunta a actor inexistente: " + actorRef);
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
                throw new InvalidUseCasePointXmlException("Factor técnico sin tipo.");
            }
            try {
                TechnicalFactorType.valueOf(factor.getType());
            } catch (IllegalArgumentException e) {
                throw new InvalidUseCasePointXmlException(
                        "Tipo de factor técnico inválido: " + factor.getType());
            }
            if (!seen.add(factor.getType())) {
                throw new InvalidUseCasePointXmlException("Factor técnico duplicado: " + factor.getType());
            }
            if (factor.getDegreeOfInfluence() == null
                    || factor.getDegreeOfInfluence() < 0
                    || factor.getDegreeOfInfluence() > 5) {
                throw new InvalidUseCasePointXmlException(
                        "Grado de influencia fuera de rango [0..5] para factor técnico: " + factor.getType());
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
                throw new InvalidUseCasePointXmlException("Factor ambiental sin tipo.");
            }
            try {
                EnvironmentalFactorType.valueOf(factor.getType());
            } catch (IllegalArgumentException e) {
                throw new InvalidUseCasePointXmlException(
                        "Tipo de factor ambiental inválido: " + factor.getType());
            }
            if (!seen.add(factor.getType())) {
                throw new InvalidUseCasePointXmlException("Factor ambiental duplicado: " + factor.getType());
            }
            if (factor.getDegreeOfInfluence() == null
                    || factor.getDegreeOfInfluence() < 0
                    || factor.getDegreeOfInfluence() > 5) {
                throw new InvalidUseCasePointXmlException(
                        "Grado de influencia fuera de rango [0..5] para factor ambiental: " + factor.getType());
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

            String desc = actorDto.getDescription();
            actor.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);

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

            String desc = moduleDto.getDescription();
            module.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);

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

        for (UseCaseEntryXmlDto ucDto : useCaseDtos) {
            UseCaseEntry uc = new UseCaseEntry();
            uc.setName(ucDto.getName().trim());

            String desc = ucDto.getDescription();
            uc.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);

            setNullableText(uc, ucDto);

            uc.setTransactionCount(ucDto.getTransactionCount());
            uc.setComplexity(deriveComplexity(ucDto.getTransactionCount()));
            uc.setWeight(0);

            UseCasePointModule module = moduleMap.get(ucDto.getModuleRef());
            uc.setUseCasePointModule(module);
            uc.setUseCasePointAnalysis(analysis);
            module.getUseCases().add(uc);
            analysis.getUseCases().add(uc);

            if (ucDto.getActorRefs() != null) {
                for (String actorRef : ucDto.getActorRefs()) {
                    UseCaseActor actor = actorMap.get(actorRef);
                    if (actor != null) {
                        uc.addActor(actor);
                    }
                }
            }
        }
    }

    private void setNullableText(UseCaseEntry uc, UseCaseEntryXmlDto ucDto) {
        String trigger = ucDto.getTriggerCondition();
        uc.setTriggerCondition(trigger != null && !trigger.isBlank() ? trigger.trim() : null);

        String pre = ucDto.getPreconditions();
        uc.setPreconditions(pre != null && !pre.isBlank() ? pre.trim() : null);

        String post = ucDto.getPostconditions();
        uc.setPostconditions(post != null && !post.isBlank() ? post.trim() : null);

        String normal = ucDto.getNormalFlow();
        uc.setNormalFlow(normal != null && !normal.isBlank() ? normal.trim() : null);

        String alt = ucDto.getAlternativeFlows();
        uc.setAlternativeFlows(alt != null && !alt.isBlank() ? alt.trim() : null);

        String exc = ucDto.getExceptionFlows();
        uc.setExceptionFlows(exc != null && !exc.isBlank() ? exc.trim() : null);
    }

    private UseCaseComplexity deriveComplexity(Integer transactionCount) {
        if (transactionCount == null || transactionCount <= 3) {
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
            for (UseCaseTechnicalFactorXmlDto dto : factorDtos) {
                TechnicalFactorType type = TechnicalFactorType.valueOf(dto.getType());
                providedDegrees.put(type, dto.getDegreeOfInfluence());
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
            for (UseCaseEnvironmentalFactorXmlDto dto : factorDtos) {
                EnvironmentalFactorType type = EnvironmentalFactorType.valueOf(dto.getType());
                providedDegrees.put(type, dto.getDegreeOfInfluence());
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
}
