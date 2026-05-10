package com.uniovi.estimacion.integration.xml.functionpoints;

import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Function Point XML import service integration tests")
class FunctionPointXmlImportServiceIntegrationTest extends AbstractIntegrationTest {

    /*
     * Objetivo:
     * - Importar XML PF válido en proyecto sin PF.
     * - Rechazar XML inválido.
     * - Rechazar XML con referencias rotas.
     * - Rechazar importación si ya existe análisis PF.
     * - Comprobar rollback si falla.
     */
}