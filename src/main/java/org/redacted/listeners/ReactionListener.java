package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.redacted.Handlers.IntroductionHandler;
import org.redacted.Handlers.IntroductionValidatorFallback;
import org.redacted.Redacted;
import org.redacted.RedactedStartup.ChannelManager;
import org.redacted.Roles.getRolesByName;

import java.util.Map;

/**
 * ReactionListener Class
 * This class listens for reactions on messages in the introduction channel.
 * It handles manual approvals for user introductions when staff reacts with a checkmark.
 *
 * @author Derrick Eberlein
 */
public class ReactionListener extends ListenerAdapter {
    private final Redacted bot;

    /**
     * Constructs a ReactionListener with the provided Redacted bot instance.
     *
     * @param bot The Redacted bot instance.
     */
    public ReactionListener(Redacted bot) {
        this.bot = bot;
    }

    /**
     * Handles message reactions, specifically looking for checkmark reactions
     * to approve user introductions.
     *
     * @param event The MessageReactionAddEvent containing the reaction and context.
     */
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        if (!event.getReaction().getEmoji().getName().equals("✅")) return;

        long messageId = event.getMessageIdLong();
        if (!IntroductionValidatorFallback.pendingManualApprovals.containsKey(messageId)) return;

        // Remove from pending approvals and get user metadata
        Map<String, String> userInfo = IntroductionValidatorFallback.pendingManualApprovals.remove(messageId);
        long userId = Long.parseLong(userInfo.get("userId"));
        String firstName = userInfo.get("firstName");
        String instagramHandle = userInfo.get("instagramHandle");
        String facebookHandle = userInfo.get("facebookHandle");

        Guild guild = event.getGuild();
        Member member = guild.getMemberById(userId);
        if (member == null) {
            event.getReaction().removeReaction(event.getUser()).queue();
            return;
        }

        Role memberRole = new getRolesByName().getRoleByName(guild, "Member");
        if (memberRole == null) return;

        // Reconstruct dummy MessageReceivedEvent to reuse approveUser logic
        MessageChannel channel = event.getChannel();
        Message dummyMessage = channel.retrieveMessageById(messageId).complete(); // Optional: only for logging

        if (dummyMessage == null) {
            event.getReaction().removeReaction(event.getUser()).queue();
            return; // Message not found, can't proceed
        }

        MessageReceivedEvent dummyEvent = new MessageReceivedEvent(event.getJDA(), 0, dummyMessage);

        // Create a ChannelManager to grab channelID's
        ChannelManager channelManager = new ChannelManager();

        // Get or create necessary channels
        TextChannel moderationChannel = channelManager.getOrCreateTextChannel(guild, "mod-log", "Moderation");
        TextChannel introductionChannel = channelManager.getOrCreateTextChannel(guild, "introductions", "Information");

        // Recreate handler with known config
        String introductionChannelId = introductionChannel.getId();
        String staffChannelId = moderationChannel.getId();
        String memberRoleId = memberRole.getId();
        String flagRoleId = new getRolesByName().getRoleByName(guild, "Flagged").getId();

        IntroductionHandler handler = new IntroductionHandler(bot, guild, introductionChannelId, staffChannelId, memberRoleId, flagRoleId);
        handler.approveUser(dummyEvent, member, memberRole, firstName, instagramHandle, facebookHandle);
    }
}