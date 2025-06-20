package org.redacted.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
import java.util.Objects;

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
        if (Objects.requireNonNull(event.getUser()).isBot()) return;

        Guild guild = event.getGuild();
        Member member = guild.retrieveMember(event.getUser()).complete();
        String messageId = event.getMessageId();
        String emoji = event.getReaction().getEmoji().getName(); // works for unicode and named emoji

        // Load saved embed
        MongoCollection<SavedEmbed> collection = GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());
        Bson filter = Filters.eq("messageId", messageId);
        SavedEmbed saved = collection.find(filter).first();

        if (saved == null || saved.getEmojiRoleMap() == null) return;

        Map<String, String> emojiRoleMap = saved.getEmojiRoleMap();

        // Value now assumed to be role name
        String roleName = emojiRoleMap.get(emoji);
        if (roleName == null) return;

        Role roleToAssign = new getRolesByName().getRoleByName(guild, roleName);
        if (roleToAssign == null) return;

        if (addRole) {
            guild.addRoleToMember(member, roleToAssign).queue();
        } else {
            guild.removeRoleFromMember(member, roleToAssign).queue();
        }
    }
}

