package org.redacted.util.SocialMedia;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Validates Instagram handles by checking if the profile exists.
 * Uses ScraperAPI's Async method to scrape the page and check for availability.
 *
 * @author Derrick Eberlein
 */
public class InstagramValidator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = dotenv.get("SCRAPERAPI_KEY");
    private static final String SUBMIT_URL = "https://async.scraperapi.com/jobs";

    /**
     * Uses ScraperAPI's Async method to validate whether an Instagram handle exists.
     *
     * @param handle Instagram handle (without @)
     * @return true if handle exists and page is available, false otherwise
     */
    public static Boolean isInstagramHandleValid(String handle) {
        String targetUrl = "https://www.instagram.com/" + handle + "/";
        String scraperUrl = "http://api.scraperapi.com?api_key=" + API_KEY + "&url=" + targetUrl;

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(scraperUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();

            // Return null for scraper-blocked responses so fallback can handle them
            if (status == 429 || status == 403) return null;
            if (status == 404) return false;

            // Read and parse the HTML content only on successful response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String html = reader.lines().reduce("", (a, b) -> a + b);
                return !html.contains("Sorry, this page isn't available");
            }
        } catch (IOException e) {
            // Log the exception but treat it as a blocking error (null → trigger fallback)
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Logs the JSON response to the console.
     *
     * @param json the JSON object to log
     */
    private static void logJson(Map<String, Object> json) {
        System.out.println(gson.toJson(json));
    }
}