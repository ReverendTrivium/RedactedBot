package org.redacted.Handlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bson.Document;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;
import org.redacted.util.SocialMedia.SocialMediaUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Handles Introduction Channel Messages for server authorization.
 *
 * @author Derrick Eberlein
 */
public class IntroductionHandler {
    private final String staffChannelId;
    private final String memberRoleId;
    private final String flagRoleId;
    private final MongoCollection<Document> userIntroMessagesCollection;
    private final MongoCollection<Document> blacklistCollection;
    private final ConcurrentMap<String, String> userIntroMessages;
    public static final ConcurrentMap<String, Boolean> staffDeletedMessages = new ConcurrentHashMap<>();
    private boolean socialfound = false;

    public IntroductionHandler(Redacted bot, Guild guild, String introductionChannelId, String staffChannelId, String memberRoleId, String flagRoleId) {
        this.staffChannelId = staffChannelId;
        this.memberRoleId = memberRoleId;
        this.flagRoleId = flagRoleId;

        // Retry until GuildData/Config is initialized, without a hard retry limit
        GuildData guildData = waitForGuildDataInitialization(guild, bot);

        if (guildData == null || guildData.getConfig() == null) {
            throw new IllegalStateException("Config for guild " + guild.getIdLong() + " is still not initialized.");
        }

        this.userIntroMessagesCollection = guildData.getUserIntroMessagesCollection();
        this.blacklistCollection = guildData.getBlacklistCollection();
        this.userIntroMessages = new ConcurrentHashMap<>();

        loadUserIntroMessages();
    }

    private void loadUserIntroMessages() {
        List<Document> documents = userIntroMessagesCollection.find().into(new ArrayList<>());
        for (Document doc : documents) {
            String messageId = doc.getString("messageId");
            String userId = doc.getString("userId");

            // Skip if either messageId or userId is null
            if (messageId != null && userId != null) {
                userIntroMessages.put(messageId, userId);
            } else {
                System.err.println("Skipping null messageId or userId during initialization");
            }
        }
    }

    private GuildData waitForGuildDataInitialization(Guild guild, Redacted bot) {
        GuildData guildData;
        long timeout = 5000L; // Maximum wait time in milliseconds
        long startTime = System.currentTimeMillis();
        do {
            guildData = GuildData.get(guild, bot);
            if (guildData != null && guildData.getConfig() != null) {
                return guildData;
            }
            System.out.println("Waiting for GuildData/Config to initialize for guild: " + guild.getName());
            try {
                Thread.sleep(500); // Exponential backoff could be applied if needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (System.currentTimeMillis() - startTime < timeout);

        return null; // Return null if the initialization fails after the timeout
    }

    private void saveUserIntroMessage(String messageId, String userId) {
        Document doc = new Document("messageId", messageId).append("userId", userId);
        userIntroMessagesCollection.replaceOne(Filters.eq("messageId", messageId), doc, new ReplaceOptions().upsert(true));
    }

    private void removeUserIntroMessage(String messageId) {
        userIntroMessagesCollection.deleteOne(Filters.eq("messageId", messageId));
    }

    public void handleIntroduction(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();
        TextChannel staffChannel = event.getGuild().getTextChannelById(staffChannelId);
        Role memberRole = event.getGuild().getRoleById(memberRoleId);
        Role flagRole = event.getGuild().getRoleById(flagRoleId);

        if (member == null || staffChannel == null || memberRole == null || flagRole == null) {
            return;
        }

        String firstName = null;
        String lastName = null;
        String instagramHandle = null;
        String facebookHandle = null;
        String socialMediaPlatform = null;
        String socialMediaHandle = null;

        String[] lines = message.getContentRaw().split("\n");
        if (lines.length < 2) {
            return;
        }

        for (String line : lines) {
            if (line.toLowerCase().startsWith("name")) {
                // Handle "Name -" and "Name:" formats
                String name = line.contains(":") ? line.substring(line.indexOf(":") + 1).trim() : line.contains("-") ? line.substring(line.indexOf("-") + 1).trim() : "";
                String[] nameParts = name.split("/");
                firstName = nameParts[0].trim(); // Use only the first part before the slash
                lastName = nameParts.length > 1 ? nameParts[1].trim() : "None";
            } else if ((line.toLowerCase().startsWith("instagram") || line.toLowerCase().startsWith("insta") || line.toLowerCase().startsWith("ig") || line.toLowerCase().startsWith("ig tag:")) && !socialfound) {
                instagramHandle = line.substring(line.indexOf(":") + 1).trim().split(" ")[0]; // Only take the first part before a space
                socialMediaPlatform = "instagram";
                socialMediaHandle = instagramHandle;
                socialfound = true;
            } else if ((line.toLowerCase().contains("facebook") || line.toLowerCase().contains("fb")) && !socialfound) {
                facebookHandle = line.substring(line.indexOf(":") + 1).trim().split(" ")[0]; // Only take the first part before a space
                socialMediaPlatform = "facebook";
                socialMediaHandle = facebookHandle;
                socialfound = true;
            }
        }

        if (firstName == null) {
            if (shouldSendDM(member)) {
                member.getUser().openPrivateChannel().queue(channel ->
                        channel.sendMessage("Your introduction is incorrect, please include a name").queue()
                );
            }
            return;
        }

        if ((instagramHandle == null && facebookHandle == null) || socialMediaPlatform == null) {
            if (shouldSendDM(member)) {
                member.getUser().openPrivateChannel().queue(channel ->
                        channel.sendMessage("Your introduction is incorrect, please include a social media\nYou can use either a Facebook or Instagram account.").queue()
                );
            }
            return;
        }

        Document query = new Document("$or", List.of(
                new Document("firstName", new Document("$regex", Pattern.quote(firstName)).append("$options", "i"))
                        .append("lastName", new Document("$regex", Pattern.quote(lastName)).append("$options", "i")),
                new Document("socialmedia." + socialMediaPlatform, new Document("$regex", Pattern.quote(socialMediaHandle)).append("$options", "i"))
        ));
        Document blacklistedEntry = blacklistCollection.find(query).first();

        boolean isFlagged = false;
        String reason = "";

        try {
            if (blacklistedEntry != null) {
                isFlagged = true;
                reason = "blacklisted name or social media handle";
            } else {
                if (instagramHandle != null && SocialMediaUtils.isValidSocialMediaHandle("instagram", instagramHandle)) {
                    isFlagged = true;
                    reason = "invalid Instagram handle";
                } else if (facebookHandle != null && SocialMediaUtils.isValidSocialMediaHandle("facebook", facebookHandle)) {
                    isFlagged = true;
                    reason = "invalid Facebook handle";
                }
            }

            if (isFlagged) {
                event.getGuild().removeRoleFromMember(member, memberRole).queue();
                event.getGuild().addRoleToMember(member, flagRole).queue();
                event.getGuild().modifyNickname(member, null).queue();
                member.getUser().openPrivateChannel().queue(channel ->
                        channel.sendMessage("Your introduction has been flagged. A staff member will review your information.").queue());
                Role adminRole = new getRolesByName().getRoleByName(event.getGuild(), "Admin");
                if (adminRole != null) {
                    staffChannel.sendMessage(
                            String.format("%s User %s has been flagged due to %s. \nSocial Media: %s \nHandle: %s",
                                    adminRole.getAsMention(), member.getAsMention(), reason, socialMediaPlatform, socialMediaHandle)
                    ).queue();
                }
            } else {
                String nickname = firstName;
                if (instagramHandle != null) {
                    nickname = String.format("%s | @%s", firstName, instagramHandle);
                    System.out.println("Testing Nickname setting....");
                } else if (facebookHandle != null) {
                    nickname = String.format("%s | @%s", firstName, facebookHandle);
                }

                if (nickname.length() > 32) {
                    nickname = firstName;
                }

                System.out.println("Trying to set Nickname...");
                event.getGuild().modifyNickname(member, nickname).queue();
                System.out.println("Nickname set sucessfully!");
                event.getGuild().addRoleToMember(member, memberRole).queue();
                userIntroMessages.put(message.getId(), member.getId());
                saveUserIntroMessage(message.getId(), member.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldSendDM(Member member) {
        // Only send DM if the member does not have any role other than the default @everyone role
        return member.getRoles().stream().allMatch(Role::isPublicRole);
    }

    public void handleIntroductionDeletion(MessageDeleteEvent event) {
        TextChannel staffChannel = event.getGuild().getTextChannelById(staffChannelId);
        String messageId = event.getMessageId();
        if (staffDeletedMessages.containsKey(messageId)) {
            staffDeletedMessages.remove(messageId);
            return;
        }

        String userId = userIntroMessages.get(messageId);
        if (userId != null) {
            userIntroMessages.remove(messageId);
            removeUserIntroMessage(messageId);
            Member member = event.getGuild().getMemberById(userId);
            Role adminRole = new getRolesByName().getRoleByName(event.getGuild(), "Admin");
            if (adminRole != null && member != null) {
                assert staffChannel != null;
                staffChannel.sendMessage(
                        String.format("%s User %s has deleted their introduction.",
                                adminRole.getAsMention(), member.getAsMention())
                ).queue();
            }

            // Optionally kick the member if desired
            // event.getGuild().kick(member).queue();
        }
    }

    public static void markAsStaffDeleted(String messageId) {
        staffDeletedMessages.put(messageId, true);
    }
}
