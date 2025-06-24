package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Handlers.IntroductionHandler;
import org.redacted.Handlers.StickyMessageHandler;
import org.redacted.Redacted;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.redacted.Handlers.IntroductionHandler.staffDeletedMessages;

/**
 * IntroductionListener Class
 * This class listens for messages in the introduction channel and handles user introductions.
 * It checks if the user has a valid role and manages sticky messages.
 *
 * @author Derrick Eberlein
 */
public class IntroductionListener extends ListenerAdapter {
    private final String introductionChannelId;
    private final IntroductionHandler introductionHandler;
    private final StickyMessageHandler stickyMessageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Create a scheduler

    /**
     * Constructs an IntroductionListener with the provided parameters.
     *
     * @param bot                  The Redacted bot instance.
     * @param introductionChannelId The ID of the introduction channel.
     * @param staffChannelId       The ID of the staff channel.
     * @param memberRoleId         The ID of the member role.
     * @param flagRoleId           The ID of the flag role.
     * @param guild                The Guild instance for the server.
     */
    public IntroductionListener(Redacted bot, String introductionChannelId, String staffChannelId, String memberRoleId, String flagRoleId, Guild guild) {
        this.introductionChannelId = introductionChannelId;
        this.stickyMessageHandler = new StickyMessageHandler(bot);
        this.introductionHandler = new IntroductionHandler(bot, guild, introductionChannelId, staffChannelId, memberRoleId, flagRoleId);
    }

    /**
     * Handles incoming messages in the introduction channel.
     * If the message is from a user (not a bot), it processes the introduction,
     * checks for valid roles, and manages sticky messages.
     *
     * @param event The MessageReceivedEvent containing the message and context.
     */
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

    /**
     * Checks if the member has a valid role (not just the @everyone role).
     *
     * @param member The Member to check.
     * @return true if the member has a valid role, false otherwise.
     */
    private boolean hasValidRole(Member member) {
        // Check if the user has any role other than the @everyone role
        return member.getRoles().stream().anyMatch(role -> !role.isPublicRole());
    }

    /**
     * Handles message deletions in the introduction channel.
     * If a message is deleted, it processes the deletion through the IntroductionHandler.
     *
     * @param event The MessageDeleteEvent containing the deletion details.
     */
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.getChannel().getId().equals(introductionChannelId)) {
            introductionHandler.handleIntroductionDeletion(event);
        }
    }

    /**
     * Marks a message as deleted by staff.
     * This is used to track messages that have been deleted by staff members.
     *
     * @param messageId The ID of the message to mark as deleted.
     */
    public static void markAsStaffDeleted(String messageId) {
        staffDeletedMessages.put(messageId, true);
    }


    /**
     * Shuts down the scheduler used for delayed message deletion.
     * This should be called when the bot is shutting down to clean up resources.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}