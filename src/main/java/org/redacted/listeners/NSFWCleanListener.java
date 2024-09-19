package org.redacted.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

public class NSFWCleanListener extends ListenerAdapter {

    public NSFWCleanListener() {
    }

    public static void cleanChannel(TextChannel channel, Redacted bot) {
        channel.getGuild().getIdLong();
        GuildData guildData = GuildData.get(channel.getGuild(), bot);

        channel.getIterableHistory().takeAsync(500).thenAccept(messages -> {
            channel.purgeMessages(messages);

            Document stickyMessageDoc = guildData.getStickyMessagesCollection().find(new Document("channelId", channel.getIdLong())).first();
            if (stickyMessageDoc != null) {
                long messageId = stickyMessageDoc.getLong("messageId");
                try {
                    channel.retrieveMessageById(messageId).queue(
                            message -> updateStickyMessage(channel, message, bot), // Message exists, update it
                            throwable -> sendStickyMessage(channel, guildData) // Message does not exist, send a new one
                    );
                } catch (ErrorResponseException e) {
                    // If the message is not found, send a new sticky message
                    if (e.getErrorCode() == 10008) { // Unknown Message
                        sendStickyMessage(channel, guildData);
                    } else {
                        e.printStackTrace();
                    }
                }
            } else {
                sendStickyMessage(channel, guildData); // No sticky message found in the database, send a new one
            }
        });
    }

    private static void sendStickyMessage(TextChannel channel, GuildData guildData) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("\uD83C\uDF1F **NSFW Channel Cleaned** \uD83C\uDF1F");
        embedBuilder.setDescription(
                """
                        *Every 3 days this channel will be automatically cleaned to help moderate the server.*

                        *This is expected, so if you see any images you like, save them before they are gone!*

                        """
        );
        embedBuilder.setImage("https://media1.tenor.com/m/dG_tr_lmYHkAAAAC/looking-around.gif");

        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
            // Store the new sticky message ID in the database
            guildData.getStickyMessagesCollection().updateOne(
                    new Document("channelId", channel.getIdLong()),
                    new Document("$set", new Document("messageId", message.getIdLong())),
                    new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
        });
    }

    private static void updateStickyMessage(TextChannel channel, Message ignoredMessage, Redacted bot) {
        GuildData guildData = GuildData.get(channel.getGuild(), bot);
        // Delete the old sticky message
        guildData.getStickyMessagesCollection().deleteOne(new Document("channelId", channel.getIdLong()));

        // Send a new sticky message
        sendStickyMessage(channel, guildData);
    }
}
