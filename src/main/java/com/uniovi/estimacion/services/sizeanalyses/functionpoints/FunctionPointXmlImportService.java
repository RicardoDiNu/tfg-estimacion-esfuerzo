package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.*;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FunctionPointXmlImportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointCalculationService functionPointCalculationService;
    private final MessageSource messageSource;

    @Transactional
    public void importFromXml(EstimationProject project, byte[] xmlBytes) {
        if (functionPointAnalysisRepository.findByEstimationProjectId(project.getId()).isPresent()) {
            throw invalidXml("fp.import.validation.existingAnalysis");
        }

        FunctionPointAnalysisXmlDto dto = parseXml(xmlBytes);
        validateDto(dto);

        FunctionPointAnalysis analysis = buildAnalysis(project, dto);

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
    }

    private FunctionPointAnalysisXmlDto parseXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw invalidXml("fp.import.validation.emptyFile");
        }

        try {
            FunctionPointAnalysisXmlDto dto =
                    XML_MAPPER.readValue(xmlBytes, FunctionPointAnalysisXmlDto.class);

            if (dto == null) {
                throw invalidXml("fp.import.validation.emptyRoot");
            }

            return dto;
        } catch (IOException exception) {
            throw invalidXml("fp.import.validation.parseError", exception);
        }
    }

    private void validateDto(FunctionPointAnalysisXmlDto dto) {
        validateModules(dto.getModules());
        validateRequirements(dto.getRequirements(), buildModuleRefSet(dto.getModules()));
        Set<String> requirementRefs = buildRequirementRefSet(dto.getRequirements());
        validateDataFunctions(dto.getDataFunctions(), requirementRefs);
        validateTransactionalFunctions(dto.getTransactionalFunctions(), requirementRefs);
        validateWeightMatrix(dto.getWeightMatrix());
        validateGscs(dto.getGscs());
    }

    private void validateModules(List<FunctionPointModuleXmlDto> modules) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        Set<String> refs = new HashSet<>();

        for (FunctionPointModuleXmlDto module : modules) {
            if (module.getRef() == null || module.getRef().isBlank()) {
                throw invalidXml("fp.import.validation.module.missingRef");
            }

            if (!refs.add(module.getRef())) {
                throw invalidXml("fp.import.validation.module.duplicateRef", module.getRef());
            }

            if (module.getName() == null || module.getName().isBlank()) {
                throw invalidXml("fp.import.validation.module.emptyName", module.getRef());
            }
        }
    }

    private Set<String> buildModuleRefSet(List<FunctionPointModuleXmlDto> modules) {
        Set<String> refs = new HashSet<>();

        if (modules != null) {
            for (FunctionPointModuleXmlDto module : modules) {
                if (module.getRef() != null) {
                    refs.add(module.getRef());
                }
            }
        }

        return refs;
    }

    private void validateRequirements(List<FunctionPointRequirementXmlDto> requirements,
                                      Set<String> moduleRefs) {
        if (requirements == null || requirements.isEmpty()) {
            return;
        }

        Set<String> refs = new HashSet<>();

        for (FunctionPointRequirementXmlDto requirement : requirements) {
            if (requirement.getRef() == null || requirement.getRef().isBlank()) {
                throw invalidXml("fp.import.validation.requirement.missingRef");
            }

            if (!refs.add(requirement.getRef())) {
                throw invalidXml("fp.import.validation.requirement.duplicateRef", requirement.getRef());
            }

            if (requirement.getModuleRef() == null
                    || requirement.getModuleRef().isBlank()
                    || !moduleRefs.contains(requirement.getModuleRef())) {
                throw invalidXml(
                        "fp.import.validation.requirement.invalidModuleRef",
                        requirement.getRef(),
                        requirement.getModuleRef()
                );
            }

            if (requirement.getIdentifier() == null || requirement.getIdentifier().isBlank()) {
                throw invalidXml(
                        "fp.import.validation.requirement.missingIdentifier",
                        requirement.getRef()
                );
            }

            if (requirement.getStatement() == null || requirement.getStatement().isBlank()) {
                throw invalidXml(
                        "fp.import.validation.requirement.missingStatement",
                        requirement.getRef()
                );
            }
        }
    }

    private Set<String> buildRequirementRefSet(List<FunctionPointRequirementXmlDto> requirements) {
        Set<String> refs = new HashSet<>();

        if (requirements != null) {
            for (FunctionPointRequirementXmlDto requirement : requirements) {
                if (requirement.getRef() != null) {
                    refs.add(requirement.getRef());
                }
            }
        }

        return refs;
    }

    private void validateDataFunctions(List<FunctionPointDataFunctionXmlDto> dataFunctions,
                                       Set<String> requirementRefs) {
        if (dataFunctions == null) {
            return;
        }

        for (FunctionPointDataFunctionXmlDto dataFunction : dataFunctions) {
            if (dataFunction.getRequirementRef() == null
                    || dataFunction.getRequirementRef().isBlank()) {
                throw invalidXml("fp.import.validation.dataFunction.missingRequirementRef");
            }

            if (!requirementRefs.contains(dataFunction.getRequirementRef())) {
                throw invalidXml(
                        "fp.import.validation.dataFunction.invalidRequirementRef",
                        dataFunction.getRequirementRef()
                );
            }

            validateDataFunctionType(dataFunction.getType());
            validateComplexity(dataFunction.getComplexity());

            if (dataFunction.getName() == null || dataFunction.getName().isBlank()) {
                throw invalidXml("fp.import.validation.dataFunction.missingName");
            }
        }
    }

    private void validateTransactionalFunctions(List<FunctionPointTransactionalFunctionXmlDto> transactionalFunctions,
                                                Set<String> requirementRefs) {
        if (transactionalFunctions == null) {
            return;
        }

        for (FunctionPointTransactionalFunctionXmlDto transactionalFunction : transactionalFunctions) {
            if (transactionalFunction.getRequirementRef() == null
                    || transactionalFunction.getRequirementRef().isBlank()) {
                throw invalidXml("fp.import.validation.transactionalFunction.missingRequirementRef");
            }

            if (!requirementRefs.contains(transactionalFunction.getRequirementRef())) {
                throw invalidXml(
                        "fp.import.validation.transactionalFunction.invalidRequirementRef",
                        transactionalFunction.getRequirementRef()
                );
            }

            validateTransactionalFunctionType(transactionalFunction.getType());
            validateComplexity(transactionalFunction.getComplexity());

            if (transactionalFunction.getName() == null || transactionalFunction.getName().isBlank()) {
                throw invalidXml("fp.import.validation.transactionalFunction.missingName");
            }
        }
    }

    private void validateDataFunctionType(String type) {
        if (type == null || type.isBlank()) {
            throw invalidXml("fp.import.validation.dataFunctionType.empty");
        }

        try {
            DataFunctionType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw invalidXml("fp.import.validation.dataFunctionType.invalid", type);
        }
    }

    private void validateTransactionalFunctionType(String type) {
        if (type == null || type.isBlank()) {
            throw invalidXml("fp.import.validation.transactionalFunctionType.empty");
        }

        try {
            TransactionalFunctionType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw invalidXml("fp.import.validation.transactionalFunctionType.invalid", type);
        }
    }

    private void validateComplexity(String complexity) {
        if (complexity == null || complexity.isBlank()) {
            throw invalidXml("fp.import.validation.complexity.empty");
        }

        try {
            FunctionPointComplexity.valueOf(complexity);
        } catch (IllegalArgumentException exception) {
            throw invalidXml("fp.import.validation.complexity.invalid", complexity);
        }
    }

    private void validateWeightMatrix(List<FunctionPointWeightMatrixEntryXmlDto> weightMatrix) {
        if (weightMatrix == null || weightMatrix.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (FunctionPointWeightMatrixEntryXmlDto entry : weightMatrix) {
            if (entry.getFunctionType() == null
                    || entry.getComplexity() == null
                    || entry.getWeight() == null) {
                throw invalidXml("fp.import.validation.weightMatrix.malformedEntry");
            }

            try {
                FunctionPointFunctionType.valueOf(entry.getFunctionType());
            } catch (IllegalArgumentException exception) {
                throw invalidXml(
                        "fp.import.validation.weightMatrix.invalidFunctionType",
                        entry.getFunctionType()
                );
            }

            try {
                FunctionPointComplexity.valueOf(entry.getComplexity());
            } catch (IllegalArgumentException exception) {
                throw invalidXml(
                        "fp.import.validation.weightMatrix.invalidComplexity",
                        entry.getComplexity()
                );
            }

            if (entry.getWeight() < 1 || entry.getWeight() > 999) {
                throw invalidXml(
                        "fp.import.validation.weightMatrix.weightOutOfRange",
                        entry.getWeight()
                );
            }

            String key = entry.getFunctionType() + "+" + entry.getComplexity();

            if (!seen.add(key)) {
                throw invalidXml(
                        "fp.import.validation.weightMatrix.duplicateCombination",
                        key
                );
            }
        }
    }

    private void validateGscs(List<FunctionPointGscXmlDto> gscs) {
        if (gscs == null || gscs.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (FunctionPointGscXmlDto gsc : gscs) {
            if (gsc.getType() == null || gsc.getType().isBlank()) {
                throw invalidXml("fp.import.validation.gsc.missingType");
            }

            try {
                GeneralSystemCharacteristicType.valueOf(gsc.getType());
            } catch (IllegalArgumentException exception) {
                throw invalidXml("fp.import.validation.gsc.invalidType", gsc.getType());
            }

            if (!seen.add(gsc.getType())) {
                throw invalidXml("fp.import.validation.gsc.duplicate", gsc.getType());
            }

            if (gsc.getDegreeOfInfluence() == null
                    || gsc.getDegreeOfInfluence() < 0
                    || gsc.getDegreeOfInfluence() > 5) {
                throw invalidXml(
                        "fp.import.validation.gsc.degreeOutOfRange",
                        gsc.getType()
                );
            }
        }
    }

    private FunctionPointAnalysis buildAnalysis(EstimationProject project,
                                                FunctionPointAnalysisXmlDto dto) {
        String boundary = dto.getSystemBoundaryDescription();

        if (boundary != null) {
            boundary = boundary.trim();
        }

        if (boundary == null || boundary.isBlank()) {
            boundary = "-";
        }

        FunctionPointAnalysis analysis = new FunctionPointAnalysis(project, boundary);

        buildWeightMatrix(analysis, dto.getWeightMatrix());
        buildGscs(analysis, dto.getGscs());

        Map<String, FunctionPointModule> moduleMap = buildModules(analysis, dto.getModules());
        Map<String, UserRequirement> requirementMap =
                buildRequirements(moduleMap, dto.getRequirements());

        buildDataFunctions(analysis, requirementMap, dto.getDataFunctions());
        buildTransactionalFunctions(analysis, requirementMap, dto.getTransactionalFunctions());

        return analysis;
    }

    private void buildWeightMatrix(FunctionPointAnalysis analysis,
                                   List<FunctionPointWeightMatrixEntryXmlDto> weightMatrixDtos) {
        Map<String, Integer> providedWeights = new HashMap<>();

        if (weightMatrixDtos != null) {
            for (FunctionPointWeightMatrixEntryXmlDto entryDto : weightMatrixDtos) {
                String key = entryDto.getFunctionType() + "+" + entryDto.getComplexity();
                providedWeights.put(key, entryDto.getWeight());
            }
        }

        for (FunctionPointFunctionType functionType : getOrderedFunctionTypes()) {
            for (FunctionPointComplexity complexity : FunctionPointComplexity.values()) {
                String key = functionType.name() + "+" + complexity.name();
                int weight = providedWeights.getOrDefault(
                        key,
                        functionType.getDefaultWeight(complexity)
                );

                FunctionPointWeightMatrixEntry entry = new FunctionPointWeightMatrixEntry();
                entry.setFunctionType(functionType);
                entry.setComplexity(complexity);
                entry.setWeight(weight);
                entry.setDisplayOrder(functionType.getDisplayOrder());
                analysis.addWeightMatrixEntry(entry);
            }
        }
    }

    private void buildGscs(FunctionPointAnalysis analysis,
                           List<FunctionPointGscXmlDto> gscDtos) {
        Map<GeneralSystemCharacteristicType, Integer> providedDegrees = new HashMap<>();

        if (gscDtos != null) {
            for (FunctionPointGscXmlDto gscDto : gscDtos) {
                GeneralSystemCharacteristicType type =
                        GeneralSystemCharacteristicType.valueOf(gscDto.getType());
                providedDegrees.put(type, gscDto.getDegreeOfInfluence());
            }
        }

        for (GeneralSystemCharacteristicType type : GeneralSystemCharacteristicType.values()) {
            int degree = providedDegrees.getOrDefault(type, 0);

            GeneralSystemCharacteristicAssessment assessment =
                    new GeneralSystemCharacteristicAssessment();
            assessment.setFunctionPointAnalysis(analysis);
            assessment.setCharacteristicType(type);
            assessment.setDegreeOfInfluence(degree);
            analysis.getGeneralSystemCharacteristicAssessments().add(assessment);
        }
    }

    private Map<String, FunctionPointModule> buildModules(FunctionPointAnalysis analysis,
                                                          List<FunctionPointModuleXmlDto> moduleDtos) {
        Map<String, FunctionPointModule> moduleMap = new HashMap<>();

        if (moduleDtos == null) {
            return moduleMap;
        }

        int displayOrder = 1;

        for (FunctionPointModuleXmlDto moduleDto : moduleDtos) {
            FunctionPointModule module = new FunctionPointModule();
            module.setName(moduleDto.getName().trim());

            String description = moduleDto.getDescription();
            module.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            module.setDisplayOrder(displayOrder++);

            analysis.addModule(module);
            moduleMap.put(moduleDto.getRef(), module);
        }

        return moduleMap;
    }

    private Map<String, UserRequirement> buildRequirements(Map<String, FunctionPointModule> moduleMap,
                                                           List<FunctionPointRequirementXmlDto> requirementDtos) {
        Map<String, UserRequirement> requirementMap = new HashMap<>();

        if (requirementDtos == null) {
            return requirementMap;
        }

        for (FunctionPointRequirementXmlDto requirementDto : requirementDtos) {
            FunctionPointModule module = moduleMap.get(requirementDto.getModuleRef());

            UserRequirement requirement = new UserRequirement();
            requirement.setIdentifier(requirementDto.getIdentifier().trim());
            requirement.setStatement(requirementDto.getStatement().trim());

            module.addUserRequirement(requirement);
            requirementMap.put(requirementDto.getRef(), requirement);
        }

        return requirementMap;
    }

    private void buildDataFunctions(FunctionPointAnalysis analysis,
                                    Map<String, UserRequirement> requirementMap,
                                    List<FunctionPointDataFunctionXmlDto> dataFunctionDtos) {
        if (dataFunctionDtos == null) {
            return;
        }

        for (FunctionPointDataFunctionXmlDto dataFunctionDto : dataFunctionDtos) {
            DataFunction dataFunction = new DataFunction();
            dataFunction.setName(dataFunctionDto.getName().trim());

            String description = dataFunctionDto.getDescription();
            dataFunction.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            dataFunction.setType(DataFunctionType.valueOf(dataFunctionDto.getType()));
            dataFunction.setComplexity(FunctionPointComplexity.valueOf(dataFunctionDto.getComplexity()));
            dataFunction.setWeight(0);
            dataFunction.setFunctionPointAnalysis(analysis);
            dataFunction.setUserRequirement(requirementMap.get(dataFunctionDto.getRequirementRef()));

            analysis.getDataFunctions().add(dataFunction);
        }
    }

    private void buildTransactionalFunctions(FunctionPointAnalysis analysis,
                                             Map<String, UserRequirement> requirementMap,
                                             List<FunctionPointTransactionalFunctionXmlDto> transactionalFunctionDtos) {
        if (transactionalFunctionDtos == null) {
            return;
        }

        for (FunctionPointTransactionalFunctionXmlDto transactionalFunctionDto : transactionalFunctionDtos) {
            TransactionalFunction transactionalFunction = new TransactionalFunction();
            transactionalFunction.setName(transactionalFunctionDto.getName().trim());

            String description = transactionalFunctionDto.getDescription();
            transactionalFunction.setDescription(
                    description != null && !description.isBlank()
                            ? description.trim()
                            : null
            );

            transactionalFunction.setType(
                    TransactionalFunctionType.valueOf(transactionalFunctionDto.getType())
            );
            transactionalFunction.setComplexity(
                    FunctionPointComplexity.valueOf(transactionalFunctionDto.getComplexity())
            );
            transactionalFunction.setWeight(0);
            transactionalFunction.setFunctionPointAnalysis(analysis);
            transactionalFunction.setUserRequirement(
                    requirementMap.get(transactionalFunctionDto.getRequirementRef())
            );

            analysis.getTransactionalFunctions().add(transactionalFunction);
        }
    }

    private List<FunctionPointFunctionType> getOrderedFunctionTypes() {
        return List.of(
                FunctionPointFunctionType.EI,
                FunctionPointFunctionType.EO,
                FunctionPointFunctionType.EQ,
                FunctionPointFunctionType.ILF,
                FunctionPointFunctionType.EIF
        );
    }

    private InvalidFunctionPointXmlException invalidXml(String messageKey, Object... args) {
        return new InvalidFunctionPointXmlException(resolveMessage(messageKey, args));
    }

    private InvalidFunctionPointXmlException invalidXml(String messageKey, Throwable cause, Object... args) {
        return new InvalidFunctionPointXmlException(resolveMessage(messageKey, args), cause);
    }

    private String resolveMessage(String messageKey, Object... args) {
        return messageSource.getMessage(
                messageKey,
                args,
                LocaleContextHolder.getLocale()
        );
    }
}