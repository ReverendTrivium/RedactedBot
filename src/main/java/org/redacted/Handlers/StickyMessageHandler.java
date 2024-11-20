package org.redacted.Handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

import java.time.Instant;

public class StickyMessageHandler {
    private final Redacted bot;

    public StickyMessageHandler(Redacted bot) {
        this.bot = bot;
    }

    public void handleIntroStickyMessage(TextChannel channel) {
        // Get the guild-specific collection for sticky messages
        GuildData guildData = GuildData.get(channel.getGuild(), bot);

        // Create the embed message
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("***Introductions***")
                .setDescription(
                        ":arrow_forward:  **Template to Follow to gain access to Server:** :arrow_backward: \n" +
                                "*Copy and Paste template below to gain access to the server!!*\n\n" +
                                "**Name:** [real name or nickname] *(Required, will not get access to server without this)*\n" +
                                "**Instagram Tag:** [Don't include the @ character] *(Required, unless you would rather use Facebook)*\n" +
                                "**Facebook:** (Required, if you don't want to use Instagram)\n" +
                                "**Pronouns:** *(Optional)*\n" +
                                "**Location (DC/MD/VA):** *(Optional)*\n" +
                                "**Favorite Animes/Manga:** *(Optional)*\n" +
                                "**About Me:** *(Optional)*\n\n" +
                                "***Do not bold the template like shown in the example format.***"
                )
                .setThumbnail(channel.getGuild().getIconUrl())
                .setFooter("Last updated", channel.getGuild().getIconUrl())
                .setTimestamp(Instant.now());

        // Retrieve the previous sticky message ID from the guild-specific database
        Document stickyMessageDoc = guildData.getStickyMessagesCollection().find(new Document("channelId", channel.getIdLong())).first();
        if (stickyMessageDoc != null) {
            long messageId = stickyMessageDoc.getLong("messageId");
            //Print Sticky Message ID:
            System.out.println("StickMessage ID: " + messageId);
            // Delete the previous sticky message
            try {
                channel.deleteMessageById(messageId).queue();
            } catch (Exception e) {
                System.err.println("Failed to delete previous sticky message: " + e.getMessage());
            }
        }

        // Send the new sticky message and save its ID with a bot-generated flag
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> guildData.getStickyMessagesCollection().updateOne(
                new Document("channelId", channel.getIdLong()),
                new Document("$set", new Document("messageId", message.getIdLong())
                        .append("isBotGenerated", true)), // Mark as bot-generated
                new com.mongodb.client.model.UpdateOptions().upsert(true)
        ));
        // print New Sticky Message ID
    }
}
