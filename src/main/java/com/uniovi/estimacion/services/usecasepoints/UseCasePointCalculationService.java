package com.uniovi.estimacion.services.usecasepoints;

import com.uniovi.estimacion.entities.usecasepoints.UseCasePointAnalysis;
import com.uniovi.estimacion.entities.usecasepoints.actors.UseCaseActor;
import com.uniovi.estimacion.entities.usecasepoints.actors.UseCaseActorComplexity;
import com.uniovi.estimacion.entities.usecasepoints.factors.EnvironmentalFactorAssessment;
import com.uniovi.estimacion.entities.usecasepoints.factors.TechnicalFactorAssessment;
import com.uniovi.estimacion.entities.usecasepoints.usecases.UseCaseComplexity;
import com.uniovi.estimacion.entities.usecasepoints.usecases.UseCaseEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UseCasePointCalculationService {

    public void recalculateAnalysis(UseCasePointAnalysis analysis) {
        int unadjustedActorWeight = calculateUnadjustedActorWeight(analysis.getActors());
        int unadjustedUseCaseWeight = calculateUnadjustedUseCaseWeight(analysis.getUseCases());
        int unadjustedUseCasePoints = unadjustedActorWeight + unadjustedUseCaseWeight;

        for (UseCaseActor actor : analysis.getActors()) {
            actor.setWeight(calculateActorWeight(actor.getComplexity()));
        }

        for (UseCaseEntry useCase : analysis.getUseCases()) {
            useCase.setWeight(calculateUseCaseWeight(useCase.getComplexity()));
        }

        double technicalFactor = calculateTechnicalFactor(analysis.getTechnicalFactorAssessments());
        double technicalComplexityFactor = calculateTechnicalComplexityFactor(technicalFactor);

        double environmentalFactor = calculateEnvironmentalFactor(analysis.getEnvironmentalFactorAssessments());
        double environmentalComplexityFactor = calculateEnvironmentalComplexityFactor(environmentalFactor);

        double adjustedUseCasePoints = unadjustedUseCasePoints
                * technicalComplexityFactor
                * environmentalComplexityFactor;

        analysis.setUnadjustedActorWeight(unadjustedActorWeight);
        analysis.setUnadjustedUseCaseWeight(unadjustedUseCaseWeight);
        analysis.setUnadjustedUseCasePoints(unadjustedUseCasePoints);
        analysis.setTechnicalFactor(technicalFactor);
        analysis.setTechnicalComplexityFactor(technicalComplexityFactor);
        analysis.setEnvironmentalFactor(environmentalFactor);
        analysis.setEnvironmentalComplexityFactor(environmentalComplexityFactor);
        analysis.setAdjustedUseCasePoints(adjustedUseCasePoints);
    }

    public UseCasePointAnalysisSummary buildSummary(UseCasePointAnalysis analysis) {
        recalculateAnalysis(analysis);

        List<Map<String, Object>> actorBreakdownRows = buildActorBreakdownRows(analysis.getActors());
        List<Map<String, Object>> useCaseBreakdownRows = buildUseCaseBreakdownRows(analysis.getUseCases());

        return new UseCasePointAnalysisSummary(
                analysis.getUnadjustedActorWeight(),
                analysis.getUnadjustedUseCaseWeight(),
                analysis.getUnadjustedUseCasePoints(),
                analysis.getTechnicalFactor(),
                analysis.getTechnicalComplexityFactor(),
                analysis.getEnvironmentalFactor(),
                analysis.getEnvironmentalComplexityFactor(),
                analysis.getAdjustedUseCasePoints(),
                actorBreakdownRows,
                useCaseBreakdownRows,
                sumRows(actorBreakdownRows, "simpleCount"),
                sumRows(actorBreakdownRows, "averageCount"),
                sumRows(actorBreakdownRows, "complexCount"),
                sumRows(actorBreakdownRows, "totalCount"),
                sumRows(actorBreakdownRows, "weightContribution"),
                sumRows(useCaseBreakdownRows, "simpleCount"),
                sumRows(useCaseBreakdownRows, "averageCount"),
                sumRows(useCaseBreakdownRows, "complexCount"),
                sumRows(useCaseBreakdownRows, "totalCount"),
                sumRows(useCaseBreakdownRows, "weightContribution")
        );
    }

    public double calculateAdjustedUseCasePointsForModule(UseCasePointAnalysis analysis,
                                                          List<UseCaseEntry> moduleUseCases) {
        recalculateAnalysis(analysis);

        if (moduleUseCases == null || moduleUseCases.isEmpty()) {
            return 0.0;
        }

        int totalUseCaseWeight = analysis.getUseCases().stream()
                .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                .sum();

        int moduleUseCaseWeight = moduleUseCases.stream()
                .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                .sum();

        if (totalUseCaseWeight <= 0 || moduleUseCaseWeight <= 0) {
            return 0.0;
        }

        double moduleActorWeightShare =
                analysis.getUnadjustedActorWeight()
                        * ((double) moduleUseCaseWeight / totalUseCaseWeight);

        double moduleUnadjustedUseCasePoints =
                moduleActorWeightShare + moduleUseCaseWeight;

        return moduleUnadjustedUseCasePoints
                * analysis.getTechnicalComplexityFactor()
                * analysis.getEnvironmentalComplexityFactor();
    }

    public int calculateActorWeight(UseCaseActorComplexity complexity) {
        return complexity != null ? complexity.getWeight() : 0;
    }

    public int calculateUseCaseWeight(UseCaseComplexity complexity) {
        return complexity != null ? complexity.getWeight() : 0;
    }

    public double calculateTechnicalFactor(List<TechnicalFactorAssessment> assessments) {
        return assessments.stream()
                .mapToDouble(assessment ->
                        assessment.getDegreeOfInfluence() * assessment.getFactorType().getWeight()
                )
                .sum();
    }

    public double calculateTechnicalComplexityFactor(double technicalFactor) {
        return 0.6 + (0.01 * technicalFactor);
    }

    public double calculateEnvironmentalFactor(List<EnvironmentalFactorAssessment> assessments) {
        return assessments.stream()
                .mapToDouble(assessment ->
                        assessment.getDegreeOfInfluence() * assessment.getFactorType().getWeight()
                )
                .sum();
    }

    public double calculateEnvironmentalComplexityFactor(double environmentalFactor) {
        return 1.4 - (0.03 * environmentalFactor);
    }

    public double calculateUnadjustedUseCasePointsForModule(UseCasePointAnalysis analysis,
                                                            List<UseCaseEntry> moduleUseCases) {
        recalculateAnalysis(analysis);

        if (moduleUseCases == null || moduleUseCases.isEmpty()) {
            return 0.0;
        }

        int totalUseCaseWeight = analysis.getUseCases().stream()
                .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                .sum();

        int moduleUseCaseWeight = moduleUseCases.stream()
                .mapToInt(useCase -> useCase.getWeight() != null ? useCase.getWeight() : 0)
                .sum();

        if (totalUseCaseWeight <= 0 || moduleUseCaseWeight <= 0) {
            return 0.0;
        }

        double moduleActorWeightShare =
                analysis.getUnadjustedActorWeight()
                        * ((double) moduleUseCaseWeight / totalUseCaseWeight);

        return moduleActorWeightShare + moduleUseCaseWeight;
    }


    public UseCaseComplexity determineUseCaseComplexity(Integer transactionCount) {
        if (transactionCount == null || transactionCount <= 0) {
            return null;
        }

        if (transactionCount <= 3) {
            return UseCaseComplexity.SIMPLE;
        }

        if (transactionCount <= 7) {
            return UseCaseComplexity.AVERAGE;
        }

        return UseCaseComplexity.COMPLEX;
    }

    public Integer calculateUseCaseWeightFromTransactionCount(Integer transactionCount) {
        UseCaseComplexity complexity = determineUseCaseComplexity(transactionCount);

        if (complexity == null) {
            return null;
        }

        return calculateUseCaseWeight(complexity);
    }

    private int calculateUnadjustedActorWeight(List<UseCaseActor> actors) {
        int total = 0;

        for (UseCaseActor actor : actors) {
            total += calculateActorWeight(actor.getComplexity());
        }

        return total;
    }

    private int calculateUnadjustedUseCaseWeight(List<UseCaseEntry> useCases) {
        int total = 0;

        for (UseCaseEntry useCase : useCases) {
            total += calculateUseCaseWeight(useCase.getComplexity());
        }

        return total;
    }

    private List<Map<String, Object>> buildActorBreakdownRows(List<UseCaseActor> actors) {
        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildActorRow(actors, UseCaseActorComplexity.SIMPLE));
        rows.add(buildActorRow(actors, UseCaseActorComplexity.AVERAGE));
        rows.add(buildActorRow(actors, UseCaseActorComplexity.COMPLEX));

        return rows;
    }

    private Map<String, Object> buildActorRow(List<UseCaseActor> actors,
                                              UseCaseActorComplexity complexity) {
        int count = (int) actors.stream()
                .filter(actor -> actor.getComplexity() == complexity)
                .count();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", complexity.getMessageKey());
        row.put("simpleCount", complexity == UseCaseActorComplexity.SIMPLE ? count : 0);
        row.put("averageCount", complexity == UseCaseActorComplexity.AVERAGE ? count : 0);
        row.put("complexCount", complexity == UseCaseActorComplexity.COMPLEX ? count : 0);
        row.put("totalCount", count);
        row.put("weightContribution", count * calculateActorWeight(complexity));

        return row;
    }

    private List<Map<String, Object>> buildUseCaseBreakdownRows(List<UseCaseEntry> useCases) {
        List<Map<String, Object>> rows = new ArrayList<>();

        rows.add(buildUseCaseRow(useCases, UseCaseComplexity.SIMPLE));
        rows.add(buildUseCaseRow(useCases, UseCaseComplexity.AVERAGE));
        rows.add(buildUseCaseRow(useCases, UseCaseComplexity.COMPLEX));

        return rows;
    }

    private Map<String, Object> buildUseCaseRow(List<UseCaseEntry> useCases,
                                                UseCaseComplexity complexity) {
        int count = (int) useCases.stream()
                .filter(useCase -> useCase.getComplexity() == complexity)
                .count();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("labelKey", complexity.getMessageKey());
        row.put("simpleCount", complexity == UseCaseComplexity.SIMPLE ? count : 0);
        row.put("averageCount", complexity == UseCaseComplexity.AVERAGE ? count : 0);
        row.put("complexCount", complexity == UseCaseComplexity.COMPLEX ? count : 0);
        row.put("totalCount", count);
        row.put("weightContribution", count * calculateUseCaseWeight(complexity));

        return row;
    }

    private int sumRows(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .mapToInt(row -> (Integer) row.get(key))
                .sum();
    }
}