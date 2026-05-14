package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunctionPointXmlExportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointModuleService functionPointModuleService;

    public Optional<byte[]> exportToXml(Long projectId) {
        Optional<FunctionPointAnalysis> optionalAnalysis =
                functionPointAnalysisRepository.findByEstimationProjectId(projectId);

        if (optionalAnalysis.isEmpty()) {
            return Optional.empty();
        }

        FunctionPointAnalysis analysis = optionalAnalysis.get();

        Hibernate.initialize(analysis.getGeneralSystemCharacteristicAssessments());
        Hibernate.initialize(analysis.getWeightMatrixEntries());
        Hibernate.initialize(analysis.getDataFunctions());
        Hibernate.initialize(analysis.getTransactionalFunctions());

        for (DataFunction df : analysis.getDataFunctions()) {
            if (df.getUserRequirement() != null) {
                Hibernate.initialize(df.getUserRequirement());
            }
        }
        for (TransactionalFunction tf : analysis.getTransactionalFunctions()) {
            if (tf.getUserRequirement() != null) {
                Hibernate.initialize(tf.getUserRequirement());
            }
        }

        List<FunctionPointModule> modules = functionPointModuleService.findAllByProjectId(projectId);

        FunctionPointAnalysisXmlDto dto = buildDto(analysis, modules);

        try {
            byte[] bytes = XML_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(dto);
            return Optional.of(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error al serializar el análisis PF a XML", e);
        }
    }

    private FunctionPointAnalysisXmlDto buildDto(FunctionPointAnalysis analysis,
                                                 List<FunctionPointModule> modules) {
        FunctionPointAnalysisXmlDto dto = new FunctionPointAnalysisXmlDto();
        dto.setVersion("1.0");
        dto.setSystemBoundaryDescription(analysis.getSystemBoundaryDescription());
        dto.setWeightMatrix(buildWeightMatrixDto(analysis));
        dto.setGscs(buildGscsDto(analysis));

        Map<Long, String> moduleRefMap = new LinkedHashMap<>();
        Map<Long, String> requirementRefMap = new LinkedHashMap<>();

        dto.setModules(buildModulesDto(modules, moduleRefMap));
        dto.setRequirements(buildRequirementsDto(modules, moduleRefMap, requirementRefMap));
        dto.setDataFunctions(buildDataFunctionsDto(analysis.getDataFunctions(), requirementRefMap));
        dto.setTransactionalFunctions(
                buildTransactionalFunctionsDto(analysis.getTransactionalFunctions(), requirementRefMap));

        return dto;
    }

    private List<FunctionPointWeightMatrixEntryXmlDto> buildWeightMatrixDto(FunctionPointAnalysis analysis) {
        List<FunctionPointWeightMatrixEntryXmlDto> entries = new ArrayList<>();

        for (FunctionPointWeightMatrixEntry entry : analysis.getWeightMatrixEntries()) {
            FunctionPointWeightMatrixEntryXmlDto entryDto = new FunctionPointWeightMatrixEntryXmlDto();
            entryDto.setFunctionType(entry.getFunctionType().name());
            entryDto.setComplexity(entry.getComplexity().name());
            entryDto.setWeight(entry.getWeight());
            entries.add(entryDto);
        }

        return entries;
    }

    private List<FunctionPointGscXmlDto> buildGscsDto(FunctionPointAnalysis analysis) {
        List<FunctionPointGscXmlDto> gscs = new ArrayList<>();

        for (GeneralSystemCharacteristicAssessment gsc : analysis.getGeneralSystemCharacteristicAssessments()) {
            FunctionPointGscXmlDto gscDto = new FunctionPointGscXmlDto();

            gscDto.setType(gsc.getCharacteristicType().name());
            gscDto.setDegreeOfInfluence(gsc.getDegreeOfInfluence());
            gscDto.setCustomText(normalizeNullableText(gsc.getCustomText()));

            gscs.add(gscDto);
        }

        return gscs;
    }

    private List<FunctionPointModuleXmlDto> buildModulesDto(List<FunctionPointModule> modules,
                                                            Map<Long, String> moduleRefMap) {
        List<FunctionPointModuleXmlDto> moduleDtos = new ArrayList<>();
        int index = 1;

        for (FunctionPointModule module : modules) {
            String ref = "M" + index++;
            moduleRefMap.put(module.getId(), ref);

            FunctionPointModuleXmlDto moduleDto = new FunctionPointModuleXmlDto();
            moduleDto.setRef(ref);
            moduleDto.setName(module.getName());
            moduleDto.setDescription(module.getDescription());
            moduleDtos.add(moduleDto);
        }

        return moduleDtos;
    }

    private List<FunctionPointRequirementXmlDto> buildRequirementsDto(List<FunctionPointModule> modules,
                                                                      Map<Long, String> moduleRefMap,
                                                                      Map<Long, String> requirementRefMap) {
        List<FunctionPointRequirementXmlDto> requirementDtos = new ArrayList<>();
        int index = 1;

        for (FunctionPointModule module : modules) {
            String moduleRef = moduleRefMap.get(module.getId());

            for (UserRequirement req : module.getUserRequirements()) {
                String ref = "RU" + index++;
                requirementRefMap.put(req.getId(), ref);

                FunctionPointRequirementXmlDto reqDto = new FunctionPointRequirementXmlDto();
                reqDto.setRef(ref);
                reqDto.setModuleRef(moduleRef);
                reqDto.setIdentifier(req.getIdentifier());
                reqDto.setStatement(req.getStatement());
                requirementDtos.add(reqDto);
            }
        }

        return requirementDtos;
    }

    private List<FunctionPointDataFunctionXmlDto> buildDataFunctionsDto(
            List<DataFunction> dataFunctions,
            Map<Long, String> requirementRefMap) {

        List<FunctionPointDataFunctionXmlDto> dtos = new ArrayList<>();

        for (DataFunction df : dataFunctions) {
            FunctionPointDataFunctionXmlDto dto = new FunctionPointDataFunctionXmlDto();
            dto.setName(df.getName());
            dto.setDescription(df.getDescription());
            dto.setType(df.getType().name());
            dto.setComplexity(df.getComplexity().name());

            if (df.getUserRequirement() != null) {
                dto.setRequirementRef(requirementRefMap.get(df.getUserRequirement().getId()));
            }

            dtos.add(dto);
        }

        return dtos;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<FunctionPointTransactionalFunctionXmlDto> buildTransactionalFunctionsDto(
            List<TransactionalFunction> transactionalFunctions,
            Map<Long, String> requirementRefMap) {

        List<FunctionPointTransactionalFunctionXmlDto> dtos = new ArrayList<>();

        for (TransactionalFunction tf : transactionalFunctions) {
            FunctionPointTransactionalFunctionXmlDto dto = new FunctionPointTransactionalFunctionXmlDto();
            dto.setName(tf.getName());
            dto.setDescription(tf.getDescription());
            dto.setType(tf.getType().name());
            dto.setComplexity(tf.getComplexity().name());

            if (tf.getUserRequirement() != null) {
                dto.setRequirementRef(requirementRefMap.get(tf.getUserRequirement().getId()));
            }

            dtos.add(dto);
        }

        return dtos;
    }
}
