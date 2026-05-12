package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class PO_UserManagementView extends PO_NavView {

    public static void open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/users");
        checkUserManagement(driver);
    }

    public static void checkUserManagement(WebDriver driver) {
        checkMessageIsPresent(driver, "user.management.list.title");
    }

    public static void checkUserIsPresent(WebDriver driver, String username) {
        checkTextIsPresent(driver, username);
    }

    public static void checkUserIsNotPresent(WebDriver driver, String username) {
        checkTextIsNotPresent(driver, username);
    }

    public static void clickAddUser(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/users/add", "user.management.add.title");
    }

    public static void fillBasicUserForm(WebDriver driver,
                                         String username,
                                         String email,
                                         String password) {
        WebElement usernameInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "username", getTimeout()).get(0);
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement emailInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "email", getTimeout()).get(0);
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "password", getTimeout()).get(0);
        passwordInput.clear();
        passwordInput.sendKeys(password);

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public static void fillWorkerForm(WebDriver driver,
                                      String username,
                                      String email,
                                      String password) {
        WebElement usernameInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "username", getTimeout()).get(0);
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement emailInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "email", getTimeout()).get(0);
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "password", getTimeout()).get(0);
        passwordInput.clear();
        passwordInput.sendKeys(password);

        WebElement roleSelect =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "role", getTimeout()).get(0);
        new Select(roleSelect).selectByValue("ROLE_PROJECT_WORKER");

        List<WebElement> pmSelects =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "projectManagerId", getTimeout());
        if (!pmSelects.isEmpty()) {
            Select pmSelect = new Select(pmSelects.get(0));
            if (pmSelect.getOptions().size() > 1) {
                pmSelect.selectByIndex(1);
            }
        }

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public static void clickEditForUser(WebDriver driver, String username) {
        List<WebElement> editLinks = SeleniumUtils.waitLoadElementsBy(
                driver, "free",
                "//tr[td[normalize-space(text())='" + username + "']]//a[contains(@href,'/edit')]",
                getTimeout());
        editLinks.get(0).click();
        checkMessageIsPresent(driver, "user.management.edit.title");
    }

    public static void fillEditEmailForm(WebDriver driver, String newEmail) {
        WebElement emailInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "email", getTimeout()).get(0);
        emailInput.clear();
        emailInput.sendKeys(newEmail);

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public static void checkWorkerRoleIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "user.role.ROLE_PROJECT_WORKER");
    }

    public static void checkProjectManagerRoleIsVisible(WebDriver driver) {
        checkMessageIsPresent(driver, "user.role.ROLE_PROJECT_MANAGER");
    }
}
