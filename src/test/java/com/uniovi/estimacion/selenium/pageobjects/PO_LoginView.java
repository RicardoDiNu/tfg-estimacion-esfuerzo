package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PO_LoginView extends PO_NavView {

    public static void open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/login");
        checkLoginView(driver);
    }

    public static void checkLoginView(WebDriver driver) {
        checkMessageIsPresent(driver, "login.title");
    }

    public static void fillLoginForm(WebDriver driver, String username, String password) {
        WebElement usernameInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "username", getTimeout()).get(0);
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement passwordInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "password", getTimeout()).get(0);
        passwordInput.clear();
        passwordInput.sendKeys(password);

        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public static void login(WebDriver driver, String username, String password) {
        fillLoginForm(driver, username, password);
    }

    public static void checkLoginError(WebDriver driver) {
        checkMessageIsPresent(driver, "login.error");
    }

    public static void checkLoginSuccessRedirectToProjects(WebDriver driver) {
        checkMessageIsPresent(driver, "nav.projects");
    }
}