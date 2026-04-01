package org.redacted.Handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.redacted.Roles.getRolesByName;

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

        Role adminRole = new getRolesByName().getRoleByName(channel.getGuild(), "Admin");
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Manual Verification Required");
        embed.setColor(Color.ORANGE);
        embed.setDescription(String.format(
                "%s I couldn't automatically verify the Instagram handle **@%s**.\n\n" +
                        "Please manually check the profile using the link below and react with ✅ if valid.", adminRole, handle));
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

    /**
     * Handles manual approval for users who do not provide any social media handles.
     *
     * @param member    Member who submitted the intro
     * @param channel   Mod-log channel where manual approval should be sent
     * @param firstName First name of the user for context
     * @param lastName  Last name of the user for context (optional)
     */
    public static void handleNoSocialManualApproval(Member member, TextChannel channel,
                                                    String firstName, String lastName) {
        Role adminRole = new getRolesByName().getRoleByName(channel.getGuild(), "Admin");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Manual Approval Required");
        embed.setColor(Color.YELLOW);
        embed.setDescription(String.format(
                "%s **%s** stated that they do not use Instagram or Facebook.\n\n" +
                        "Please manually review and react with ✅ to approve them.",
                adminRole, member.getAsMention()));
        embed.addField("Submitted Name", firstName + (lastName != null && !lastName.equalsIgnoreCase("None") ? " / " + lastName : ""), false);
        embed.addField("Social Media", "None provided", false);
        embed.setFooter("Manual approval required for members without social media.");

        channel.sendMessageEmbeds(embed.build()).queue((Message msg) -> {
            msg.addReaction(Emoji.fromUnicode("✅")).queue();

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userId", String.valueOf(member.getIdLong()));
            userInfo.put("firstName", firstName != null ? firstName : "");
            userInfo.put("lastName", lastName != null ? lastName : "");
            userInfo.put("instagram", "");
            userInfo.put("facebook", "");
            userInfo.put("manualType", "no_social");

            pendingManualApprovals.put(msg.getIdLong(), userInfo);
        });
    }
}