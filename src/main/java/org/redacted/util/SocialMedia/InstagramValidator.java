package org.redacted.util.SocialMedia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.cdimascio.dotenv.Dotenv;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class InstagramValidator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static boolean isInstagramHandleValid(String handle) {
        Map<String, Object> jsonResponse = new HashMap<>();
        String url = "https://www.instagram.com/" + handle;
        jsonResponse.put("url", url);

        String username = dotenv.get("INSTAGRAM_USERNAME");
        String password = dotenv.get("INSTAGRAM_PASSWORD");

        if (username == null || password == null) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "Missing credentials in .env");
            logJson(jsonResponse);
            return false;
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Go to Instagram login page
            driver.get("https://www.instagram.com/accounts/login/");

            // Wait for login form
            WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
            WebElement passwordInput = driver.findElement(By.name("password"));
            usernameInput.sendKeys(username);
            passwordInput.sendKeys(password);

            WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
            loginButton.click();

            // Wait for post-login redirect (we can wait for homepage or something stable)
            wait.until(ExpectedConditions.urlContains("/"));

            // Go to target profile
            driver.get(url);

            // Wait for body to confirm page load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            String title = driver.getTitle();
            String pageSource = driver.getPageSource();

            jsonResponse.put("pageTitle", title);

            if (title.contains("Page Not Found") || pageSource.contains("Sorry, this page isn't available.")) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Page Not Found");
                logJson(jsonResponse);
                return false;
            }

            jsonResponse.put("status", "successful");
            logJson(jsonResponse);
            return true;

        } catch (Exception e) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "Exception occurred");
            jsonResponse.put("exceptionMessage", e.getMessage());
            logJson(jsonResponse);
            return false;
        } finally {
            driver.quit();
        }
    }

    private static void logJson(Map<String, Object> json) {
        System.out.println(gson.toJson(json));
    }
}