package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Redacted;
import org.redacted.Handlers.IntroductionHandler;
import org.redacted.Handlers.StickyMessageHandler;

import static org.redacted.Handlers.IntroductionHandler.staffDeletedMessages;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntroductionListener extends ListenerAdapter {
    private final String introductionChannelId;
    private final IntroductionHandler introductionHandler;
    private final StickyMessageHandler stickyMessageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Create a scheduler

    public IntroductionListener(Redacted bot, String introductionChannelId, String staffChannelId, String memberRoleId, String flagRoleId, Guild guild) {
        this.introductionChannelId = introductionChannelId;
        this.stickyMessageHandler = new StickyMessageHandler(bot);
        this.introductionHandler = new IntroductionHandler(bot, guild, introductionChannelId, staffChannelId, memberRoleId, flagRoleId);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getId().equals(introductionChannelId) && !event.getAuthor().isBot()) {
            introductionHandler.handleIntroduction(event);
            stickyMessageHandler.handleIntroStickyMessage(event.getChannel().asTextChannel());

            Member member = event.getMember();
            if (member != null && !hasValidRole(member)) {
                // Schedule a delayed check to delete the message
                scheduler.schedule(() -> {
                    // Recheck the roles after a delay
                    if (!hasValidRole(member)) {
                        event.getMessage().delete().queue(); // Delete the message if no valid role
                        event.getAuthor().openPrivateChannel().queue(channel ->
                                channel.sendMessage("Please follow the introduction template to gain access to the server.").queue()
                        );
                    }
                }, 5, TimeUnit.SECONDS); // Delay of 3 seconds (adjust as necessary)
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

    // Shut down the scheduler when the listener is no longer needed
    public void shutdown() {
        scheduler.shutdown();
    }
}