package com.uniovi.estimacion.services.sizeanalyses.usecasepoints;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.modules.UseCasePointModule;
import com.uniovi.estimacion.entities.sizeanalyses.usecasepoints.usecases.UseCaseEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.usecasepoints.UseCasePointAnalysisRepository;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseActorXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseEntryXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseEnvironmentalFactorXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCasePointAnalysisXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCasePointModuleXmlDto;
import com.uniovi.estimacion.web.dtos.xml.usecasepoints.UseCaseTechnicalFactorXmlDto;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UseCasePointXmlExportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final UseCasePointAnalysisRepository useCasePointAnalysisRepository;

    public Optional<byte[]> exportToXml(Long projectId) {
        Optional<UseCasePointAnalysis> optionalAnalysis =
                useCasePointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        UseCasePointAnalysis analysis = optionalAnalysis.get();

        Hibernate.initialize(analysis.getActors());
        Hibernate.initialize(analysis.getModules());
        Hibernate.initialize(analysis.getTechnicalFactorAssessments());
        Hibernate.initialize(analysis.getEnvironmentalFactorAssessments());

        for (UseCasePointModule module : analysis.getModules()) {
            Hibernate.initialize(module.getUseCases());
            for (UseCaseEntry uc : module.getUseCases()) {
                Hibernate.initialize(uc.getActors());
            }
        }

        UseCasePointAnalysisXmlDto dto = buildDto(analysis);

        try {
            byte[] bytes = XML_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(dto);
            return Optional.of(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error al serializar el análisis UCP a XML", e);
        }
    }

    private UseCasePointAnalysisXmlDto buildDto(UseCasePointAnalysis analysis) {
        UseCasePointAnalysisXmlDto dto = new UseCasePointAnalysisXmlDto();
        dto.setVersion("1.0");
        dto.setSystemBoundaryDescription(analysis.getSystemBoundaryDescription());

        Map<Long, String> actorRefMap = new LinkedHashMap<>();
        dto.setActors(buildActorsDto(analysis.getActors(), actorRefMap));

        Map<Long, String> moduleRefMap = new LinkedHashMap<>();
        dto.setModules(buildModulesDto(analysis.getModules(), moduleRefMap));
        dto.setUseCases(buildUseCasesDto(analysis.getModules(), moduleRefMap, actorRefMap));
        dto.setTechnicalFactors(buildTechnicalFactorsDto(analysis.getTechnicalFactorAssessments()));
        dto.setEnvironmentalFactors(buildEnvironmentalFactorsDto(analysis.getEnvironmentalFactorAssessments()));

        return dto;
    }

    private List<UseCaseActorXmlDto> buildActorsDto(List<UseCaseActor> actors,
                                                     Map<Long, String> actorRefMap) {
        List<UseCaseActorXmlDto> dtos = new ArrayList<>();
        int index = 1;

        for (UseCaseActor actor : actors) {
            String ref = "A" + index++;
            actorRefMap.put(actor.getId(), ref);

            UseCaseActorXmlDto dto = new UseCaseActorXmlDto();
            dto.setRef(ref);
            dto.setComplexity(actor.getComplexity().name());
            dto.setName(actor.getName());
            dto.setDescription(actor.getDescription());
            dtos.add(dto);
        }

        return dtos;
    }

    private List<UseCasePointModuleXmlDto> buildModulesDto(List<UseCasePointModule> modules,
                                                            Map<Long, String> moduleRefMap) {
        List<UseCasePointModuleXmlDto> dtos = new ArrayList<>();
        int index = 1;

        for (UseCasePointModule module : modules) {
            String ref = "M" + index++;
            moduleRefMap.put(module.getId(), ref);

            UseCasePointModuleXmlDto dto = new UseCasePointModuleXmlDto();
            dto.setRef(ref);
            dto.setName(module.getName());
            dto.setDescription(module.getDescription());
            dtos.add(dto);
        }

        return dtos;
    }

    private List<UseCaseEntryXmlDto> buildUseCasesDto(List<UseCasePointModule> modules,
                                                       Map<Long, String> moduleRefMap,
                                                       Map<Long, String> actorRefMap) {
        List<UseCaseEntryXmlDto> dtos = new ArrayList<>();
        int index = 1;

        for (UseCasePointModule module : modules) {
            String moduleRef = moduleRefMap.get(module.getId());

            for (UseCaseEntry uc : module.getUseCases()) {
                String ref = "UC" + index++;

                UseCaseEntryXmlDto dto = new UseCaseEntryXmlDto();
                dto.setRef(ref);
                dto.setModuleRef(moduleRef);
                dto.setTransactionCount(uc.getTransactionCount());
                dto.setName(uc.getName());
                dto.setDescription(uc.getDescription());
                dto.setTriggerCondition(uc.getTriggerCondition());
                dto.setPreconditions(uc.getPreconditions());
                dto.setPostconditions(uc.getPostconditions());
                dto.setNormalFlow(uc.getNormalFlow());
                dto.setAlternativeFlows(uc.getAlternativeFlows());
                dto.setExceptionFlows(uc.getExceptionFlows());

                List<String> actorRefs = new ArrayList<>();
                for (UseCaseActor actor : uc.getActors()) {
                    String actorRef = actorRefMap.get(actor.getId());
                    if (actorRef != null) {
                        actorRefs.add(actorRef);
                    }
                }
                if (!actorRefs.isEmpty()) {
                    dto.setActorRefs(actorRefs);
                }

                dtos.add(dto);
            }
        }

        return dtos;
    }

    private List<UseCaseTechnicalFactorXmlDto> buildTechnicalFactorsDto(
            List<TechnicalFactorAssessment> assessments) {
        List<UseCaseTechnicalFactorXmlDto> dtos = new ArrayList<>();

        for (TechnicalFactorAssessment assessment : assessments) {
            UseCaseTechnicalFactorXmlDto dto = new UseCaseTechnicalFactorXmlDto();
            dto.setType(assessment.getFactorType().name());
            dto.setDegreeOfInfluence(assessment.getDegreeOfInfluence());
            dtos.add(dto);
        }

        return dtos;
    }

    private List<UseCaseEnvironmentalFactorXmlDto> buildEnvironmentalFactorsDto(
            List<EnvironmentalFactorAssessment> assessments) {
        List<UseCaseEnvironmentalFactorXmlDto> dtos = new ArrayList<>();

        for (EnvironmentalFactorAssessment assessment : assessments) {
            UseCaseEnvironmentalFactorXmlDto dto = new UseCaseEnvironmentalFactorXmlDto();
            dto.setType(assessment.getFactorType().name());
            dto.setDegreeOfInfluence(assessment.getDegreeOfInfluence());
            dtos.add(dto);
        }

        return dtos;
    }
}
