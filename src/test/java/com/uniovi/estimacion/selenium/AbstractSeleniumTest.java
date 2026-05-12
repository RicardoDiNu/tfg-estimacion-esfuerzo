package com.uniovi.estimacion.selenium;

import com.uniovi.estimacion.selenium.support.SeleniumTestDataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("selenium")
public abstract class AbstractSeleniumTest {

    protected static WebDriver driver;

    @LocalServerPort
    protected int port;

    @Value("${selenium.headless:false}")
    private boolean headless;

    protected String baseUrl;

    @Autowired
    protected SeleniumTestDataFactory factory;

    @BeforeAll
    static void openBrowser() {
        driver = new FirefoxDriver();
    }

    @BeforeEach
    void setUpBaseUrl() {
        baseUrl = "http://localhost:" + port;
        factory.cleanDatabase();
        driver.manage().deleteAllCookies();
        driver.navigate().to(baseUrl);
    }

    @AfterEach
    void cleanBrowser() {
        driver.manage().deleteAllCookies();
    }

    @AfterAll
    static void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}
