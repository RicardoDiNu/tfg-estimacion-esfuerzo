package com.uniovi.estimacion.integration.xml.functionpoints;

import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Function Point XML export service integration tests")
class FunctionPointXmlExportServiceIntegrationTest extends AbstractIntegrationTest {

    /*
     * Objetivo:
     * - Exportar análisis PF existente.
     * - Comprobar que el XML no contiene IDs reales.
     * - Comprobar que contiene refs internas.
     * - Comprobar que exportar + reimportar conserva datos principales.
     */
}