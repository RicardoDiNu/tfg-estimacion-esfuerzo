package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;

public class PO_FunctionPointImportView extends PO_NavView {

    public static void checkImportView(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.import.title");
        checkMessageIsPresent(driver, "fp.import.file");
    }

    public static void uploadXmlFile(WebDriver driver, Path xmlPath) {
        WebElement fileInput =
                SeleniumUtils.waitLoadElementsBy(driver, "free", "//input[@type='file']", getTimeout()).get(0);

        fileInput.sendKeys(xmlPath.toAbsolutePath().toString());

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public static void checkImportSuccess(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.details.title");
    }

    public static void checkImportError(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.import.error.invalid");
    }

    public static void cancelImport(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/projects/", "project.details.title");
    }
}