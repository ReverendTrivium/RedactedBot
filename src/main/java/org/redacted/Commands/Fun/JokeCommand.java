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
 * Command that generates a joke from a joke API.
 *
 * @author Derrick Eberlein
 */
public class JokeCommand extends Command {

    public JokeCommand(Redacted bot) {
        super(bot);
        this.name = "joke";
        this.description = "Get a random joke.";
        this.category = Category.FUN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        OkHttpClient client = bot.httpClient;

        String url = "https://official-joke-api.appspot.com/random_joke";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                event.getHook().sendMessage("Failed to fetch a joke! Please try again later.").queue();
            }

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
