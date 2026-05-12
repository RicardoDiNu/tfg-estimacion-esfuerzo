package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;

public class PO_NavView extends PO_View {

    public static void clickOption(WebDriver driver,
                                   String hrefText,
                                   String targetCriterion,
                                   String targetText) {
        List<WebElement> elements =
                SeleniumUtils.waitLoadElementsBy(driver, "@href", hrefText, getTimeout());

        Assertions.assertFalse(elements.isEmpty());

        elements.get(0).click();

        SeleniumUtils.waitLoadElementsBy(driver, targetCriterion, targetText, getTimeout());
    }

    public static void clickOptionAndWaitForMessage(WebDriver driver,
                                                    String hrefText,
                                                    String targetMessageKey) {
        clickOption(
                driver,
                hrefText,
                "text",
                getMessage(targetMessageKey)
        );
    }

    public static void clickOptionAndWaitForMessage(WebDriver driver,
                                                    String hrefText,
                                                    String targetMessageKey,
                                                    Locale locale) {
        clickOption(
                driver,
                hrefText,
                "text",
                getMessage(targetMessageKey, locale)
        );
    }

    public static void goToProjects(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/projects", "nav.projects");
    }

    public static void goToUsers(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/users", "user.management.list.title");
    }

    public static void goToAccount(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/account", "account.profile.title");
    }

    public static void goToLogin(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/login", "login.title");
    }

    public static void logout(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/logout", "login.title");
    }

    public static void switchToEnglish(WebDriver driver) {
        checkElementBy(driver, "id", "btnEnglish").get(0).click();
        checkMessageIsPresent(driver, "index.title", PO_Properties.ENGLISH);
    }

    public static void switchToSpanish(WebDriver driver) {
        checkElementBy(driver, "id", "btnSpanish").get(0).click();
        checkMessageIsPresent(driver, "index.title", PO_Properties.SPANISH);
    }
}