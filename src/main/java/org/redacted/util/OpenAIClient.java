package org.redacted.util;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public class OpenAIClient {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = dotenv.get("OPENAI_API_KEY"); // Best practice
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo"; // Or use "gpt-4"

    private static final OkHttpClient client = new OkHttpClient();

    public static String summarize(String context) throws IOException {

        // Validate API key
        System.out.println("API Key: " + API_KEY);
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
        }

        JSONObject payload = new JSONObject();
        payload.put("model", MODEL);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are a helpful assistant that summarizes Discord conversations."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Summarize the following:\n\n" + context));

        payload.put("messages", messages);
        payload.put("temperature", 0.7);

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + Objects.requireNonNull(response.body()).string());
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            JSONObject responseJson = new JSONObject(responseBody);
            return responseJson
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        }
    }
}
