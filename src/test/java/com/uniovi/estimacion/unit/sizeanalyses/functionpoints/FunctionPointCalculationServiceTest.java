package com.uniovi.estimacion.unit.sizeanalyses.functionpoints;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Function Point calculation service unit tests")
class FunctionPointCalculationServiceTest {

    /*
     * Objetivo:
     * - Probar la lógica de cálculo de Puntos Función.
     *
     * Casos que generará el agente:
     * - calcula UFP a partir de funciones de datos y transaccionales.
     * - calcula TDI a partir de GSC.
     * - calcula VAF = 0.65 + 0.01 * TDI.
     * - calcula AFP = UFP * VAF.
     * - recalcula pesos usando la matriz de pesos del análisis.
     * - respeta una matriz personalizada de pesos.
     */
}