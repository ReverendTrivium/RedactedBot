package org.redacted.listeners.Ticket;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * TicketRemoveUserHandler Class
 * This class handles the removal of users from ticket channels in a Discord server.
 * It listens for messages that start with "-ticket removeuser" and removes the mentioned user from the ticket channel.
 *
 * @author Derrick Eberlein
 */
public class TicketRemoveUserHandler extends ListenerAdapter {

    /**
     * Handles incoming messages to check for the "-ticket removeuser" command.
     * If the command is detected, it removes the mentioned user from the ticket channel.
     *
     * @param event The MessageReceivedEvent containing the message and context.
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ");
        if (!args[0].equalsIgnoreCase("-ticket") || args.length < 3 || !args[1].equalsIgnoreCase("removeuser")) return;

        TextChannel channel = event.getChannel().asTextChannel();
        String channelName = channel.getName();
        if (!channelName.matches("\\d+-.*")) {
            channel.sendMessage("❌ This doesn't look like a ticket channel.").queue();
            return;
        }

        List<Member> mentioned = event.getMessage().getMentions().getMembers();
        if (mentioned.isEmpty()) {
            channel.sendMessage("❌ Please mention a user to remove from the ticket.").queue();
            return;
        }

        Member toRemove = mentioned.get(0);

        if (channel.getPermissionOverride(toRemove) == null) {
            channel.sendMessage("❌ This user does not have access to this ticket.").queue();
            return;
        }

        Objects.requireNonNull(channel.getPermissionOverride(toRemove)).delete().queue(
                success -> channel.sendMessage("✅ Removed " + toRemove.getAsMention() + " from the ticket.").queue(),
                error -> channel.sendMessage("❌ Failed to remove user: " + error.getMessage()).queue()
        );
    }
}
