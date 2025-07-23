package org.redacted.Roles;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RoleHierarchyManager manages the role hierarchy in a Discord guild.
 * It ensures that roles are positioned correctly based on a specified order,
 * with the bot's role at the top of the hierarchy.
 * It also defines various permission sets and colors for different roles.
 * * This class is designed to be used in a Discord bot context, where roles and permissions
 * are managed through the JDA (Java Discord API).
 *
 * @author Derrick Eberlein
 */
public class RoleHierarchyManager {
    /**
     * A set containing all permissions available in Discord.
     * This can be used to grant all permissions to a role if needed.
     */
    public static final EnumSet<Permission> ALL_PERMISSIONS = EnumSet.allOf(Permission.class);

    /**
     * An empty set of permissions, used when no permissions are granted.
     * This can be useful for roles that should not have any permissions.
     */
    public static final EnumSet<Permission> NO_PERMISSIONS = EnumSet.noneOf(Permission.class);

    /**
     * Permissions granted to the Head DJ role in the guild.
     * This set includes permissions that are typically granted to the Head DJ.
     */
    public static final EnumSet<Permission> HEAD_DJ_PERMISSIONS = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.CREATE_INSTANT_INVITE,
            Permission.KICK_MEMBERS,
            Permission.NICKNAME_CHANGE,
            Permission.NICKNAME_MANAGE,
            Permission.MODERATE_MEMBERS,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_TTS,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_MENTION_EVERYONE,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.USE_APPLICATION_COMMANDS,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS,
            Permission.MESSAGE_SEND_POLLS,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.VOICE_MUTE_OTHERS,
            Permission.VOICE_DEAF_OTHERS,
            Permission.VOICE_MOVE_OTHERS,
            Permission.VOICE_USE_VAD,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_USE_EXTERNAL_SOUNDS,
            Permission.VOICE_SET_STATUS
    );

    /**
     * Permissions granted to members in the guild.
     * This set includes permissions that are typically granted to regular members.
     */
    public static final EnumSet<Permission> MEMBER_PERMISSIONS = EnumSet.of(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.VIEW_CHANNEL,
            Permission.CREATE_INSTANT_INVITE,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_EXT_STICKER,
            Permission.VOICE_CONNECT,
            Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
            Permission.VOICE_SPEAK,
            Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_EXTERNAL_SOUNDS
    );

    /**
     * Permissions granted to event staff in the guild.
     * This set includes permissions that are typically granted to event staff members.
     */
    public static final EnumSet<Permission> EVENT_STAFF_PERMISSIONS = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.CREATE_INSTANT_INVITE,
            Permission.KICK_MEMBERS,
            Permission.NICKNAME_CHANGE,
            Permission.NICKNAME_MANAGE,
            Permission.MODERATE_MEMBERS,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_TTS,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_MENTION_EVERYONE,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.USE_APPLICATION_COMMANDS,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS,
            Permission.MESSAGE_SEND_POLLS,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.VOICE_MUTE_OTHERS,
            Permission.VOICE_DEAF_OTHERS,
            Permission.VOICE_MOVE_OTHERS,
            Permission.VOICE_USE_VAD,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_USE_EXTERNAL_SOUNDS,
            Permission.VOICE_SET_STATUS
    );

    /**
     * Colors associated with different roles in the guild.
     */
    public static final Color MEMBER_COLOR = Color.decode("#4682B4");
    public static final Color DEVELOPER_COLOR = Color.decode("#71368a");
    public static final Color ADMIN_COLOR = Color.decode("#71368a");
    public static final Color HEAD_DJ_COLOR = Color.decode("#5944b9");
    public static final Color EVENT_STAFF_COLOR = Color.decode("#cb0f02");
    public static final Color FLAGGED_COLOR = Color.decode("#4682B4");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Adjusts the role hierarchy in the specified guild based on the provided list of roles.
     * The bot's role must be at the top of the hierarchy for this adjustment to proceed.
     *
     * @param guild          The guild where the roles are located.
     * @param rolesInOrder   The list of roles in the desired order, starting with the top role.
     */
    public void adjustRoleHierarchy(Guild guild, List<Role> rolesInOrder) {
        if (rolesInOrder == null || rolesInOrder.isEmpty()) {
            System.out.println("No roles provided to adjust hierarchy.");
            return;
        }

        Role botRole = guild.getBotRole();
        if (botRole == null) {
            System.out.println("Bot role not found!");
            return;
        }

        // Check if the bot role is the top role
        if (!isBotRoleTop(guild, botRole)) {
            System.out.println("Bot role is not the top role. Skipping role hierarchy adjustment.");
            return;
        }

        System.out.println("Role hierarchy adjustment started for guild: " + guild.getName());
        ensureRolePosition(guild, rolesInOrder.get(0), rolesInOrder, 1, botRole);
        System.out.println("Role hierarchy adjustment finished for guild: " + guild.getName());
    }

    /**
     * Checks if the bot role is the top role in the guild's role hierarchy.
     *
     * @param guild    The guild where the roles are located.
     * @param botRole  The bot's role to check.
     * @return true if the bot role is at the top of the hierarchy, false otherwise.
     */
    private boolean isBotRoleTop(Guild guild, Role botRole) {
        List<Role> roles = guild.getRoles();
        return roles.get(0).equals(botRole); // Check if the bot role is the top role in the hierarchy
    }

    /**
     * Ensures that the specified role is positioned just below the previous role in the hierarchy.
     *
     * @param guild         The guild where the roles are located.
     * @param role          The role to be positioned.
     * @param rolesInOrder  The list of roles in the desired order.
     * @param nextIndex     The index of the next role to adjust.
     * @param previousRole  The role above which the current role should be positioned.
     */
    private void ensureRolePosition(Guild guild, Role role, List<Role> rolesInOrder, int nextIndex, Role previousRole) {
        try {
            //System.out.println("Trying to move role positions...");
            int previousRolePosition = previousRole.getPosition();
            int rolePosition = role.getPosition();

            //System.out.println("Role: " + role.getName() + " Position: " + rolePosition);
            //System.out.println("Previous Role: " + previousRole.getName() + " Position: " + previousRolePosition);

            List<Role> roles = guild.getRoles();
            int previousRoleAscPos = roles.size() - previousRolePosition - 1;
            int roleAscPos = roles.size() - rolePosition - 1;

            //System.out.println("Ascending Previous Role Position: " + previousRoleAscPos);
            //System.out.println("Ascending Role Position: " + roleAscPos);

            if (roleAscPos == previousRoleAscPos + 1) {
                //System.out.println("Role " + role.getName() + " is already below " + previousRole.getName());
                scheduleNext(guild, rolesInOrder, nextIndex, role, 0);
                return;
            }

            guild.modifyRolePositions()
                    .selectPosition(role)
                    .moveTo(previousRoleAscPos + 1)
                    .queue(success -> {
                        //System.out.println("Successfully moved role " + role.getName() + " to position just below " + previousRole.getName() + " role.");
                        guild.loadMembers().onSuccess(members -> {
                            Role updatedRole = guild.getRoleById(role.getId());
                            if (updatedRole != null) {
                                //System.out.println("New position for role " + updatedRole.getName() + " is " + updatedRole.getPosition());
                                scheduleNext(guild, rolesInOrder, nextIndex, updatedRole, 10);
                            } else {
                                System.out.println("Role not found in updated roles list.");
                            }
                        });
                    }, failure -> System.err.println("Failed to move role " + role.getName() + ": " + failure.getMessage()));
            System.out.println("Successfully Moved Role Positions...");
        } catch (Exception e) {
            System.err.println("Failed to adjust role position for " + role.getName());
            e.printStackTrace();
        }
    }

    /**
     * Schedules the next role position adjustment after a delay.
     *
     * @param guild         The guild where the roles are located.
     * @param rolesInOrder  The list of roles in the desired order.
     * @param nextIndex     The index of the next role to adjust.
     * @param aboveRole     The role above which the next role should be positioned.
     * @param delaySeconds  The delay in seconds before the next adjustment.
     */
    private void scheduleNext(Guild guild, List<Role> rolesInOrder, int nextIndex, Role aboveRole, int delaySeconds) {
        if (nextIndex < rolesInOrder.size()) {
            Role nextRole = rolesInOrder.get(nextIndex);
            scheduler.schedule(() -> ensureRolePosition(guild, nextRole, rolesInOrder, nextIndex + 1, aboveRole), delaySeconds, TimeUnit.SECONDS);
        }
    }
}
