package com.uniovi.estimacion.selenium.pageobjects;

import org.openqa.selenium.WebDriver;

import java.util.Locale;

public class PO_HomeView extends PO_NavView {

    public static void open(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/");
    }

    public static void checkHome(WebDriver driver) {
        checkMessageIsPresent(driver, "index.title");
    }

    public static void checkHome(WebDriver driver, Locale locale) {
        checkMessageIsPresent(driver, "index.title", locale);
    }

    public static void checkAbout(WebDriver driver) {
        checkMessageIsPresent(driver, "about.title");
    }

    public static void checkHelp(WebDriver driver) {
        checkMessageIsPresent(driver, "help.title");
    }

    public static void goToAbout(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/about", "about.title");
    }

    public static void goToHelp(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/help", "help.title");
    }

    public static void goToLoginFromHome(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/login", "login.title");
    }
}