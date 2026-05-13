package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;
import java.util.List;

public class PO_UseCasePointImportView extends PO_NavView {

    public static void checkImportView(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.import.title");
        checkMessageIsPresent(driver, "ucp.import.file");
    }

    public static void uploadXmlFile(WebDriver driver, Path xmlPath) {
        WebElement fileInput =
                SeleniumUtils.waitLoadElementsBy(driver, "free", "//input[@type='file']", getTimeout()).get(0);

        fileInput.sendKeys(xmlPath.toAbsolutePath().toString());

        driver.findElement(By.xpath("//button[@type='submit' and not(ancestor::nav)]")).click();
    }

    public static void checkImportSuccess(WebDriver driver) {
        checkMessageIsPresent(driver, "ucp.details.title");
    }

    public static void checkImportError(WebDriver driver) {
        checkImportView(driver);

        List<WebElement> alerts = driver.findElements(
                By.cssSelector(".alert-danger, .alert-warning, .alert")
        );

        Assertions.assertFalse(alerts.isEmpty(),
                "Se esperaba un mensaje de error tras importar un XML UCP inválido");
    }

    public static void cancelImport(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/projects/", "project.details.title");
    }
}