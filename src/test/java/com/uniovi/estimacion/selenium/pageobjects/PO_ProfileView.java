package com.uniovi.estimacion.selenium.pageobjects;

import com.uniovi.estimacion.selenium.util.SeleniumUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PO_ProfileView extends PO_NavView {

    public static void checkProfileView(WebDriver driver) {
        checkMessageIsPresent(driver, "account.profile.title");
    }

    public static void clickEditAccount(WebDriver driver) {
        clickOptionAndWaitForMessage(driver, "/account/edit", "account.edit.title");
    }

    public static void fillEditAccountForm(WebDriver driver, String newEmail) {
        WebElement emailInput =
                SeleniumUtils.waitLoadElementsBy(driver, "id", "email", getTimeout()).get(0);
        emailInput.clear();
        emailInput.sendKeys(newEmail);

        driver.findElement(By.xpath("//button[@type='submit' and not(ancestor::nav)]")).click();
    }

    public static void checkEmailIsPresent(WebDriver driver, String email) {
        checkTextIsPresent(driver, email);
    }
}
