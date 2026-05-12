package com.uniovi.estimacion.selenium.util;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SeleniumUtils {

    public static List<WebElement> waitLoadElementsBy(WebDriver driver,
                                                      String criterion,
                                                      String value,
                                                      int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        By by = getBy(criterion, value);

        wait.until(ExpectedConditions.presenceOfElementLocated(by));

        return driver.findElements(by);
    }

    public static void waitTextIsNotPresentOnPage(WebDriver driver,
                                                  String text,
                                                  int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text)
        ));
    }

    private static By getBy(String criterion, String value) {
        return switch (criterion) {
            case "id" -> By.id(value);
            case "class" -> By.className(value);
            case "text" -> By.xpath("//*[contains(text(),'" + value + "')]");
            case "@href" -> By.xpath("//a[contains(@href,'" + value + "')]");
            case "free" -> By.xpath(value);
            default -> throw new IllegalArgumentException("Unsupported Selenium criterion: " + criterion);
        };
    }
}