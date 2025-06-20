package org.redacted.util.SocialMedia.Reddit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * Handles Reddit OAuth authentication using the password grant type.
 * Requires OkHttp for HTTP requests and Gson for JSON parsing.
 *
 * @author Derrick Eberlein
 */
public class RedditOAuth {
    private final OkHttpClient httpClient;
    private final Gson gson;

    /**
     * Constructs a RedditOAuth instance with the provided OkHttpClient and Gson.
     *
     * @param httpClient the OkHttpClient to use for HTTP requests
     * @param gson the Gson instance for JSON parsing
     */
    public RedditOAuth(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Authenticates with Reddit using the password grant type.
     *
     * @param clientId the Reddit application client ID
     * @param clientSecret the Reddit application client secret
     * @param username the Reddit username
     * @param password the Reddit password
     * @return the access token if authentication is successful
     * @throws IOException if an error occurs during the HTTP request
     */
    public String authenticate(String clientId, String clientSecret, String username, String password) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .build();

        String authString = Credentials.basic(clientId, clientSecret);

        Request request = new Request.Builder()
                .url("https://www.reddit.com/api/v1/access_token")
                .post(formBody)
                .addHeader("Authorization", authString)
                .addHeader("User-Agent", "YourAppName")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = Objects.requireNonNull(response.body()).string();
            System.out.println("Reddit OAuth Response: " + responseBody);

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            String accessToken = json.get("access_token").getAsString();
            System.out.println("Reddit API Token: " + accessToken);

            return accessToken;
        }
    }

    /**
     * Refreshes the Reddit OAuth token.
     *
     * @return the new access token
     * @throws IOException if an error occurs during the HTTP request
     */
    public String refreshToken() throws IOException {
        // Make the token refresh request here
        // (similar to how you originally got the token)
        // For example:

        Request request = new Request.Builder()
                .url("https://www.reddit.com/api/v1/access_token")
                .post(new FormBody.Builder()
                        .add("grant_type", "password")
                        .add("username", "YOUR_REDDIT_USERNAME")
                        .add("password", "YOUR_REDDIT_PASSWORD")
                        .build())
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("YOUR_CLIENT_ID" + ":" + "YOUR_CLIENT_SECRET").getBytes()))
                .header("User-Agent", "YourAppName")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            assert response.body() != null;
            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            return jsonResponse.get("access_token").getAsString(); // Return the new access token
        }
    }
}
