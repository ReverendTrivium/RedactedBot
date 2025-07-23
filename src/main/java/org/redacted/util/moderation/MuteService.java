package org.redacted.util.moderation;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bson.Document;
import org.redacted.Database.Data.GuildData;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing user mutes in a Discord guild.
 * Handles muting users by removing their roles, adding a mute role,
 * and scheduling unmute actions after a specified duration.
 * This service uses a singleton instance of the Database class to interact with MongoDB.
 *
 * @author Derrick Eberlein
 */
public class MuteService {
    private final RoleService roleService;
    private final GuildData guildData;

    /**
     * Constructor for MuteService.
     * Initializes the service with a RoleService instance and the Database singleton.
     *
     * @param roleService The RoleService instance to manage roles.
     */
    public MuteService(RoleService roleService, GuildData guildData) {
        this.roleService = roleService;
        this.guildData = guildData;

    }

    /**
     * Mutes a member in the specified guild for a given duration.
     * Removes all roles from the member, adds a mute role, and schedules an unmute action.
     *
     * @param target The member to mute.
     * @param muteRole The role to assign for muting.
     * @param duration The duration for which the member should be muted.
     * @param guild The guild in which the member is being muted.
     */
    public void mute(Member target, Role muteRole, Duration duration, Guild guild) {
        List<Role> oldRoles = roleService.removeAllRoles(guild, target);
        guild.addRoleToMember(target, muteRole).queue();

        List<String> oldRoleIds = oldRoles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        Instant now = Instant.now();
        Instant unmuteAt = now.plus(duration);

        Document doc = new Document("guildId", guild.getId())
                .append("userId", target.getId())
                .append("mutedAt", now.toString())
                .append("unmuteAt", unmuteAt.toString())
                .append("oldRoles", oldRoleIds);

        guildData.getMuteCollection(guild.getIdLong()).insertOne(doc);

        Executors.newSingleThreadScheduledExecutor().schedule(
                () -> unmute(guild, target.getId(), muteRole),
                duration.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Unmutes a user by restoring their previous roles and removing the mute role.
     * Also delete the mute record from the database.
     *
     * @param guild The guild from which the user is being unmuted.
     * @param userId The ID of the user to unmute.
     * @param muteRole The mute role to remove from the user.
     */
    public void unmute(Guild guild, String userId, Role muteRole) {
        MongoCollection<Document> muteCollection = guildData.getMuteCollection(guild.getIdLong());
        Document doc = muteCollection.find(Filters.eq("userId", userId)).first();
        if (doc == null) return;

        List<String> oldRoleIds = doc.getList("oldRoles", String.class);

        guild.retrieveMemberById(userId).queue(member -> {
            roleService.restoreRoles(guild, member, oldRoleIds);
            guild.removeRoleFromMember(member, muteRole).queue();
            muteCollection.deleteOne(Filters.eq("_id", doc.getObjectId("_id")));
        });
    }

    /**
     * Parses a duration string into a Duration object.
     * Accepts formats like "30m", "1h", "2d" for minutes, hours, and days respectively.
     *
     * @param input The duration string to parse.
     * @return A Duration object representing the parsed duration, or null if parsing fails.
     */
    public Duration parseDuration(String input) {
        try {
            if (input.endsWith("m")) return Duration.ofMinutes(Long.parseLong(input.replace("m", "")));
            if (input.endsWith("h")) return Duration.ofHours(Long.parseLong(input.replace("h", "")));
            if (input.endsWith("d")) return Duration.ofDays(Long.parseLong(input.replace("d", "")));
        } catch (NumberFormatException ignored) {}
        return null;
    }

    /**
     * Recovers scheduled unmutes from the database.
     * This method checks the mute collection for any users that should be unmuted
     * and schedules their unmuting if the time has not yet passed.
     *
     * @param guild The guild in which to recover scheduled unmutes.
     */
    public void recoverScheduledUnmutes(Guild guild) {
        MongoCollection<Document> muteCollection = guildData.getMuteCollection(guild.getIdLong());
        Instant now = Instant.now();

        FindIterable<Document> mutes = muteCollection.find();
        for (Document doc : mutes) {
            String userId = doc.getString("userId");
            Instant unmuteAt = Instant.parse(doc.getString("unmuteAt"));

            if (unmuteAt.isBefore(now)) {
                // Time has already passed — unmute immediately
                unmute(guild, userId, getMuteRole(guild));
            } else {
                // Time is in the future — schedule it
                long delay = Duration.between(now, unmuteAt).toMillis();
                Executors.newSingleThreadScheduledExecutor().schedule(
                        () -> unmute(guild, userId, getMuteRole(guild)),
                        delay,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    /**
     * Retrieves the mute role from the guild.
     * This method looks for a role named "Mute" in the guild's roles.
     *
     * @param guild The guild from which to retrieve the mute role.
     * @return The mute role if found, or null if not found.
     */
    private Role getMuteRole(Guild guild) {
        return guild.getRolesByName("Mute", true).stream().findFirst().orElse(null);
    }

}
