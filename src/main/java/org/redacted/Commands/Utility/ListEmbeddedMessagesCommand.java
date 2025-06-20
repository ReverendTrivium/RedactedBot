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

public class ListEmbeddedMessagesCommand extends Command {

    public ListEmbeddedMessagesCommand(Redacted bot) {
        super(bot);
        this.name = "listembeddedmessages";
        this.description = "Lists all saved embed messages for this server.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

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
