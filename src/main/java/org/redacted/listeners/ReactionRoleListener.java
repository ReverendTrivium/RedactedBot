package org.redacted.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Roles.getRolesByName;

import java.util.Map;

/**
 * ReactionRoleListener Class
 * This class listens for reaction events on messages and assigns or removes roles
 * based on the reactions.
 * It is designed to work with saved embeds that have a mapping of emojis to role names.
 *
 * @author Derrick Eberlein
 */
public class ReactionRoleListener extends ListenerAdapter {

    /**
     * Handles message reaction events.
     * If the reaction is added, it assigns the corresponding role to the user.
     * If the reaction is removed, it removes the corresponding role from the user.
     *
     * @param event The GenericMessageReactionEvent containing the reaction details.
     */
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        handleReaction(event, true);
    }

    /**
     * Handles message reaction removal events.
     * If the reaction is removed, it removes the corresponding role from the user.
     *
     * @param event The MessageReactionRemoveEvent containing the reaction removal details.
     */
    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        handleReaction(event, false);
    }

    /**
     * Handles the reaction event and assigns or removes roles based on the reaction emoji.
     *
     * @param event The GenericMessageReactionEvent containing the reaction details.
     * @param addRole True if the role should be added, false if it should be removed.
     */
    private void handleReaction(GenericMessageReactionEvent event, boolean addRole) {
        if (event.getUser() == null || event.getUser().isBot()) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = guild.retrieveMember(event.getUser()).complete();
        if (member == null) {
            System.out.println("Member not found for user: " + event.getUserId());
            return;
        }

        String messageId = event.getMessageId();
        EmojiUnion reactionEmoji = event.getReaction().getEmoji();

        String emojiKey = getEmojiKey(reactionEmoji);

        System.out.println("Handling reaction event: " + (addRole ? "Add" : "Remove"));
        System.out.println("Message ID: " + messageId);
        System.out.println("Emoji key: " + emojiKey);

        MongoCollection<SavedEmbed> collection =
                GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());

        Bson filter = Filters.eq("messageId", messageId);
        SavedEmbed saved = collection.find(filter).first();

        if (saved == null) {
            System.out.println("No saved embed found for message ID: " + messageId);
            return;
        }

        if (saved.getEmojiRoleMap() == null) {
            System.out.println("Emoji role map is null for message ID: " + messageId);
            return;
        }

        Map<String, String> emojiRoleMap = saved.getEmojiRoleMap();
        String roleName = emojiRoleMap.get(emojiKey);

        if (roleName == null) {
            System.out.println("No role mapping found for emoji key: " + emojiKey);
            return;
        }

        Role roleToAssign = new getRolesByName().getRoleByName(guild, roleName);
        if (roleToAssign == null) {
            System.out.println("Role not found: " + roleName);
            return;
        }

        if (addRole) {
            System.out.println("Adding role " + roleToAssign.getName() + " to " + member.getEffectiveName());
            guild.addRoleToMember(member, roleToAssign).queue(
                    success -> System.out.println("Role added successfully."),
                    error -> {
                        System.out.println("Failed to add role:");
                        error.printStackTrace();
                    }
            );
        } else {
            System.out.println("Removing role " + roleToAssign.getName() + " from " + member.getEffectiveName());
            guild.removeRoleFromMember(member, roleToAssign).queue(
                    success -> System.out.println("Role removed successfully."),
                    error -> {
                        System.out.println("Failed to remove role:");
                        error.printStackTrace();
                    }
            );
        }
    }

    /**
     * Generates a unique key for the given emoji, distinguishing between custom and unicode emojis.
     * For custom emojis, the key is in the format "custom:emojiId".
     * For unicode emojis, the key is in the format "unicode:emojiName".
     * This allows for consistent mapping of emojis to roles in the database.
     *
     * @param emoji The EmojiUnion object representing the emoji used in the reaction.
     * @return A string key that uniquely identifies the emoji for role mapping purposes.
     */
    private String getEmojiKey(EmojiUnion emoji) {
        if (emoji.getType() == EmojiUnion.Type.CUSTOM) {
            return "custom:" + emoji.asCustom().getId();
        } else {
            return "unicode:" + emoji.getName();
        }

    }
}