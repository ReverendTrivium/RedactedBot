package org.redacted.listeners.Ticket;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class TicketAddUserHandler extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ");
        if (!args[0].equalsIgnoreCase("-ticket") || args.length < 3 || !args[1].equalsIgnoreCase("adduser")) return;

        TextChannel channel = event.getChannel().asTextChannel();
        String channelName = channel.getName();
        if (!channelName.matches("\\d+-.*")) {
            channel.sendMessage("❌ This doesn't look like a ticket channel.").queue();
            return;
        }

        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            channel.sendMessage("❌ Please mention a user to add to the ticket.").queue();
            return;
        }

        Member toAdd = event.getMessage().getMentions().getMembers().get(0);

        channel.getManager().putPermissionOverride(toAdd,
                EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null).queue(
                success -> channel.sendMessage("✅ Added " + toAdd.getAsMention() + " to the ticket.").queue(),
                error -> channel.sendMessage("❌ Failed to add user: " + error.getMessage()).queue()
        );
    }
}
