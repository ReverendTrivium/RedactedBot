package org.redacted.util.channels;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * ChannelFinder Class
 * Utility class for finding text channels by name in a Discord guild.
 *
 * @author Derrick Eberlein
 */
public class ChannelFinder {

    /**
     * Finds a text channel by name and returns its channel ID.
     *
     * @param guild The guild where the search will take place.
     * @param channelName The name of the text channel to search for.
     * @return The ID of the text channel if found, null otherwise.
     */
    public static Long getChannelIdByName(Guild guild, String channelName) {
        // Find the text channel by name
        TextChannel channel = guild.getTextChannelsByName(channelName, true)
                .stream()
                .findFirst()
                .orElse(null);

        // Return the ID of the channel if it exists, otherwise return null
        if (channel != null) {
            return channel.getIdLong();  // Returns the ID as a long
        } else {
            return null;  // Channel not found
        }
    }
}
