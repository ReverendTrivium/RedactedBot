package org.redacted.util.SocialMedia.Reddit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class RedditClient {
    private final OkHttpClient httpClient;
    private final RedditTokenManager tokenProvider;
    private final Random random;

    public RedditClient(OkHttpClient httpClient, RedditTokenManager tokenProvider) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.random = new Random();
    }

    public String getRandomImage(String subreddit) throws IOException {
        String accessToken = tokenProvider.getValidToken();
        if (accessToken == null) throw new IOException("Failed to get valid Reddit access token");

        String[] endpoints = {"hot", "new", "top"}; // Exclude "random" for reliability
        List<String> endpointPool = new ArrayList<>(List.of(endpoints));
        Collections.shuffle(endpointPool); // Randomize order

        IOException lastException = null;

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

        throw new IOException("Failed to fetch Reddit image after trying all endpoints.", lastException);
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
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful() && response.body() != null;
        }
    }

    public List<String> getGalleryImages(String galleryUrl) {
        List<String> imageUrls = new ArrayList<>();
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(galleryUrl).get();
            doc.select("a[href]").forEach(element -> {
                String url = element.attr("href");
                if (url.contains("preview.redd.it") && url.contains("format=pjpg&auto=webp")) {
                    url = url.replace("&amp;", "&");
                    imageUrls.add(url);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageUrls;
    }
}
