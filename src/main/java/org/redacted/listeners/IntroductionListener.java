package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Handlers.IntroductionHandler;
import org.redacted.Handlers.StickyMessageHandler;
import org.redacted.Redacted;

import static org.redacted.Handlers.IntroductionHandler.staffDeletedMessages;

public class IntroductionListener extends ListenerAdapter {
    private final String introductionChannelId;
    private final IntroductionHandler introductionHandler;
    private final StickyMessageHandler stickyMessageHandler;

    public IntroductionListener(Redacted bot, String introductionChannelId, String staffChannelId, String memberRoleId, String flagRoleId, Guild guild) {
        this.introductionChannelId = introductionChannelId;
        this.stickyMessageHandler = new StickyMessageHandler(bot);
        this.introductionHandler = new IntroductionHandler(bot, guild, introductionChannelId, staffChannelId, memberRoleId, flagRoleId);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getId().equals(introductionChannelId) && !event.getAuthor().isBot()) {
            introductionHandler.handleIntroduction(event);
            stickyMessageHandler.handleStickyMessage(event.getChannel().asTextChannel());

            // Check if the user has the member role or other specific roles
            Member member = event.getMember();
            if (member != null && !hasValidRole(member)) {
                event.getMessage().delete().queue();
                event.getAuthor().openPrivateChannel().queue(channel ->
                        channel.sendMessage("Please follow the introduction template to gain access to the server.").queue()
                );
            }
        }
    }

    private boolean hasValidRole(Member member) {
        // Check if the user has any role other than the @everyone role
        return member.getRoles().stream().anyMatch(role -> !role.isPublicRole());
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.getChannel().getId().equals(introductionChannelId)) {
            introductionHandler.handleIntroductionDeletion(event);
        }
    }

    public static void markAsStaffDeleted(String messageId) {
        staffDeletedMessages.put(messageId, true);
    }
}
