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

/**
 * Client for interacting with the Reddit API to fetch random images from subreddits.
 * Supports both NSFW and SFW content, with fallback mechanisms for better reliability.
 * Uses OkHttp for HTTP requests and Gson for JSON parsing.
 *
 * @author Derrick Eberlein
 */
public class RedditClient {
    private final OkHttpClient httpClient;
    private final RedditTokenManager tokenProvider;

    /**
     * Constructs a RedditClient with the specified OkHttpClient and RedditTokenManager.
     *
     * @param httpClient the OkHttpClient to use for HTTP requests
     * @param tokenProvider the RedditTokenManager to manage OAuth tokens
     */
    public RedditClient(OkHttpClient httpClient, RedditTokenManager tokenProvider) {
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Fetch a random NSFW image from the specified subreddit.
     * This method is specifically designed for NSFW content and will fall back to a default subreddit if the main one fails.
     *
     * * @param subreddit the subreddit to fetch images from
     * @return a URL of a random NSFW image
     */
    public String getRandomImageNSFW(String subreddit) throws IOException {
        return getRandomImageWithFallbackNSFW(subreddit, "porn");
    }

    /**
     * Fetches a random image from the specified subreddit, with a fallback to a default subreddit if the main one fails.
     * This method is specifically designed for NSFW content.
     *
     * @param subreddit the subreddit to fetch images from
     * @param fallbackSubreddit the subreddit to fall back to if the main one fails
     * @return a URL of a random image from the subreddit
     * @throws IOException if an error occurs while fetching images
     */
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

    /**
     * Fetch a random image from the specified subreddit.
     * This method is designed for SFW content and will fall back to a default subreddit if the main one fails.
     *
     * @param subreddit the subreddit to fetch images from
     * @return a URL of a random image
     * @throws IOException if an error occurs while fetching images
     */
    public String getRandomImage(String subreddit) throws IOException {
        return getRandomImageWithFallback(subreddit, "MoeBlushing");
    }

    /**
     * Fetches a random image from the specified subreddit, with a fallback to a default subreddit if the main one fails.
     * This method is specifically designed for SFW content.
     *
     * @param subreddit the subreddit to fetch images from
     * @param fallbackSubreddit the subreddit to fall back to if the main one fails
     * @return a URL of a random image from the subreddit
     * @throws IOException if an error occurs while fetching images
     */
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

    /**
     * Extracts the media URL from a Reddit post's JSON data.
     * Handles both image and video formats.
     *
     * @param mediaData the JSON object containing media data
     * @return the extracted media URL, or null if not found
     */
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

    /**
     * Validates a media URL by checking its accessibility and content type.
     * Skips known non-media URLs and Reddit HTML posts.
     *
     * @param url the URL to validate
     * @param includeVideos whether to include video URLs in validation
     * @return true if the URL is valid media, false otherwise
     * @throws IOException if an error occurs while checking the URL
     */
    public boolean isValidMediaUrl(String url, boolean includeVideos) throws IOException {
        // Basic skip: known non-media or unwanted domains
        if (!includeVideos && (
                url.endsWith(".mp4") ||
                        url.contains("v.redd.it") ||
                        url.contains("redgifs.com") ||
                        url.contains("youtu.be") ||
                        url.contains("youtube"))
        ) {
            System.out.println("Skipping video: " + url);
            return false;
        }

        if (url.contains("imgur.com") || url.contains("patreon.com")) {
            System.out.println("Skipping unsupported domain: " + url);
            return false;
        }

        // Skip Reddit post HTML (but allow Reddit API .json endpoints)
        if (url.matches("https://(www\\.)?reddit\\.com/r/[^/]+/comments/[^/]+.*") && !url.endsWith(".json")) {
            System.out.println("Skipping Reddit HTML post: " + url);
            return false;
        }

        // Check HTTP response
        System.out.println("Checking if URL is valid: " + url);
        String accessToken = tokenProvider.getValidToken();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "EyaBot/1.0 by /u/JonTronsCareer")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            boolean isOk = response.isSuccessful();
            if (!isOk) {
                System.out.println("Request failed: " + response.code());
            }
            return isOk;
        }
    }

    /**
     * Fetches gallery images from a Reddit post URL.
     * The URL should be in the format: https://www.reddit.com/r/subreddit/comments/post_id/title/
     *
     * @param galleryUrl the Reddit post URL containing the gallery
     * @return a list of image URLs from the gallery
     */
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
