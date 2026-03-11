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
 * NSFWCleanListener.java
 * This listener handles the automatic cleaning of NSFW channels in a Discord server.
 * It purges messages in the channel and sends a sticky message to inform users about the clean-up.
 * The sticky message is updated or created based on whether it exists in the database.
 *
 * @author Derrick Eberlein
 */
public class NSFWCleanListener extends ListenerAdapter {

    /**
     * Default constructor for NSFWCleanListener.
     * This constructor is used to create an instance of the listener.
     */
    public NSFWCleanListener() {
    }

    /**
     * Cleans the specified NSFW channel by purging messages and sending a sticky message.
     * This method retrieves the last 500 messages from the channel, purges them, and checks for an existing sticky message.
     * If a sticky message exists, it updates it; otherwise, it sends a new sticky message.
     *
     * @param channel The NSFW channel to clean.
     * @param bot     The Redacted bot instance used for database operations.
     */
    public static void cleanChannel(TextChannel channel, Redacted bot) {
        channel.getGuild().getIdLong();
        GuildData guildData = GuildData.get(channel.getGuild(), bot);

        // Check if the NSFW clean is toggled on for the guild
        if (!guildData.isNSFWCleanEnabled()) {
            return; // Feature is disabled, exit early
        }

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
     * Sends a sticky message to the specified channel.
     * This message informs users about the automatic cleaning of NSFW channels and provides a reminder to save any liked images.
     *
     * @param channel   The channel where the sticky message will be sent.
     * @param guildData The GuildData instance containing the database collection for sticky messages.
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
     * Updates the existing sticky message in the specified channel.
     * This method deletes the old sticky message from the database and sends a new one.
     *
     * @param channel          The channel where the sticky message will be updated.
     * @param ignoredMessage   The old sticky message that is being replaced (not used in this method).
     * @param bot              The Redacted bot instance used for database operations.
     */
    private static void updateStickyMessage(TextChannel channel, Message ignoredMessage, Redacted bot) {
        GuildData guildData = GuildData.get(channel.getGuild(), bot);
        // Delete the old sticky message
        guildData.getStickyMessagesCollection().deleteOne(new Document("channelId", channel.getIdLong()));

        // Send a new sticky message
        sendStickyMessage(channel, guildData);
    }
}
