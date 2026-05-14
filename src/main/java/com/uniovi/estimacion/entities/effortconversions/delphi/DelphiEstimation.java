package com.uniovi.estimacion.entities.effortconversions.delphi;

import com.uniovi.estimacion.common.codes.EffortConversionTechniqueCodes;
import com.uniovi.estimacion.entities.effortconversions.AbstractEffortConversion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delphi_estimations")
@Getter
@Setter
@NoArgsConstructor
public class DelphiEstimation extends AbstractEffortConversion {

    @Column(nullable = false)
    private Long minimumModuleId;

    @Column(nullable = false, length = 150)
    private String minimumModuleNameSnapshot;

    @Column(nullable = false)
    private Double minimumModuleSizeSnapshot;

    @Column(nullable = false)
    private Long maximumModuleId;

    @Column(nullable = false, length = 150)
    private String maximumModuleNameSnapshot;

    @Column(nullable = false)
    private Double maximumModuleSizeSnapshot;

    @Column
    private Double minimumModuleEstimatedEffortHours;

    @Column
    private Double maximumModuleEstimatedEffortHours;

    @Column
    private Double regressionIntercept;

    @Column
    private Double regressionSlope;

    @Column(nullable = false)
    private Double acceptableDeviationPercentage = 10.0;

    @Column(nullable = false)
    private Integer maximumIterations = 2;

    @Column(nullable = false)
    private Integer expertCount = 3;

    @OneToMany(mappedBy = "delphiEstimation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelphiIteration> iterations = new ArrayList<>();

    @Override
    public String getConversionTechniqueCode() {
        return EffortConversionTechniqueCodes.DELPHI;
    }

    @Override
    public boolean isFinished() {
        return regressionIntercept != null && regressionSlope != null;
    }

    public void addIteration(DelphiIteration iteration) {
        iteration.setDelphiEstimation(this);
        this.iterations.add(iteration);
    }

    public void removeIteration(DelphiIteration iteration) {
        this.iterations.remove(iteration);
        iteration.setDelphiEstimation(null);
    }
}