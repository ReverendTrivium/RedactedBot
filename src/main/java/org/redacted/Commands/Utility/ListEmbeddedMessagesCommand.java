package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command to list all saved embed messages for the server.
 * This command retrieves and displays a list of embed messages that have been saved in the database.
 * It is useful for managing and reviewing previously created embeds.
 *
 * @author Derrick Eberlein
 */
public class ListEmbeddedMessagesCommand extends Command {

    /**
     * Constructor for the ListEmbeddedMessagesCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public ListEmbeddedMessagesCommand(Redacted bot) {
        super(bot);
        this.name = "listembeddedmessages";
        this.description = "Lists all saved embed messages for this server.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    /**
     * Executes the ListEmbeddedMessagesCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves all saved embed messages from the database and formats them into an embed for display.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        MongoCollection<SavedEmbed> collection = GuildData.getDatabase().getSavedEmbedsCollection(Objects.requireNonNull(guild).getIdLong());

        List<SavedEmbed> embeds = collection.find().into(new java.util.ArrayList<>());

        if (embeds.isEmpty()) {
            event.reply("❌ No saved embed messages found for this guild.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embedList = new EmbedBuilder();
        embedList.setTitle("📄 Saved Embed Messages");
        embedList.setColor(Color.CYAN);

        String embedSummary = embeds.stream()
                .map(e -> String.format("• [`%s`](https://discord.com/channels/%s/%s/%s) — %s",
                        e.getMessageId(),
                        guild.getId(),
                        e.getChannelId(),
                        e.getMessageId(),
                        e.getTitle() != null ? e.getTitle() : "(no title)"
                ))
                .limit(10) // avoid overfill
                .collect(Collectors.joining("\n"));

        embedList.setDescription(embedSummary);
        embedList.setFooter("Showing up to 10 saved embeds. Total: " + embeds.size());

        event.replyEmbeds(embedList.build()).setEphemeral(true).queue();
    }
}
