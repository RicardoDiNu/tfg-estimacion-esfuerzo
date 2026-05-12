package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        checkMessageIsPresent(driver, "fp.weights.details.title");
    }

    public static void resetWeightMatrix(WebDriver driver) {
        List<WebElement> resetButtons =
                SeleniumUtils.waitLoadElementsBy(driver, "free",
                        "//button[@type='submit' and @onclick]", getTimeout());
        resetButtons.get(0).click();
        checkMessageIsPresent(driver, "fp.weights.details.title");
    }
}
