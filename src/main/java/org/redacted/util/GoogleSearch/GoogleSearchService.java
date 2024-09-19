package org.redacted.util.GoogleSearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class GoogleSearchService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GOOGLE_API_KEY");  // Load from .env
    private static final String SEARCH_ENGINE_ID = dotenv.get("GOOGLE_SEARCH_ENGINE_ID");  // Load from .env
    private static final String SEARCH_URL = "https://www.googleapis.com/customsearch/v1";

    private final OkHttpClient httpClient;

    public GoogleSearchService() {
        this.httpClient = new OkHttpClient();
    }

    public List<SearchResult> search(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        String url = SEARCH_URL + "?key=" + API_KEY + "&cx=" + SEARCH_ENGINE_ID + "&q=" + query;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response);
            }

            JsonObject jsonObject = JsonParser.parseString(Objects.requireNonNull(response.body()).string()).getAsJsonObject();
            JsonArray items = jsonObject.getAsJsonArray("items");

            if (items != null) {
                items.forEach(item -> {
                    JsonObject obj = item.getAsJsonObject();
                    String title = obj.get("title").getAsString();
                    String link = obj.get("link").getAsString();
                    String snippet = obj.get("snippet").getAsString();

                    results.add(new SearchResult(title, link, snippet));
                });
            }
        }

        return results;
    }

    @Getter
    public static class SearchResult {
        private final String title;
        private final String link;
        private final String snippet;

        public SearchResult(String title, String link, String snippet) {
            this.title = title;
            this.link = link;
            this.snippet = snippet;
        }

    }
}