package org.redacted.util.SocialMedia;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks social media accounts to make sure they work and are real handles.
 *
 * @author Derrick Eberlein
 */
public class SocialMediaUtils {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static boolean isValidSocialMediaHandle(String platform, String handle) throws IOException {
        return switch (platform.toLowerCase()) {
            case "instagram" -> isValidInstagramHandle(handle);
            case "facebook" -> isValidFacebookHandle(handle);
            default -> true;
        };
    }

    public static boolean isValidInstagramHandle(String handle) {
        String url = "https://www.instagram.com/" + handle;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Map<String, Object> jsonResponse = new HashMap<>();

        try (Response response = client.newCall(request).execute()) {
            jsonResponse.put("url", url);
            jsonResponse.put("responseCode", response.code());

            if (!response.isSuccessful()) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Response not successful");
                logJsonResponse(jsonResponse);
                return false;
            }

            ResponseBody body = response.body();
            if (body != null) {
                String responseBody = body.string();
                jsonResponse.put("responseBody", responseBody);

                Document doc = Jsoup.parse(responseBody);

                String title = doc.title();
                if (title.contains("Page Not Found") || title.contains("Sorry, this page isn't available.")) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Page Not Found");
                    jsonResponse.put("pageTitle", title);
                    logJsonResponse(jsonResponse);
                    return false;
                }

                if (responseBody.toLowerCase().contains("polariserror")) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Page Not Found / polariserror");
                    logJsonResponse(jsonResponse);
                    return false;
                }

                if (!doc.select("div.error-container").isEmpty()) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Error container found");
                    logJsonResponse(jsonResponse);
                    return false;
                }
            }
            jsonResponse.put("status", "successful");
            logJsonResponse(jsonResponse);
            return true;
        } catch (IOException e) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "IOException occurred");
            jsonResponse.put("exceptionMessage", e.getMessage());
            logJsonResponse(jsonResponse);
            return false;
        }
    }

    public static boolean isValidFacebookHandle(String handle) {
        String url = "https://www.facebook.com/" + handle;

        Request requests = new Request.Builder()
                .url(url)
                .build();

        Map<String, Object> jsonResponse = new HashMap<>();

        try (Response response = client.newCall(requests).execute()) {
            jsonResponse.put("url", url);
            jsonResponse.put("responseCode", response.code());

            if (!response.isSuccessful()) {
                jsonResponse.put("status", "failed");
                jsonResponse.put("reason", "Response not successful");
                logJsonResponse(jsonResponse);
                return false;
            }

            ResponseBody body = response.body();
            if (body != null) {
                String responseBody = body.string();
                jsonResponse.put("responseBody", responseBody);

                Document doc = Jsoup.parse(responseBody);

                String title = doc.title();
                if (title.contains("Page Not Found") || title.contains("Sorry, this page isn't available.")) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Page Not Found");
                    jsonResponse.put("pageTitle", title);
                    logJsonResponse(jsonResponse);
                    return false;
                }

                if (responseBody.toLowerCase().contains("this content is")) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Page Not Found");
                    logJsonResponse(jsonResponse);
                    return false;
                }

                if (!doc.select("div.error-container").isEmpty()) {
                    jsonResponse.put("status", "failed");
                    jsonResponse.put("reason", "Error container found");
                    logJsonResponse(jsonResponse);
                    return false;
                }
            }
            jsonResponse.put("status", "successful");
            logJsonResponse(jsonResponse);
            return true;
        } catch (IOException e) {
            jsonResponse.put("status", "failed");
            jsonResponse.put("reason", "IOException occurred");
            jsonResponse.put("exceptionMessage", e.getMessage());
            logJsonResponse(jsonResponse);
            return false;
        }
    }

    private static void logJsonResponse(Map<String, Object> jsonResponse) {
        System.out.println(gson.toJson(jsonResponse));
    }
}