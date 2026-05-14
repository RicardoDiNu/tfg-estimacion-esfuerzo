package com.uniovi.estimacion.selenium.pageobjects;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PO_UseCasePointDetailsView extends PO_NavView {

    public static void checkUseCasePointDetails(WebDriver driver) {
        boolean validUrl = driver.getCurrentUrl().contains("/use-case-points");

        boolean validContent =
                !driver.findElements(By.xpath("//*[contains(text(),'UCP')]")).isEmpty()
                        || !driver.findElements(By.xpath("//*[contains(text(),'Puntos de Casos de Uso')]")).isEmpty()
                        || !driver.findElements(By.xpath("//*[contains(text(),'Puntos de casos de uso')]")).isEmpty()
                        || !driver.findElements(By.xpath("//*[contains(text(),'Casos de uso')]")).isEmpty()
                        || !driver.findElements(By.xpath("//*[contains(text(),'Actores')]")).isEmpty()
                        || !driver.findElements(By.xpath("//*[contains(text(),'Factores técnicos')]")).isEmpty();

        Assertions.assertTrue(validUrl || validContent,
                "No parece haberse cargado la pantalla de detalle UCP. URL actual: " + driver.getCurrentUrl());
    }

    public static void checkResultsAreVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.results.title");
        checkMessageIsPresent(driver, "ucp.field.uaw");
        checkMessageIsPresent(driver, "ucp.field.uucw");
        checkMessageIsPresent(driver, "ucp.field.tcf");
        checkMessageIsPresent(driver, "ucp.field.ecf");
    }

    public static void checkActorsSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.actors.title");
    }

    public static void checkModulesSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.modules.title");
    }

    public static void checkUseCasesSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.useCases.title");
    }

    public static void checkTechnicalFactorsSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.technicalFactors.title");
    }

    public static void checkEnvironmentalFactorsSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.environmentalFactors.title");
    }

    public static void clickExportXml(WebDriver driver) {
        checkElementBy(driver, "@href", "/use-case-points/export/xml").get(0).click();
        checkUseCasePointDetails(driver);
    }

    public static void clickGenerateReport(WebDriver driver) {
        checkElementBy(driver, "@href", "/use-case-points/report/pdf").get(0).click();
        checkUseCasePointDetails(driver);
    }

    public static void checkEditAnalysisActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.details.editAnalysis");
    }

    public static void checkEditAnalysisActionIsNotVisible(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("ucp.details.editAnalysis"));
    }

    public static void checkManageEffortConversionsActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "project.details.ucp.delphi.title");
        checkMessageIsPresent(driver, "project.details.ucp.transformation.title");
    }
}