package org.redacted.util.SocialMedia.Reddit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class RedditClient {
    private final OkHttpClient httpClient;
    private final RedditTokenManager tokenProvider;

    public RedditClient(OkHttpClient httpClient, RedditTokenManager tokenProvider) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
    }

    public String getRandomImageNSFW(String subreddit) throws IOException {
        return getRandomImageWithFallbackNSFW(subreddit, "porn");
    }

    private String getRandomImageWithFallbackNSFW(String subreddit, String fallbackSubreddit) throws IOException {
        String[] endpoints = {"hot", "new", "top"};
        List<String> endpointPool = new ArrayList<>(List.of(endpoints));
        Collections.shuffle(endpointPool); // Randomize to vary traffic

        IOException lastException = null;

        // Try the main subreddit first
        for (String endpoint : endpointPool) {
            String url = "https://oauth.reddit.com/r/" + subreddit + "/" + endpoint + ".json?limit=50";
            System.out.println("Trying URL: " + url);

            for (int attempt = 0; attempt < 3; attempt++) {
                String token = tokenProvider.getValidToken();
                if (token == null) throw new IOException("Token fetch failed during retry");

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", "YourAppName")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.out.println("Reddit request failed: " + response);
                        continue;
                    }

                    String responseData = Objects.requireNonNull(response.body()).string();
                    JsonReader reader = new JsonReader(new StringReader(responseData));
                    reader.setLenient(true);

                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray children = jsonObject.getAsJsonObject("data").getAsJsonArray("children");

                    List<String> mediaUrls = new ArrayList<>();
                    for (int i = 0; i < children.size(); i++) {
                        JsonObject postData = children.get(i).getAsJsonObject().getAsJsonObject("data");
                        String mediaUrl = extractMediaUrl(postData);
                        if (mediaUrl != null) {
                            mediaUrls.add(mediaUrl);
                        }
                    }

                    if (!mediaUrls.isEmpty()) {
                        Collections.shuffle(mediaUrls);
                        return mediaUrls.get(0);
                    } else {
                        throw new IOException("No valid media URLs found.");
                    }
                } catch (IOException ex) {
                    System.out.println("Retrying fetch (attempt " + (attempt + 1) + ") for " + endpoint + ": " + ex.getMessage());
                    lastException = ex;
                    httpClient.connectionPool().evictAll();
                }
            }

            System.out.println("All 3 attempts failed for endpoint: " + endpoint + ", trying next one.");
        }

        // If all endpoints fail, try fallback subreddit
        if (!subreddit.equalsIgnoreCase(fallbackSubreddit)) {
            System.out.println("All subreddit attempts failed. Falling back to: " + fallbackSubreddit);
            return getRandomImageWithFallback(fallbackSubreddit, fallbackSubreddit); // Avoid infinite loop
        }

        throw new IOException("Failed to fetch Reddit image after trying all endpoints and fallback.", lastException);
    }

    public String getRandomImage(String subreddit) throws IOException {
        return getRandomImageWithFallback(subreddit, "MoeBlushing");
    }

    private String getRandomImageWithFallback(String subreddit, String fallbackSubreddit) throws IOException {
        String[] endpoints = {"hot", "new", "top"};
        List<String> endpointPool = new ArrayList<>(List.of(endpoints));
        Collections.shuffle(endpointPool); // Randomize to vary traffic

        IOException lastException = null;

        // Try the main subreddit first
        for (String endpoint : endpointPool) {
            String url = "https://oauth.reddit.com/r/" + subreddit + "/" + endpoint + ".json?limit=50";
            System.out.println("Trying URL: " + url);

            for (int attempt = 0; attempt < 3; attempt++) {
                String token = tokenProvider.getValidToken();
                if (token == null) throw new IOException("Token fetch failed during retry");

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", "YourAppName")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.out.println("Reddit request failed: " + response);
                        continue;
                    }

                    String responseData = Objects.requireNonNull(response.body()).string();
                    JsonReader reader = new JsonReader(new StringReader(responseData));
                    reader.setLenient(true);

                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray children = jsonObject.getAsJsonObject("data").getAsJsonArray("children");

                    List<String> mediaUrls = new ArrayList<>();
                    for (int i = 0; i < children.size(); i++) {
                        JsonObject postData = children.get(i).getAsJsonObject().getAsJsonObject("data");
                        String mediaUrl = extractMediaUrl(postData);
                        if (mediaUrl != null) {
                            mediaUrls.add(mediaUrl);
                        }
                    }

                    if (!mediaUrls.isEmpty()) {
                        Collections.shuffle(mediaUrls);
                        return mediaUrls.get(0);
                    } else {
                        throw new IOException("No valid media URLs found.");
                    }
                } catch (IOException ex) {
                    System.out.println("Retrying fetch (attempt " + (attempt + 1) + ") for " + endpoint + ": " + ex.getMessage());
                    lastException = ex;
                    httpClient.connectionPool().evictAll();
                }
            }

            System.out.println("All 3 attempts failed for endpoint: " + endpoint + ", trying next one.");
        }

        // If all endpoints fail, try fallback subreddit
        if (!subreddit.equalsIgnoreCase(fallbackSubreddit)) {
            System.out.println("All subreddit attempts failed. Falling back to: " + fallbackSubreddit);
            return getRandomImageWithFallback(fallbackSubreddit, fallbackSubreddit); // Avoid infinite loop
        }

        throw new IOException("Failed to fetch Reddit image after trying all endpoints and fallback.", lastException);
    }


    private String extractMediaUrl(JsonObject mediaData) {
        if (mediaData.has("url")) {
            return mediaData.get("url").getAsString();
        } else if (mediaData.has("media")) {
            JsonObject media = mediaData.getAsJsonObject("media");
            if (media.has("reddit_video")) {
                return media.getAsJsonObject("reddit_video").get("fallback_url").getAsString();
            }
        }
        return null;
    }

    public boolean isValidUrl(String url) throws IOException {
        System.out.println("Checking if URL is valid: " + url);
        String accessToken = tokenProvider.getValidToken();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "EyaBot/1.0 by /u/JonTronsCareer") // Make it Reddit-compliant
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public List<String> getGalleryImages(String galleryUrl) {
        List<String> imageUrls = new ArrayList<>();
        try {
            String accessToken = tokenProvider.getValidToken();
            String postId = galleryUrl.replaceAll(".*/(\\w+)$", "$1"); // Extract post ID
            String apiUrl = "https://oauth.reddit.com/comments/" + postId + ".json";

            System.out.println("Fetching gallery images from: " + apiUrl);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("User-Agent", "EyaBot/1.0 by /u/JonTronsCareer") // Make it Reddit-compliant
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("Request failed: " + response.code());
                    return imageUrls;
                }

                assert response.body() != null;
                String body = response.body().string();
                JSONArray root = new JSONArray(body);
                JSONObject postData = root.getJSONObject(0)
                        .getJSONObject("data")
                        .getJSONArray("children")
                        .getJSONObject(0)
                        .getJSONObject("data");

                if (!postData.has("gallery_data") || !postData.has("media_metadata")) {
                    System.out.println("Not a gallery post.");
                    return imageUrls;
                }

                JSONArray items = postData.getJSONObject("gallery_data").getJSONArray("items");
                JSONObject metadata = postData.getJSONObject("media_metadata");

                for (int i = 0; i < items.length(); i++) {
                    String mediaId = items.getJSONObject(i).getString("media_id");
                    String imageUrl = metadata.getJSONObject(mediaId)
                            .getJSONObject("s")
                            .getString("u")
                            .replaceAll("&amp;", "&");
                    imageUrls.add(imageUrl);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrls;
    }

}
