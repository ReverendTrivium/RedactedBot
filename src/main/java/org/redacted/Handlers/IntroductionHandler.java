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
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * IntroductionHandler Class
 * This class handles user introductions in a Discord guild.
 * It processes introduction messages, checks for blacklisted names or social media handles,
 * and manages user roles based on the introduction content.
 *
 * @author Derrick Eberlein
 */
public class IntroductionHandler {
    private final Redacted bot;
    private final String staffChannelId;
    private final String memberRoleId;
    private final String flagRoleId;
    private final MongoCollection<Document> userIntroMessagesCollection;
    private final MongoCollection<Document> blacklistCollection;
    private final ConcurrentMap<String, String> userIntroMessages;
    public static final ConcurrentMap<String, Boolean> staffDeletedMessages = new ConcurrentHashMap<>();

    /**
     * Constructs an IntroductionHandler for the specified guild.
     * It initializes the necessary collections and loads existing user introduction messages.
     *
     * @param bot                  The Redacted bot instance.
     * @param guild                The guild for which the introduction handler is being created.
     * @param introductionChannelId The ID of the channel where introductions are posted.
     * @param staffChannelId       The ID of the channel where staff notifications are sent.
     * @param memberRoleId         The ID of the role assigned to members after successful introduction.
     * @param flagRoleId           The ID of the role assigned to flagged users.
     */
    public IntroductionHandler(Redacted bot, Guild guild, String introductionChannelId, String staffChannelId, String memberRoleId, String flagRoleId) {
        this.bot = bot;
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

    /**
     * Loads existing user introduction messages from the database into memory.
     * This method is called during initialization to populate the userIntroMessages map.
     */
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

    /**
     * Waits for the GuildData and its configuration to be initialized.
     * This method will keep checking until the GuildData is available or a timeout occurs.
     *
     * @param guild The guild for which the data is being initialized.
     * @param bot   The Redacted bot instance.
     * @return The initialized GuildData, or null if initialization fails after the timeout.
     */
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

    /**
     * Saves a user introduction message to the database.
     * This method is called when a user posts an introduction message.
     *
     * @param messageId The ID of the introduction message.
     * @param userId    The ID of the user who posted the introduction.
     */
    private void saveUserIntroMessage(String messageId, String userId) {
        Document doc = new Document("messageId", messageId).append("userId", userId);
        userIntroMessagesCollection.replaceOne(Filters.eq("messageId", messageId), doc, new ReplaceOptions().upsert(true));
    }

    /**
     * Removes a user introduction message from the database.
     * This method is called when a user deletes their introduction message.
     *
     * @param messageId The ID of the introduction message to be removed.
     */
    private void removeUserIntroMessage(String messageId) {
        userIntroMessagesCollection.deleteOne(Filters.eq("messageId", messageId));
    }

    private String extractFieldValue(String line) {
        if (line == null) return "";

        int colonIndex = line.indexOf(':');
        int dashIndex = line.indexOf('-');

        if (colonIndex >= 0) {
            return line.substring(colonIndex + 1).trim();
        }

        if (dashIndex >= 0) {
            return line.substring(dashIndex + 1).trim();
        }

        return "";
    }

    /**
     * Checks if the provided value indicates that the user has no social media or does not want to provide it.
     * This method looks for common phrases that users might use to indicate they do not have social media or do not want to share it.
     *
     * @param value The input string to check.
     * @return true if the value indicates no social media, false otherwise.
     */
    private boolean isNoSocialValue(String value) {
        if (value == null) return false;

        String v = value.trim().toLowerCase();
        return v.contains("don't use")
                || v.contains("dont use")
                || v.contains("don’t use")
                || v.contains("do not use")
                || v.equals("none")
                || v.contains("no social")
                || v.contains("no insta")
                || v.contains("no instagram")
                || v.contains("no fb")
                || v.contains("no facebook")
                || v.contains("n/a")
                || v.contains("na");
    }

    /**
     * Normalizes a social media handle by extracting the relevant part from a raw input string.
     * This method handles various input formats, such as full URLs, handles with leading '@',
     * and removes any extraneous characters or query parameters.
     *
     * @param raw The raw input string containing the social media handle.
     * @return The normalized social media handle, or null if the input is invalid or empty.
     */
    private String normalizeHandle(String raw) {
        if (raw == null) return null;

        String handle = raw.trim();

        if (handle.isEmpty()) return null;

        // If a full URL is pasted, pull the last path segment
        handle = handle.replace("\\", "/");

        if (handle.contains("instagram.com/")) {
            handle = handle.substring(handle.indexOf("instagram.com/") + "instagram.com/".length());
        } else if (handle.contains("facebook.com/")) {
            handle = handle.substring(handle.indexOf("facebook.com/") + "facebook.com/".length());
        }

        // Remove query strings / fragments
        int qIndex = handle.indexOf('?');
        if (qIndex >= 0) handle = handle.substring(0, qIndex);

        int hashIndex = handle.indexOf('#');
        if (hashIndex >= 0) handle = handle.substring(0, hashIndex);

        // Remove leading @
        while (handle.startsWith("@")) {
            handle = handle.substring(1).trim();
        }

        // Remove trailing slash
        while (handle.endsWith("/")) {
            handle = handle.substring(0, handle.length() - 1).trim();
        }

        // If user typed extra words after the handle, keep first token
        if (handle.contains(" ")) {
            handle = handle.split("\\s+")[0].trim();
        }

        return handle.isBlank() ? null : handle;
    }

    /**
     * Handles the introduction message received event.
     * It processes the introduction, checks for blacklisted names or social media handles,
     * and manages user roles based on the introduction content.
     *
     * @param event The MessageReceivedEvent containing the introduction message.
     */
    public void handleIntroduction(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();
        TextChannel staffChannel = event.getGuild().getTextChannelById(staffChannelId);
        Role memberRole = event.getGuild().getRoleById(memberRoleId);
        Role flagRole = event.getGuild().getRoleById(flagRoleId);

        if (member == null || staffChannel == null || memberRole == null || flagRole == null) {
            return;
        }

        // Reset the flag for each message
        boolean socialfound = false;
        boolean noSocialProvided = false;

        String firstName = null;
        String lastName = null;
        String instagramHandle = null;
        String facebookHandle = null;
        String socialMediaPlatform = null;
        String socialMediaHandle = null;

        String[] lines = message.getContentRaw().split("\n");
        if (lines.length < 1) {
            sendDM(member, "Your introduction is incorrect, please include all required fields.");
            return;
        }

        for (String line : lines) {
            if (line == null || line.isBlank()) continue;

            String lowerLine = line.toLowerCase().trim();
            String value = extractFieldValue(line);

            // Name can appear anywhere
            if (lowerLine.startsWith("name")) {
                String[] nameParts = value.split("/");
                firstName = nameParts.length > 0 ? nameParts[0].trim() : null;
                lastName = nameParts.length > 1 ? nameParts[1].trim() : "None";
                continue;
            }

            // Generic "social media:" line
            if (lowerLine.startsWith("social media") || lowerLine.startsWith("socialmedia")) {
                if (isNoSocialValue(value)) {
                    socialfound = true;
                    noSocialProvided = true;
                    socialMediaPlatform = "none";
                    socialMediaHandle = null;
                    continue;
                }

                String normalized = normalizeHandle(value);
                if (normalized != null) {
                    // Default generic social media field to Instagram first
                    instagramHandle = normalized;
                    socialMediaPlatform = "instagram";
                    socialMediaHandle = normalized;
                    socialfound = true;
                }
                continue;
            }

            // Instagram / Insta / IG lines
            if (lowerLine.startsWith("instagram")
                    || lowerLine.startsWith("insta")
                    || lowerLine.startsWith("ig")
                    || lowerLine.startsWith("ig tag")) {

                if (isNoSocialValue(value)) {
                    socialfound = true;
                    noSocialProvided = true;
                    socialMediaPlatform = "none";
                    socialMediaHandle = null;
                    continue;
                }

                String normalized = normalizeHandle(value);
                if (normalized != null) {
                    instagramHandle = normalized;
                    socialMediaPlatform = "instagram";
                    socialMediaHandle = normalized;
                    socialfound = true;
                }
                continue;
            }

            // Facebook / FB lines
            if (lowerLine.startsWith("facebook") || lowerLine.startsWith("fb")) {
                if (isNoSocialValue(value)) {
                    socialfound = true;
                    noSocialProvided = true;
                    socialMediaPlatform = "none";
                    socialMediaHandle = null;
                    continue;
                }

                String normalized = normalizeHandle(value);
                if (normalized != null) {
                    facebookHandle = normalized;
                    socialMediaPlatform = "facebook";
                    socialMediaHandle = normalized;
                    socialfound = true;
                }
                continue;
            }

            // Fallback: if a line anywhere says they do not use social media, honor it
            if (!socialfound && isNoSocialValue(lowerLine)) {
                socialfound = true;
                noSocialProvided = true;
                socialMediaPlatform = "none";
                socialMediaHandle = null;
            }
        }

        if (firstName == null || firstName.isEmpty()) {
            sendDM(member, "Your introduction is incorrect, please include a name.");
            return;
        }

        if (noSocialProvided) {
            IntroductionValidatorFallback.handleNoSocialManualApproval(
                    member,
                    staffChannel,
                    firstName,
                    lastName
            );
            return;
        }

        if ((instagramHandle == null && facebookHandle == null) || socialMediaPlatform == null) {
            sendDM(member, "Your introduction is incorrect, please include a social media.\nYou can use either a Facebook or Instagram account.");
            return;
        }

        // Check if the user is blacklisted
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
                // Validate social media handles
                if (instagramHandle != null) {
                    Boolean result = SocialMediaUtils.isValidSocialMediaHandle("instagram", instagramHandle);

                    if (Boolean.FALSE.equals(result)) {
                        isFlagged = true;
                        reason = "invalid Instagram handle";
                    } else if (result == null) {
                        IntroductionValidatorFallback.handleManualFallback(instagramHandle, member, staffChannel, firstName, instagramHandle, null);
                        // Return early so no nickname/role assignment is attempted
                        return;
                    }
                } else if (facebookHandle != null) {
                    Boolean result = SocialMediaUtils.isValidSocialMediaHandle("facebook", facebookHandle);

                    if (Boolean.FALSE.equals(result)) {
                        isFlagged = true;
                        reason = "invalid Facebook handle";
                    } else if (result == null) {
                        IntroductionValidatorFallback.handleManualFallback(facebookHandle, member, staffChannel, firstName, null, facebookHandle);
                        // Return early so no nickname/role assignment is attempted
                        return;
                    }
                }
            }

            // Decide whether to flag or approve the user
            if (isFlagged) {
                flagUser(event, member, staffChannel, memberRole, flagRole, socialMediaPlatform, socialMediaHandle, reason, firstName, instagramHandle, facebookHandle);
            } else {
                approveUser(event, member, memberRole, firstName, instagramHandle, facebookHandle);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flags a user for review based on their introduction content.
     * It removes the member role, adds the flag role, and notifies staff in the staff channel.
     *
     * @param event                The MessageReceivedEvent containing the introduction message.
     * @param member               The member being flagged.
     * @param staffChannel         The channel where staff notifications are sent.
     * @param memberRole           The role assigned to members after successful introduction.
     * @param flagRole             The role assigned to flagged users.
     * @param socialMediaPlatform  The social media platform used by the user.
     * @param socialMediaHandle    The social media handle provided by the user.
     * @param reason               The reason for flagging the user.
     */
    private void flagUser(MessageReceivedEvent event, Member member, TextChannel staffChannel, Role memberRole, Role flagRole, String socialMediaPlatform, String socialMediaHandle, String reason, String firstName, String instagramHandle, String facebookHandle) {
        event.getGuild().removeRoleFromMember(member, memberRole).queue();
        event.getGuild().addRoleToMember(member, flagRole).queue();
        event.getGuild().modifyNickname(member, null).queue();
        sendDM(member, "Your introduction has been flagged. A staff member will review your information.");
        Role adminRole = new getRolesByName().getRoleByName(event.getGuild(), "Admin");
        if (adminRole != null) {
            staffChannel.sendMessage(String.format(
                    "%s User %s has been flagged due to %s.\nSocial Media: %s\nHandle: %s",
                    adminRole.getAsMention(), member.getAsMention(), reason, socialMediaPlatform, socialMediaHandle
            )).queue(msg -> {
                msg.addReaction(Emoji.fromUnicode("✅")).queue();

                // Use msg here instead of undefined variable
                ManualVerificationTracker.addPendingVerification(msg.getIdLong(), member.getIdLong(),
                        firstName, instagramHandle, facebookHandle);
            });
        }
    }

    /**
     * Approves a user based on their introduction content.
     * It sets the user's nickname, adds the member role, and saves the introduction message.
     *
     * @param event                The MessageReceivedEvent containing the introduction message.
     * @param member               The member being approved.
     * @param memberRole           The role assigned to members after successful introduction.
     * @param firstName            The first name of the user.
     * @param instagramHandle      The Instagram handle provided by the user, if any.
     * @param facebookHandle       The Facebook handle provided by the user, if any.
     */
    public void approveUser(MessageReceivedEvent event, Member member, Role memberRole, String firstName, String instagramHandle, String facebookHandle) {
        // Debugging output
        System.out.println("Approving user: " + member.getUser().getName() + " with first name: " + firstName);
        System.out.println("Instagram handle: " + instagramHandle);
        System.out.println("Facebook handle: " + facebookHandle);

        // Set the nickname based on the first name and social media handles
        String nickname;

        if ((instagramHandle == null || instagramHandle.isBlank()) &&
                (facebookHandle == null || facebookHandle.isBlank())) {

            // Set nickname to just first name if no social media provided
            nickname = firstName;

        } else {
            // Prefer Instagram handle for nickname if available, otherwise use Facebook handle
            String handle = instagramHandle != null && !instagramHandle.isBlank()
                    ? instagramHandle
                    : facebookHandle;

            nickname = firstName + " | @" + handle;
        }

        // Fallback to effective name if nickname is null/blank for any reason
        if (nickname == null || nickname.isBlank()) {
            nickname = member.getEffectiveName();
        }

        // Ensure nickname does not exceed Discord's 32 character limit
        if (nickname.length() > 32) {
            nickname = firstName != null && !firstName.isBlank()
                    ? firstName
                    : member.getEffectiveName();
        }

        event.getGuild().modifyNickname(member, nickname).queue();
        event.getGuild().addRoleToMember(member, memberRole).queue();

        userIntroMessages.put(event.getMessage().getId(), member.getId());
        saveUserIntroMessage(event.getMessage().getId(), member.getId());

        // Print Introduction Message ID:
        System.out.println("User Intro Message ID: " + event.getMessage().getId());
    }

    /**
     * Sends a direct message to the user.
     *
     * @param member  The member to whom the DM will be sent.
     * @param message The message content to send.
     */
    private void sendDM(Member member, String message) {
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessage(message).queue());
    }

    /**
     * Checks if a direct message should be sent to the user based on their roles.
     *
     * @param member The member to check.
     * @return true if the DM should be sent, false otherwise.
     */
    private boolean shouldSendDM(Member member) {
        // Only send DM if the member does not have any role other than the default @everyone role
        return member.getRoles().stream().allMatch(Role::isPublicRole);
    }

    /**
     * Handles the deletion of an introduction message.
     * It checks if the deleted message was a user introduction and not a bot-generated sticky message.
     * If it was a user introduction, it removes the user's role and sends a notification to the staff channel.
     *
     * @param event The MessageDeleteEvent containing the deleted message details.
     */
    public void handleIntroductionDeletion(MessageDeleteEvent event) {
        TextChannel staffChannel = event.getGuild().getTextChannelById(staffChannelId);
        String messageId = event.getMessageId();

        // Skip handling deletion for bot-generated messages
        GuildData guildData = GuildData.get(event.getGuild(), bot);
        Document stickyMessageDoc = guildData.getStickyMessagesCollection().find(new Document("messageId", Long.parseLong(messageId))).first();
        if (stickyMessageDoc != null && stickyMessageDoc.getBoolean("isBotGenerated", false)) {
            return; // Ignore bot-generated sticky messages
        }

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
                Objects.requireNonNull(staffChannel).sendMessage(
                        String.format("%s User %s has deleted their introduction.",
                                adminRole.getAsMention(), member.getAsMention())
                ).queue();
            }

            // Remove member Role from User
            Role memberRole = event.getGuild().getRoleById(memberRoleId);
            event.getGuild().removeRoleFromMember(Objects.requireNonNull(member), Objects.requireNonNull(memberRole)).queue();

            // Send DM to User telling them they lost access to server
            // due to deleting their introduction, and to redo it if they want access again.
            member.getUser().openPrivateChannel().queue(channel ->
                    channel.sendMessage("You deleted your introduction, if you want access to the server again, repost your introduction.").queue());

            // Optionally kick the member if desired
            // event.getGuild().kick(member).queue();
        }
    }

    /**
     * Marks a message as deleted by staff.
     * This method is used to track messages that have been deleted by staff members.
     *
     * @param messageId The ID of the message to mark as deleted.
     */
    public static void markAsStaffDeleted(String messageId) {
        staffDeletedMessages.put(messageId, true);
    }
}