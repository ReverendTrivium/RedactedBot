package org.redacted.util.SocialMedia;

import org.htmlunit.WebRequest;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks social media accounts to make sure they work and are real handles.
 *
 * @author Derrick Eberlein
 */
public class SocialMediaUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static boolean isValidSocialMediaHandle(String platform, String handle) throws IOException {
        return !switch (platform.toLowerCase()) {
            case "instagram" -> isValidInstagramHandle(handle);
            case "facebook" -> isValidFacebookHandle(handle);
            default -> true;
        };
    }

    public static boolean isValidInstagramHandle(String handle) {
        String url = "https://www.instagram.com/" + handle;
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("url", url);

        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setTimeout(5000);

            WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
            request.setAdditionalHeader("User-Agent", "Mozilla/5.0");

            HtmlPage page = webClient.getPage(request);
            String title = page.getTitleText();
            String bodyText = page.asNormalizedText();

            jsonResponse.put("pageTitle", title);

            if (title.contains("Page Not Found") || bodyText.contains("Sorry, this page isn't available.")) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Page Not Found");
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
            return false;
        }
    }

    public static boolean isValidFacebookHandle(String handle) {
        String url = "https://www.facebook.com/" + handle;
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("url", url);

        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setTimeout(5000);

            WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
            request.setAdditionalHeader("User-Agent", "Mozilla/5.0");

            HtmlPage page = webClient.getPage(request);
            String title = page.getTitleText();
            String bodyText = page.asNormalizedText();

            jsonResponse.put("pageTitle", title);

            if (title.contains("Page Not Found") || bodyText.contains("This content isn't available")) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Page Not Found");
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
            return false;
        }
    }

    private static void logJsonResponse(Map<String, Object> jsonResponse) {
        System.out.println(gson.toJson(jsonResponse));
    }
}