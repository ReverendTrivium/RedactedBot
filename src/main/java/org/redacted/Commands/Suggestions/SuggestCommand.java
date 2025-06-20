package org.redacted.Commands.Suggestions;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.SuggestionHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;


/**
 * Command that allows a user to make a suggestion on the suggestion board.
 *
 * @author Derrick Eberlein
 */
public class SuggestCommand extends Command {

    /**
     * Constructor for the SuggestCommand.
     * Initializes the command with its name, description, category, and required arguments.
     *
     * @param bot The Redacted bot instance.
     */
    public SuggestCommand(Redacted bot) {
        super(bot);
        this.name = "suggest";
        this.description = "Add a suggestion to the suggestion board.";
        this.category = Category.SUGGESTIONS;
        this.args.add(new OptionData(OptionType.STRING, "suggestion", "The content for your suggestion", true));
    }

    /**
     * Executes the SuggestCommand.
     * This method handles the interaction when the command is invoked.
     * It processes the suggestion and sends it to the designated suggestion channel.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        // Check if suggestion board has been setup
        SuggestionHandler suggestionHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getSuggestionHandler();
        if (!suggestionHandler.isSetup()) {
            String text = "The suggestion channel has not been set!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
            return;
        }

        // Create suggestion embed
        String content = Objects.requireNonNull(event.getOption("suggestion")).getAsString();
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("Suggestion #" + suggestionHandler.getNumber())
                .setDescription(content);

        // Add author to embed if anonymous mode is turned off
        if (suggestionHandler.isAnonymous()) {
            embed.setAuthor("Anonymous", null, "https://cdn.discordapp.com/embed/avatars/0.png");
        } else {
            embed.setAuthor(event.getUser().getName().substring(0, 1).toUpperCase() + event.getUser().getName().substring(1).toLowerCase(), null, event.getUser().getEffectiveAvatarUrl());
        }

        // Make sure channel is valid
        TextChannel channel = event.getGuild().getTextChannelById(suggestionHandler.getChannel());
        if (channel == null) {
            String text = "The suggestion channel has been deleted, please set a new one!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
            return;
        }

        // Add suggestion and reaction buttons
        channel.sendMessageEmbeds(embed.build()).queue(suggestion -> {
            suggestionHandler.add(suggestion.getIdLong(), event.getUser().getIdLong());
            try {
                suggestion.addReaction(Emoji.fromUnicode("U+2B06")).queue(); // Using Emoji.fromUnicode()
                suggestion.addReaction(Emoji.fromUnicode("U+2B07")).queue(); // Using Emoji.fromUnicode()
            } catch (InsufficientPermissionException ignored) { }
        });

        // Send a response message
        String text = EmbedUtils.BLUE_TICK + "Your suggestion has been added to <#" + suggestionHandler.getChannel() + ">!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).setEphemeral(true).queue();
    }
}
