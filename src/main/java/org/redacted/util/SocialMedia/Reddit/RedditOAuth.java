package org.redacted.util.SocialMedia.Reddit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

public class RedditOAuth {
    private final OkHttpClient httpClient;
    private final Gson gson;

    public RedditOAuth(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

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

    // Method to request a new token
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
