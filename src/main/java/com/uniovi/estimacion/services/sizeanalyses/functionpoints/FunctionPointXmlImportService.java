package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.estimacion.entities.projects.EstimationProject;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.FunctionPointAnalysis;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.DataFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.FunctionPointComplexity;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunction;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.functions.TransactionalFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicAssessment;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.gscs.GeneralSystemCharacteristicType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.modules.FunctionPointModule;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.requirements.UserRequirement;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointFunctionType;
import com.uniovi.estimacion.entities.sizeanalyses.functionpoints.weights.FunctionPointWeightMatrixEntry;
import com.uniovi.estimacion.repositories.sizeanalyses.functionpoints.FunctionPointAnalysisRepository;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointAnalysisXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointDataFunctionXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointGscXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointModuleXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointRequirementXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointTransactionalFunctionXmlDto;
import com.uniovi.estimacion.web.dtos.xml.functionpoints.FunctionPointWeightMatrixEntryXmlDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FunctionPointXmlImportService {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final FunctionPointAnalysisRepository functionPointAnalysisRepository;
    private final FunctionPointCalculationService functionPointCalculationService;

    @Transactional
    public void importFromXml(EstimationProject project, byte[] xmlBytes) {
        if (functionPointAnalysisRepository.findByEstimationProjectId(project.getId()).isPresent()) {
            throw new InvalidFunctionPointXmlException("El proyecto ya tiene un análisis PF creado.");
        }

        FunctionPointAnalysisXmlDto dto = parseXml(xmlBytes);
        validateDto(dto);

        FunctionPointAnalysis analysis = buildAnalysis(project, dto);

        functionPointCalculationService.recalculateAnalysis(analysis);
        functionPointAnalysisRepository.save(analysis);
    }

    private FunctionPointAnalysisXmlDto parseXml(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new InvalidFunctionPointXmlException("El archivo XML está vacío.");
        }
        try {
            return XML_MAPPER.readValue(xmlBytes, FunctionPointAnalysisXmlDto.class);
        } catch (IOException e) {
            throw new InvalidFunctionPointXmlException("El XML no puede ser procesado.", e);
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
        for (FunctionPointModuleXmlDto m : modules) {
            if (m.getRef() == null || m.getRef().isBlank()) {
                throw new InvalidFunctionPointXmlException("Módulo sin ref.");
            }
            if (!refs.add(m.getRef())) {
                throw new InvalidFunctionPointXmlException("Ref de módulo duplicada: " + m.getRef());
            }
            if (m.getName() == null || m.getName().isBlank()) {
                throw new InvalidFunctionPointXmlException("Módulo con nombre vacío: " + m.getRef());
            }
        }
    }

    private Set<String> buildModuleRefSet(List<FunctionPointModuleXmlDto> modules) {
        Set<String> refs = new HashSet<>();
        if (modules != null) {
            for (FunctionPointModuleXmlDto m : modules) {
                if (m.getRef() != null) {
                    refs.add(m.getRef());
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
        for (FunctionPointRequirementXmlDto r : requirements) {
            if (r.getRef() == null || r.getRef().isBlank()) {
                throw new InvalidFunctionPointXmlException("Requisito sin ref.");
            }
            if (!refs.add(r.getRef())) {
                throw new InvalidFunctionPointXmlException("Ref de requisito duplicada: " + r.getRef());
            }
            if (r.getModuleRef() == null || !moduleRefs.contains(r.getModuleRef())) {
                throw new InvalidFunctionPointXmlException(
                        "Requisito '" + r.getRef() + "' apunta a módulo inexistente: " + r.getModuleRef());
            }
            if (r.getIdentifier() == null || r.getIdentifier().isBlank()) {
                throw new InvalidFunctionPointXmlException("Requisito sin identifier: " + r.getRef());
            }
            if (r.getStatement() == null || r.getStatement().isBlank()) {
                throw new InvalidFunctionPointXmlException("Requisito sin statement: " + r.getRef());
            }
        }
    }

    private Set<String> buildRequirementRefSet(List<FunctionPointRequirementXmlDto> requirements) {
        Set<String> refs = new HashSet<>();
        if (requirements != null) {
            for (FunctionPointRequirementXmlDto r : requirements) {
                if (r.getRef() != null) {
                    refs.add(r.getRef());
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
        for (FunctionPointDataFunctionXmlDto df : dataFunctions) {
            if (df.getRequirementRef() != null && !requirementRefs.contains(df.getRequirementRef())) {
                throw new InvalidFunctionPointXmlException(
                        "Función de datos apunta a requisito inexistente: " + df.getRequirementRef());
            }
            validateDataFunctionType(df.getType());
            validateComplexity(df.getComplexity());
            if (df.getName() == null || df.getName().isBlank()) {
                throw new InvalidFunctionPointXmlException("Función de datos sin nombre.");
            }
        }
    }

    private void validateTransactionalFunctions(List<FunctionPointTransactionalFunctionXmlDto> transactionalFunctions,
                                                 Set<String> requirementRefs) {
        if (transactionalFunctions == null) {
            return;
        }
        for (FunctionPointTransactionalFunctionXmlDto tf : transactionalFunctions) {
            if (tf.getRequirementRef() != null && !requirementRefs.contains(tf.getRequirementRef())) {
                throw new InvalidFunctionPointXmlException(
                        "Función transaccional apunta a requisito inexistente: " + tf.getRequirementRef());
            }
            validateTransactionalFunctionType(tf.getType());
            validateComplexity(tf.getComplexity());
            if (tf.getName() == null || tf.getName().isBlank()) {
                throw new InvalidFunctionPointXmlException("Función transaccional sin nombre.");
            }
        }
    }

    private void validateDataFunctionType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidFunctionPointXmlException("Tipo de función de datos nulo o vacío.");
        }
        try {
            DataFunctionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidFunctionPointXmlException("Tipo de función de datos inválido: " + type);
        }
    }

    private void validateTransactionalFunctionType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidFunctionPointXmlException("Tipo de función transaccional nulo o vacío.");
        }
        try {
            TransactionalFunctionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidFunctionPointXmlException("Tipo de función transaccional inválido: " + type);
        }
    }

    private void validateComplexity(String complexity) {
        if (complexity == null || complexity.isBlank()) {
            throw new InvalidFunctionPointXmlException("Complejidad nula o vacía.");
        }
        try {
            FunctionPointComplexity.valueOf(complexity);
        } catch (IllegalArgumentException e) {
            throw new InvalidFunctionPointXmlException("Complejidad inválida: " + complexity);
        }
    }

    private void validateWeightMatrix(List<FunctionPointWeightMatrixEntryXmlDto> weightMatrix) {
        if (weightMatrix == null || weightMatrix.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (FunctionPointWeightMatrixEntryXmlDto entry : weightMatrix) {
            if (entry.getFunctionType() == null || entry.getComplexity() == null || entry.getWeight() == null) {
                throw new InvalidFunctionPointXmlException("Entrada de matriz de pesos mal formada.");
            }
            try {
                FunctionPointFunctionType.valueOf(entry.getFunctionType());
            } catch (IllegalArgumentException e) {
                throw new InvalidFunctionPointXmlException(
                        "Tipo de función inválido en matriz de pesos: " + entry.getFunctionType());
            }
            try {
                FunctionPointComplexity.valueOf(entry.getComplexity());
            } catch (IllegalArgumentException e) {
                throw new InvalidFunctionPointXmlException(
                        "Complejidad inválida en matriz de pesos: " + entry.getComplexity());
            }
            if (entry.getWeight() < 1 || entry.getWeight() > 999) {
                throw new InvalidFunctionPointXmlException(
                        "Peso fuera de rango [1..999]: " + entry.getWeight());
            }
            String key = entry.getFunctionType() + "+" + entry.getComplexity();
            if (!seen.add(key)) {
                throw new InvalidFunctionPointXmlException(
                        "Combinación duplicada en matriz de pesos: " + key);
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
                throw new InvalidFunctionPointXmlException("GSC sin tipo.");
            }
            try {
                GeneralSystemCharacteristicType.valueOf(gsc.getType());
            } catch (IllegalArgumentException e) {
                throw new InvalidFunctionPointXmlException("Tipo de GSC inválido: " + gsc.getType());
            }
            if (!seen.add(gsc.getType())) {
                throw new InvalidFunctionPointXmlException("GSC duplicada: " + gsc.getType());
            }
            if (gsc.getDegreeOfInfluence() == null
                    || gsc.getDegreeOfInfluence() < 0
                    || gsc.getDegreeOfInfluence() > 5) {
                throw new InvalidFunctionPointXmlException(
                        "Grado de influencia fuera de rango [0..5] para GSC: " + gsc.getType());
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
                buildRequirements(analysis, moduleMap, dto.getRequirements());

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
                int weight = providedWeights.getOrDefault(key, functionType.getDefaultWeight(complexity));

                FunctionPointWeightMatrixEntry entry = new FunctionPointWeightMatrixEntry();
                entry.setFunctionType(functionType);
                entry.setComplexity(complexity);
                entry.setWeight(weight);
                entry.setDisplayOrder(functionType.getDisplayOrder());
                analysis.addWeightMatrixEntry(entry);
            }
        }
    }

    private void buildGscs(FunctionPointAnalysis analysis, List<FunctionPointGscXmlDto> gscDtos) {
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

            GeneralSystemCharacteristicAssessment assessment = new GeneralSystemCharacteristicAssessment();
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

            String desc = moduleDto.getDescription();
            module.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);
            module.setDisplayOrder(displayOrder++);

            analysis.addModule(module);
            moduleMap.put(moduleDto.getRef(), module);
        }

        return moduleMap;
    }

    private Map<String, UserRequirement> buildRequirements(FunctionPointAnalysis analysis,
                                                            Map<String, FunctionPointModule> moduleMap,
                                                            List<FunctionPointRequirementXmlDto> requirementDtos) {
        Map<String, UserRequirement> requirementMap = new HashMap<>();

        if (requirementDtos == null) {
            return requirementMap;
        }

        for (FunctionPointRequirementXmlDto reqDto : requirementDtos) {
            FunctionPointModule module = moduleMap.get(reqDto.getModuleRef());

            UserRequirement req = new UserRequirement();
            req.setIdentifier(reqDto.getIdentifier().trim());
            req.setStatement(reqDto.getStatement().trim());

            module.addUserRequirement(req);
            requirementMap.put(reqDto.getRef(), req);
        }

        return requirementMap;
    }

    private void buildDataFunctions(FunctionPointAnalysis analysis,
                                     Map<String, UserRequirement> requirementMap,
                                     List<FunctionPointDataFunctionXmlDto> dataFunctionDtos) {
        if (dataFunctionDtos == null) {
            return;
        }

        for (FunctionPointDataFunctionXmlDto dfDto : dataFunctionDtos) {
            DataFunction df = new DataFunction();
            df.setName(dfDto.getName().trim());

            String desc = dfDto.getDescription();
            df.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);

            df.setType(DataFunctionType.valueOf(dfDto.getType()));
            df.setComplexity(FunctionPointComplexity.valueOf(dfDto.getComplexity()));
            df.setWeight(0);
            df.setFunctionPointAnalysis(analysis);

            if (dfDto.getRequirementRef() != null) {
                df.setUserRequirement(requirementMap.get(dfDto.getRequirementRef()));
            }

            analysis.getDataFunctions().add(df);
        }
    }

    private void buildTransactionalFunctions(FunctionPointAnalysis analysis,
                                              Map<String, UserRequirement> requirementMap,
                                              List<FunctionPointTransactionalFunctionXmlDto> transactionalFunctionDtos) {
        if (transactionalFunctionDtos == null) {
            return;
        }

        for (FunctionPointTransactionalFunctionXmlDto tfDto : transactionalFunctionDtos) {
            TransactionalFunction tf = new TransactionalFunction();
            tf.setName(tfDto.getName().trim());

            String desc = tfDto.getDescription();
            tf.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);

            tf.setType(TransactionalFunctionType.valueOf(tfDto.getType()));
            tf.setComplexity(FunctionPointComplexity.valueOf(tfDto.getComplexity()));
            tf.setWeight(0);
            tf.setFunctionPointAnalysis(analysis);

            if (tfDto.getRequirementRef() != null) {
                tf.setUserRequirement(requirementMap.get(tfDto.getRequirementRef()));
            }

            analysis.getTransactionalFunctions().add(tf);
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
}
