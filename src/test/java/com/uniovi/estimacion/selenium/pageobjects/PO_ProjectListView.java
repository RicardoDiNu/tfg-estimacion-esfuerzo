package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class PO_ProjectListView extends PO_NavView {

    public static void open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/projects");
        checkProjectList(driver);
    }

    public static void checkProjectList(WebDriver driver) {
        checkMessageIsPresent(driver, "nav.projects");
    }

    public static void checkProjectIsPresent(WebDriver driver, String projectName) {
        checkTextIsPresent(driver, projectName);
    }

    public static void checkProjectIsNotPresent(WebDriver driver, String projectName) {
        checkTextIsNotPresent(driver, projectName);
    }

    public static void clickAddProject(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/projects/add", "project.add.title");
    }

    public static void openProjectDetails(WebDriver driver, String projectName) {
        checkProjectIsPresent(driver, projectName);

        List<WebElement> projectLinks =
                SeleniumUtils.waitLoadElementsBy(driver, "text", projectName, getTimeout());

        projectLinks.get(0).click();

        PO_ProjectDetailsView.checkProjectDetails(driver, projectName);
    }

    public static void fillProjectForm(WebDriver driver,
                                       String name,
                                       String description,
                                       String hourlyRate,
                                       String currencyCode) {
        WebElement nameInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "name", getTimeout()).get(0);
        nameInput.clear();
        nameInput.sendKeys(name);

        WebElement descriptionInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "description", getTimeout()).get(0);
        descriptionInput.clear();
        descriptionInput.sendKeys(description);

        WebElement hourlyRateInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "hourlyRate", getTimeout()).get(0);
        hourlyRateInput.clear();
        hourlyRateInput.sendKeys(hourlyRate);

        WebElement currencyInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "currencyCode", getTimeout()).get(0);
        currencyInput.clear();
        currencyInput.sendKeys(currencyCode);

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }
}