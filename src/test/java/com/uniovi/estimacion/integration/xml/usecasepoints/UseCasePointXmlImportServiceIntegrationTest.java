package com.uniovi.estimacion.integration.xml.usecasepoints;

import com.uniovi.estimacion.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Use Case Point XML import service integration tests")
class UseCasePointXmlImportServiceIntegrationTest extends AbstractIntegrationTest {

    /*
     * Objetivo:
     * - Importar XML UCP válido en proyecto sin UCP.
     * - Rechazar actorRef inexistente.
     * - Rechazar moduleRef inexistente.
     * - Rechazar transactionCount inválido.
     * - Rechazar factor técnico/ambiental inválido.
     * - Rechazar importación si ya existe análisis UCP.
     */
}