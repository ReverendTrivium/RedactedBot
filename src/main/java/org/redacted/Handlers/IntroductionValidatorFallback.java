package org.redacted.Handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles manual fallback for Instagram verification when automatic checks fail.
 * This is used when Instagram returns a 403 Forbidden or 429 Too Many Requests error.
 * It sends a message to the mod-log channel for staff to manually verify the handle.
 *
 * @author Derrick Eberlein
 */
public class IntroductionValidatorFallback {

    // Maps mod-log message ID ➝ user ID of the person needing verification
    public static final Map<Long, Map<String, String>> pendingManualApprovals = new ConcurrentHashMap<>();

    /**
     * Sends a manual fallback message when Instagram returns 403 or 429, with ✅ reaction for staff.
     *
     * @param handle   Social Media handle to verify
     * @param member   Member who submitted the intro
     * @param channel  Mod-log channel where manual fallback should be sent
     * @param firstName First name of the user for context
     * @param instagramHandle Instagram handle submitted by the user (optional)
     * @param facebookHandle Facebook handle submitted by the user (optional)
     */
    public static void handleManualFallback(String handle, Member member, TextChannel channel, String firstName,
                                            String instagramHandle, String facebookHandle) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Manual Verification Required");
        embed.setColor(Color.ORANGE);
        embed.setDescription(String.format(
                "I couldn't automatically verify the Instagram handle **@%s**.\n\n" +
                        "Please manually check the profile using the link below and react with ✅ if valid.", handle));
        embed.addField("Instagram Profile", "https://www.instagram.com/" + handle + "/", false);
        embed.setFooter("Automatic check was blocked or rate-limited.");

        channel.sendMessageEmbeds(embed.build()).queue((Message msg) -> {
            msg.addReaction(Emoji.fromUnicode("✅")).queue();
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userId", String.valueOf(member.getIdLong()));
            userInfo.put("firstName", firstName);
            userInfo.put("instagram", instagramHandle != null ? instagramHandle : "");
            userInfo.put("facebook", facebookHandle != null ? facebookHandle : "");

            pendingManualApprovals.put(msg.getIdLong(), userInfo);
        });
    }
}