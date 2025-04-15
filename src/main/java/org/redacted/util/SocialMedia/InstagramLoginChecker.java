package org.redacted.util.SocialMedia;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class InstagramLoginChecker {

    public static boolean loginAndCheck(String username, String password, String targetHandle) {
        WebDriver driver = new ChromeDriver();

        try {
            driver.get("https://www.instagram.com/accounts/login/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Wait for login inputs
            WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
            WebElement passwordInput = driver.findElement(By.name("password"));

            usernameInput.sendKeys(username);
            passwordInput.sendKeys(password);

            WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
            loginButton.click();

            // Wait for post-login redirect
            wait.until(ExpectedConditions.urlContains("/"));

            // Go to target handle
            driver.get("https://www.instagram.com/" + targetHandle);

            // Wait to confirm page loads
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            String title = driver.getTitle();
            return !(title.contains("Page Not Found") || driver.getPageSource().contains("Sorry, this page isn't available."));

        } catch (Exception e) {
            System.err.println("Exception during check: " + e.getMessage());
            return false;
        } finally {
            driver.quit();
        }
    }
}

