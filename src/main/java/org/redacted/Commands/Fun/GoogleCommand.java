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

/**
 * Command that allows users to search Google for answers to their questions.
 * Utilizes the GoogleSearchService to perform the search and returns the top result.
 *
 * @author Derrick Eberlein
 */
public class GoogleCommand extends Command {

    private final GoogleSearchService googleSearchService;

    /**
     * Constructor for the GoogleCommand.
     * Initializes the command with its name, description, and GoogleSearchService.
     *
     * @param bot The Redacted bot instance.
     * @param googleSearchService The service used to perform Google searches.
     */
    public GoogleCommand(Redacted bot, GoogleSearchService googleSearchService) {
        super(bot);
        this.name = "google";
        this.description = "Search Google for an answer to your question.";
        this.googleSearchService = googleSearchService;

        this.args.add(new OptionData(OptionType.STRING, "query", "The question you want to ask Google.", true));
    }

    /**
     * Executes the Google command.
     * Searches Google for the provided query and returns the top result in an embed.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
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


