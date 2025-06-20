package org.redacted.listeners.Ticket;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TicketRemoveUserHandler extends ListenerAdapter {

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
