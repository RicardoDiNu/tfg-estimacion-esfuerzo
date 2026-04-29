package com.uniovi.estimacion.services.effortconversions;

public record LinearEffortModel(double intercept, double slope) {

    public double estimate(Double size) {
        if (size == null || size < 0) {
            throw new IllegalArgumentException("El tamaño debe ser un valor no negativo.");
        }

        return intercept + (slope * size);
    }

    public static LinearEffortModel fromTwoPoints(
            double firstSize,
            double firstEffort,
            double secondSize,
            double secondEffort
    ) {
        if (Double.compare(firstSize, secondSize) == 0) {
            throw new IllegalArgumentException("Los dos tamaños no pueden coincidir.");
        }

        double slope = (secondEffort - firstEffort) / (secondSize - firstSize);
        double intercept = firstEffort - (slope * firstSize);

        return new LinearEffortModel(intercept, slope);
    }
}