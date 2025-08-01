package org.redacted.util.SocialMedia;

import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlPage;
import io.github.cdimascio.dotenv.Dotenv;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks social media accounts to make sure they work and are real handles.
 * Now includes Instagram login-based validation.
 *
 * @author Derrick Eberlein
 */
public class SocialMediaUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    /**
     * Validates a social media handle based on the platform.
     * Bot Currently supports Instagram and Facebook.
     *
     * @param platform the social media platform (e.g., "instagram", "facebook")
     * @param handle   the handle to validate (without @ for Instagram)
     * @return true if the handle is valid, false otherwise
     * @throws IOException if an error occurs during validation
     */
    public static Boolean isValidSocialMediaHandle(String platform, String handle) throws IOException {
        return switch (platform.toLowerCase()) {
            case "instagram" -> {
                yield InstagramValidator.isInstagramHandleValid(handle); // could be true, false, or null (handled by caller)
            }
            case "facebook" -> {
                yield isValidFacebookHandle(handle);
            }
            default -> true; // Assume valid for unknown platforms
        };
    }

    /**
     * Validates a Facebook handle by checking if the profile exists.
     * Uses HtmlUnit to fetch the page and check for "Page Not Found" or "This content isn't available".
     *
     * @param handle the Facebook handle to validate
     * @return true if the handle is valid, false otherwise
     */
    public static Boolean isValidFacebookHandle(String handle) {
        String url = "https://www.facebook.com/" + handle;
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("url", url);

        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false); // critical
            webClient.getOptions().setTimeout(5000);

            WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
            request.setAdditionalHeader("User-Agent", "Mozilla/5.0");

            HtmlPage page = webClient.getPage(request);
            WebResponse response = page.getWebResponse();
            int statusCode = response.getStatusCode();

            jsonResponse.put("statusCode", statusCode);

            /// Handle 403 Forbidden or 429 Too Many Requests
            if (statusCode == 403 || statusCode == 429) {
                jsonResponse.put("status", "manual_verification_required");
                jsonResponse.put("reason", "Blocked by Facebook (403 or 429)");
                logJsonResponse(jsonResponse);
                return null;
            }

            /// Handle 404 Not Found
            if (statusCode == 404) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Page Not Found (404)");
                logJsonResponse(jsonResponse);
                return false;
            }

            String title = page.getTitleText();
            String bodyText = page.asNormalizedText();
            jsonResponse.put("pageTitle", title);

            if (title.contains("Page Not Found") || bodyText.contains("This content isn't available")) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Page Not Found or Unavailable");
                logJsonResponse(jsonResponse);
                return false;
            }

            jsonResponse.put("status", "successful");
            logJsonResponse(jsonResponse);
            return true;

        } catch (Exception e) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "Exception occurred");
            jsonResponse.put("exceptionMessage", e.getMessage());
            logJsonResponse(jsonResponse);
            return null; // treat all exceptions as fallback triggers
        }
    }

    /**
     * Logs the JSON response to the console.
     *
     * @param jsonResponse the JSON response to log
     */
    private static void logJsonResponse(Map<String, Object> jsonResponse) {
        System.out.println(gson.toJson(jsonResponse));
    }
}