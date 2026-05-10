package com.uniovi.estimacion.integration.xml.usecasepoints;

import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Use Case Point XML export service integration tests")
class UseCasePointXmlExportServiceIntegrationTest extends AbstractIntegrationTest {

    /*
     * Objetivo:
     * - Exportar análisis UCP existente.
     * - Comprobar que el XML no contiene IDs reales.
     * - Comprobar refs internas de actores, módulos y casos de uso.
     * - Comprobar que exportar + reimportar conserva datos principales.
     */
}