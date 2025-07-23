package org.redacted.util.SocialMedia;

import com.google.gson.*;
import org.htmlunit.*;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.Map;

/**
 * Validates Instagram handles by checking if the profile exists.
 * Uses HtmlUnit to scrape the Instagram page and check for availability.
 *
 * @author Derrick Eberlein
 */
public class InstagramValidator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Validates whether an Instagram handle exists using HtmlUnit.
     *
     * @param handle Instagram handle (without @)
     * @return true if the handle exists and the page is available, false if 404, null if other failure
     */
    public static Boolean isInstagramHandleValid(String handle) {
        String targetUrl = "https://www.instagram.com/" + handle + "/";

        try (final WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setTimeout(5000);

            HtmlPage page = webClient.getPage(targetUrl);
            WebResponse response = page.getWebResponse();
            int status = response.getStatusCode();

            if (status == 404) return false;
            if (status == 403 || status == 429) return null;

            String content = page.asNormalizedText();
            return !content.contains("Sorry, this page isn't available");

        } catch (IOException e) {
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
