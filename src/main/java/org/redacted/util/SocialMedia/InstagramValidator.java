package org.redacted.util.SocialMedia;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
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
    public static boolean isInstagramHandleValid(String handle) {
        try {
            // Step 1: Submit async job
            System.out.println("Submitting async scrape job for Instagram handle: " + handle);
            String payload = "{"
                    + "\"apiKey\": \"" + API_KEY + "\","
                    + "\"urls\": [\"https://www.instagram.com/" + handle + "/\"],"
                    + "\"render\": true"
                    + "}";
            HttpURLConnection conn = (HttpURLConnection) new URL(SUBMIT_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to submit scrape job.");
                return false;
            }

            JsonArray jobArray;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                jobArray = JsonParser.parseReader(in).getAsJsonArray();
            }

            if (jobArray.isEmpty()) {
                System.err.println("Empty job response.");
                return false;
            }

            JsonObject job = jobArray.get(0).getAsJsonObject();
            String statusUrl = job.get("statusUrl").getAsString();

            // Step 2: Poll for job completion
            JsonObject jobStatus;
            int maxAttempts = 10;
            int attempts = 0;

            do {
                Thread.sleep(2000);
                HttpURLConnection statusConn = (HttpURLConnection) new URL(statusUrl).openConnection();
                statusConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(statusConn.getInputStream()))) {
                    jobStatus = JsonParser.parseReader(in).getAsJsonObject();
                }

                String status = jobStatus.get("status").getAsString();
                if (status.equalsIgnoreCase("failed")) {
                    System.err.println("Scrape job failed.");
                    return false;
                }

                if (status.equalsIgnoreCase("successful")) {
                    break;
                }

            } while (++attempts < maxAttempts);

            System.out.println("Response:" + gson.toJson(jobStatus));

            if (!jobStatus.has("responseBody")) {
                System.err.println("No responseBody found in successful job.");
                return false;
            }

            // Step 3: Inspect responseBody
            String responseBody = jobStatus.get("responseBody").getAsString();
            return !responseBody.contains("Sorry, this page isn't available") &&
                    !responseBody.contains("The link you followed may be broken") &&
                    !responseBody.contains("Page Not Found");

        } catch (Exception e) {
            System.err.println("Exception during async scrape: " + e.getMessage());
            e.printStackTrace();
            return false;
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