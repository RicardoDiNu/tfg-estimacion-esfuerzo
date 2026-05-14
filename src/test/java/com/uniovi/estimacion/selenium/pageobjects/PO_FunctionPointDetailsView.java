package com.uniovi.estimacion.selenium.pageobjects;

import org.openqa.selenium.WebDriver;

public class PO_FunctionPointDetailsView extends PO_NavView {

    public static void checkFunctionPointDetails(WebDriver driver) {
        checkTextIsPresent(driver, "Puntos Función");
    }

    public static void checkResultsAreVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.results.title");
        checkMessageIsPresent(driver, "fp.field.ufp");
        checkMessageIsPresent(driver, "fp.field.afp");
        checkMessageIsPresent(driver, "fp.field.tdi");
        checkMessageIsPresent(driver, "fp.field.vaf");
    }

    public static void checkModulesSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.modules.title");
    }

    public static void checkGscSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.gsc.list.title");
    }

    public static void checkWeightMatrixSectionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.weights.details.title");
    }

    public static void clickExportXml(WebDriver driver) {
        checkElementBy(driver, "@href", "/function-points/export/xml").get(0).click();
        checkFunctionPointDetails(driver);
    }

    public static void clickGenerateReport(WebDriver driver) {
        checkElementBy(driver, "@href", "/function-points/report/pdf").get(0).click();
        checkFunctionPointDetails(driver);
    }

    public static void checkEditAnalysisActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.details.editAnalysis");
    }

    public static void checkEditAnalysisActionIsNotVisible(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("fp.details.editAnalysis"));
    }

    public static void checkEditWeightMatrixActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.weights.edit.button");
    }

    public static void checkEditWeightMatrixActionIsNotVisible(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("fp.weights.edit.button"));
    }

    public static void checkManageEffortConversionsActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.delphi.title");
        checkMessageIsPresent(driver, "fp.transformation.title");
    }
}