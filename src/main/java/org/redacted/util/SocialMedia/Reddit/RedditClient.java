package org.redacted.util.SocialMedia.Reddit;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.jsoup.Jsoup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RedditClient {
    private final OkHttpClient httpClient;
    private final String accessToken;
    private final Random random;

    public RedditClient(OkHttpClient httpClient, String accessToken) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .retryOnConnectionFailure(true)
                .build();
        this.accessToken = accessToken;
        this.random = new Random();
    }

    public String getRandomImage(String subreddit) throws IOException {
        String[] endpoints = {"hot", "new", "top"};
        String endpoint = endpoints[random.nextInt(endpoints.length)];
        String url = "https://oauth.reddit.com/r/" + subreddit + "/" + endpoint + ".json?limit=50";

        if (random.nextBoolean()) {
            url = "https://oauth.reddit.com/r/" + subreddit + "/random.json";
        }

        System.out.println("URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "MyBot/1.0 by u/your_reddit_user")
                .build();

        IOException lastException = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("Reddit request failed: " + response);
                    continue;
                }

                String responseData = Objects.requireNonNull(response.body()).string();
                JsonReader reader = new JsonReader(new StringReader(responseData));
                reader.setLenient(true);

                JsonArray children;

                if (url.contains("random")) {
                    try {
                        JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
                        if (!arr.isEmpty()) {
                            children = arr.get(0).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("children");
                        } else {
                            throw new IOException("No data found in random.json response.");
                        }
                    } catch (IllegalStateException e) {
                        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                        children = obj.getAsJsonObject("data").getAsJsonArray("children");
                    }
                } else {
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                    children = jsonObject.getAsJsonObject("data").getAsJsonArray("children");
                }

                List<String> mediaUrls = new ArrayList<>();
                for (int i = 0; i < children.size(); i++) {
                    JsonObject postData = children.get(i).getAsJsonObject().getAsJsonObject("data");
                    String mediaUrl = extractMediaUrl(postData);
                    if (mediaUrl != null) {
                        mediaUrls.add(mediaUrl);
                    }
                }

                if (mediaUrls.isEmpty()) {
                    throw new IOException("No valid media URLs found.");
                }

                Collections.shuffle(mediaUrls);
                return mediaUrls.get(0);
            } catch (IOException ex) {
                System.out.println("Retrying Reddit fetch (attempt " + (attempt + 1) + "): " + ex.getMessage());
                lastException = ex;
                httpClient.connectionPool().evictAll();
            }
        }

        throw new IOException("Failed to fetch Reddit image after 3 attempts.", lastException);
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