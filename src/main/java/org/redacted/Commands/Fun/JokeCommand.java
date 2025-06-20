package org.redacted.Commands.Fun;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.io.IOException;
import java.util.Objects;

/**
 * Command that fetches a random joke from an external API and sends it to the user.
 * Utilizes OkHttp for HTTP requests and Gson for JSON parsing.
 *
 * @author Derrick Eberlein
 */
public class JokeCommand extends Command {

    /**
     * Constructor for the JokeCommand.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public JokeCommand(Redacted bot) {
        super(bot);
        this.name = "joke";
        this.description = "Get a random joke.";
        this.category = Category.FUN;
    }

    /**
     * Executes the joke command.
     * Fetches a random joke from the Joke API and sends it to the user.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        OkHttpClient client = bot.httpClient;

        String url = "https://official-joke-api.appspot.com/random_joke";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            /**
             * Handles the failure of the API call.
             *
             * @param call The call that was made to the API.
             * @param e The IOException that occurred during the call.
             */
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                event.getHook().sendMessage("Failed to fetch a joke! Please try again later.").queue();
            }

            /**
             * Handles the response from the Joke API.
             *
             * @param call The call that was made to the API.
             * @param response The response received from the API.
             * @throws IOException If an I/O error occurs while reading the response.
             * This method parses the JSON response to extract the joke setup and punchline,
             */
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    event.getHook().sendMessage("Failed to fetch a joke! Please try again later.").queue();
                    return;
                }

                String responseData = Objects.requireNonNull(response.body()).string();
                JsonObject jsonObject = JsonParser.parseString(responseData).getAsJsonObject();
                String setup = jsonObject.get("setup").getAsString();
                String punchline = jsonObject.get("punchline").getAsString();

                event.getHook().sendMessage(setup + "\n" + punchline).queue();
            }
        });
    }
}
