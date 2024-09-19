package org.redacted.Commands.Fun;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.GoogleSearch.GoogleSearchService;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class GoogleCommand extends Command {

    private final GoogleSearchService googleSearchService;

    public GoogleCommand(Redacted bot, GoogleSearchService googleSearchService) {
        super(bot);
        this.name = "google";
        this.description = "Search Google for an answer to your question.";
        this.googleSearchService = googleSearchService;

        this.args.add(new OptionData(OptionType.STRING, "query", "The question you want to ask Google.", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();

        event.deferReply().queue();  // Acknowledge the command

        try {
            List<GoogleSearchService.SearchResult> results = googleSearchService.search(query);

            if (results.isEmpty()) {
                event.getHook().sendMessage("No results found.").setEphemeral(true).queue();
                return;
            }

            GoogleSearchService.SearchResult topResult = results.get(0);

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(topResult.getTitle(), topResult.getLink())
                    .setDescription(topResult.getSnippet())
                    .setColor(Color.BLUE)
                    .setFooter("Search provided by Google", "https://www.google.com/favicon.ico");

            event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("An error occurred while searching. Please try again later.").setEphemeral(true).queue();
            e.printStackTrace();
        }
    }
}


