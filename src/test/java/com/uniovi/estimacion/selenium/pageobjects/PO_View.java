package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;

public class PO_View {

    protected static int timeout = 5;
    protected static PO_Properties p = new PO_Properties();

    public static int getTimeout() {
        return timeout;
    }

    public static String getMessage(String key) {
        return p.getString(key);
    }

    public static String getMessage(String key, Locale locale) {
        return p.getString(key, locale);
    }

    public static String getMessage(String key, Locale locale, Object... args) {
        return p.getString(key, locale, args);
    }

    public static List<WebElement> checkElementBy(WebDriver driver,
                                                  String criterion,
                                                  String value) {
        return SeleniumUtils.waitLoadElementsBy(driver, criterion, value, timeout);
    }

    public static void checkTextIsPresent(WebDriver driver, String text) {
        SeleniumUtils.waitLoadElementsBy(driver, "text", text, timeout);
    }

    public static void checkTextIsNotPresent(WebDriver driver, String text) {
        SeleniumUtils.waitTextIsNotPresentOnPage(driver, text, timeout);
    }

    public static void checkMessageIsPresent(WebDriver driver, String messageKey) {
        checkTextIsPresent(driver, getMessage(messageKey));
    }

    public static void checkMessageIsPresent(WebDriver driver,
                                             String messageKey,
                                             Locale locale) {
        checkTextIsPresent(driver, getMessage(messageKey, locale));
    }
}