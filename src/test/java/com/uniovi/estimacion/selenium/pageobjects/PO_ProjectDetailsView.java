package com.uniovi.estimacion.selenium.pageobjects;

import org.openqa.selenium.WebDriver;

public class PO_ProjectDetailsView extends PO_NavView {

    public static void checkProjectDetails(WebDriver driver, String projectName) {
        checkMessageIsPresent(driver, "project.details.title");
        checkTextIsPresent(driver, projectName);
    }

    public static void checkProjectManager(WebDriver driver, String username) {
        checkTextIsPresent(driver, username);
    }

    public static void checkEditProjectActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "project.action.edit");
    }

    public static void checkEditProjectActionIsNotVisible(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("project.action.edit"));
    }

    public static void checkManageWorkersActionIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "project.membership.action.manage");
    }

    public static void checkManageWorkersActionIsNotVisible(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("project.membership.action.manage"));
    }

    public static void openFunctionPointAnalysis(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/function-points/access", "fp.details.title");
    }

    public static void openUseCasePointAnalysis(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/use-case-points/access", "ucp.details.title");
    }

    public static void openFunctionPointImport(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/function-points/import", "fp.import.title");
    }

    public static void openUseCasePointImport(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/use-case-points/import", "ucp.import.title");
    }

    public static void checkFunctionPointSizeIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "project.details.fp.size");
    }

    public static void checkUseCasePointSizeIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "project.details.ucp.size");
    }

    public static void checkWorkerCannotEditEstimationData(WebDriver driver) {
        checkTextIsNotPresent(driver, getMessage("fp.details.editAnalysis"));
        checkTextIsNotPresent(driver, getMessage("ucp.details.editAnalysis"));
    }
}