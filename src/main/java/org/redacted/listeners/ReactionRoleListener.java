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

public class ReactionRoleListener extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        handleReaction(event, true);
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        handleReaction(event, false);
    }

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

