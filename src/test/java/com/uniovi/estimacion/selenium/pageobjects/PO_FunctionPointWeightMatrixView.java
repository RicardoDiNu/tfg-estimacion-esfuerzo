package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class PO_FunctionPointWeightMatrixView extends PO_NavView {

    public static void checkWeightMatrixEditView(WebDriver driver) {
        checkMessageIsPresent(driver, "fp.weights.edit.title");
    }

    public static void openEditWeightMatrix(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/function-points/weights/edit", "fp.weights.edit.title");
    }

    public static void setFirstRowWeights(WebDriver driver, String low, String average, String high) {
        List<WebElement> lowInputs =
                SeleniumUtils.waitLoadElementsBy(driver, "free",
                        "//input[contains(@name,'lowWeight')]", getTimeout());
        lowInputs.get(0).clear();
        lowInputs.get(0).sendKeys(low);

        List<WebElement> avgInputs =
                SeleniumUtils.waitLoadElementsBy(driver, "free",
                        "//input[contains(@name,'averageWeight')]", getTimeout());
        avgInputs.get(0).clear();
        avgInputs.get(0).sendKeys(average);

        List<WebElement> highInputs =
                SeleniumUtils.waitLoadElementsBy(driver, "free",
                        "//input[contains(@name,'highWeight')]", getTimeout());
        highInputs.get(0).clear();
        highInputs.get(0).sendKeys(high);
    }

    public static void saveWeightMatrix(WebDriver driver) {
        driver.findElement(By.xpath("//button[@type='submit' and not(ancestor::nav)]")).click();
        checkMessageIsPresent(driver, "fp.weights.details.title");
    }

    public static void resetWeightMatrix(WebDriver driver) {
        List<WebElement> candidates = driver.findElements(By.xpath(
                "//a[contains(@href,'reset')]" +
                        " | //button[contains(normalize-space(.),'Restaurar')]" +
                        " | //button[contains(normalize-space(.),'Restablecer')]" +
                        " | //button[contains(normalize-space(.),'Reiniciar')]"
        ));

        Assertions.assertFalse(candidates.isEmpty(),
                "No se encontró botón/enlace para restaurar la matriz de pesos");

        safeClick(driver, candidates.get(0));

        try {
            new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
                    .until(ExpectedConditions.alertIsPresent())
                    .accept();
        } catch (TimeoutException ignored) {
            // Si no hay confirm, seguimos.
        }

        checkTextIsPresent(driver, "Matriz de pesos");
    }
}
