package org.redacted.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

/**
 * NSFWCleanListener Class
 * This class listens for events in NSFW channels and cleans them periodically.
 * It purges messages and sends a sticky message to inform users about the cleaning.
 *
 * @author Derrick Eberlein
 */
public class NSFWCleanListener extends ListenerAdapter {

    /**
     * Default constructor for NSFWCleanListener.
     * Initializes the listener without any specific setup.
     */
    public NSFWCleanListener() {
    }

    /**
     * Cleans the specified NSFW channel by purging messages and sending a sticky message.
     * It retrieves the last 500 messages, purges them, and checks for an existing sticky message.
     * If the sticky message exists, it updates it; otherwise, it sends a new sticky message.
     *
     * @param channel The NSFW TextChannel to clean.
     * @param bot     The Redacted bot instance for database access and operations.
     */
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

    /**
     * Sends a sticky message to the specified NSFW channel.
     * The sticky message informs users about the periodic cleaning of the channel.
     *
     * @param channel   The NSFW TextChannel to send the sticky message to.
     * @param guildData The GuildData instance containing the sticky messages collection.
     */
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

    /**
     * Updates the sticky message in the specified NSFW channel.
     * Deletes the old sticky message and sends a new one.
     *
     * @param channel          The NSFW TextChannel where the sticky message is located.
     * @param ignoredMessage   The old sticky message to be deleted (ignored in this context).
     * @param bot              The Redacted bot instance for database access and operations.
     */
    private static void updateStickyMessage(TextChannel channel, Message ignoredMessage, Redacted bot) {
        GuildData guildData = GuildData.get(channel.getGuild(), bot);
        // Delete the old sticky message
        guildData.getStickyMessagesCollection().deleteOne(new Document("channelId", channel.getIdLong()));

        // Send a new sticky message
        sendStickyMessage(channel, guildData);
    }
}
