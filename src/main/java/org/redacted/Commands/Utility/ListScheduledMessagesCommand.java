package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListScheduledMessagesCommand extends Command {

    public ListScheduledMessagesCommand(Redacted bot) {
        super(bot);
        this.name = "list-scheduled";
        this.description = "List all scheduled messages.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get the guild ID from the event
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        // Retrieve the scheduled messages for this specific guild
        List<Document> scheduledMessages = bot.database.getScheduledMessages(guildId);

        if (scheduledMessages.isEmpty()) {
            event.reply("There are no scheduled messages.").queue();
            return;
        }

        List<EmbedBuilder> embeds = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");

        for (Document message : scheduledMessages) {
            String title = message.getString("title");
            String content = message.getString("content");
            String channelId = message.getString("channelId");
            String repeat = message.getString("repeat");
            LocalDateTime time = LocalDateTime.parse(message.getString("time"));
            String messageId = message.getObjectId("_id").toHexString();  // Get the unique ID

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Scheduled Message");
            embed.addField("Message ID", messageId, false);  // Add the unique ID field
            embed.addField("Title", title != null ? title : "No Title", false);
            embed.addField("Content", content, false);
            embed.addField("Channel", "<#" + channelId + ">", false);
            embed.addField("Scheduled Time", time.format(formatter), false);
            embed.addField("Repeat", repeat != null ? repeat : "None", false);
            embed.setFooter("Scheduled by: " + message.getString("userId"));

            embeds.add(embed);
        }

        // Use ButtonListener for pagination
        ButtonListener.sendPaginatedMenu(event.getUser().getId(), event.replyEmbeds(embeds.get(0).build()), toMessageEmbeds(embeds));
    }

    private List<net.dv8tion.jda.api.entities.MessageEmbed> toMessageEmbeds(List<EmbedBuilder> embeds) {
        List<net.dv8tion.jda.api.entities.MessageEmbed> messageEmbeds = new ArrayList<>();
        for (EmbedBuilder builder : embeds) {
            messageEmbeds.add(builder.build());
        }
        return messageEmbeds;
    }
}
