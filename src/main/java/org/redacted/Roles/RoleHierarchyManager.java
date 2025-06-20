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
 * RoleHierarchyManager Class
 * Manages the role hierarchy in a Discord guild, ensuring that roles are positioned correctly
 * relative to each other and the bot's role.
 *
 * @author Derrick Eberlein
 */
public class RoleHierarchyManager {
    public static final EnumSet<Permission> ALL_PERMISSIONS = EnumSet.allOf(Permission.class);
    public static final EnumSet<Permission> NO_PERMISSIONS = EnumSet.noneOf(Permission.class);
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
     * Permissions for the developer role, which includes all permissions except for
     * managing roles and channels.
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
     * Permissions for the developer role, which includes all permissions except for
     * managing roles and channels.
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

    public static final Color MEMBER_COLOR = Color.decode("#4682B4");
    public static final Color DEVELOPER_COLOR = Color.decode("#71368a");
    public static final Color ADMIN_COLOR = Color.decode("#71368a");
    public static final Color HEAD_DJ_COLOR = Color.decode("#5944b9");
    public static final Color EVENT_STAFF_COLOR = Color.decode("#cb0f02");
    public static final Color FLAGGED_COLOR = Color.decode("#4682B4");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Adjusts the role hierarchy in a guild based on the provided list of roles.
     * The bot's role must be the top role in the hierarchy for this adjustment to take place.
     *
     * @param guild The guild where the roles are located.
     * @param rolesInOrder The list of roles in the desired order, starting with the top role.
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

        ensureRolePosition(guild, rolesInOrder.get(0), rolesInOrder, 1, botRole);
    }

    /**
     * Checks if the bot's role is the top role in the guild's role hierarchy.
     *
     * @param guild The guild to check.
     * @param botRole The bot's role.
     * @return true if the bot's role is the top role, false otherwise.
     */
    private boolean isBotRoleTop(Guild guild, Role botRole) {
        List<Role> roles = guild.getRoles();
        return roles.get(0).equals(botRole); // Check if the bot role is the top role in the hierarchy
    }

    /**
     * Ensures that the specified role is positioned correctly in the guild's role hierarchy.
     * It moves the role to be just below the specified previous role.
     *
     * @param guild The guild where the roles are located.
     * @param role The role to position.
     * @param rolesInOrder The list of roles in the desired order.
     * @param nextIndex The index of the next role to process in the list.
     * @param previousRole The role that should be above the specified role.
     */
    private void ensureRolePosition(Guild guild, Role role, List<Role> rolesInOrder, int nextIndex, Role previousRole) {
        try {
            System.out.println("Trying to move role positions...");
            int previousRolePosition = previousRole.getPosition();
            int rolePosition = role.getPosition();

            System.out.println("Role: " + role.getName() + " Position: " + rolePosition);
            System.out.println("Previous Role: " + previousRole.getName() + " Position: " + previousRolePosition);

            List<Role> roles = guild.getRoles();
            int previousRoleAscPos = roles.size() - previousRolePosition - 1;
            int roleAscPos = roles.size() - rolePosition - 1;

            System.out.println("Ascending Previous Role Position: " + previousRoleAscPos);
            System.out.println("Ascending Role Position: " + roleAscPos);

            if (roleAscPos == previousRoleAscPos + 1) {
                System.out.println("Role " + role.getName() + " is already below " + previousRole.getName());
                scheduleNext(guild, rolesInOrder, nextIndex, role, 0);
                return;
            }

            guild.modifyRolePositions()
                    .selectPosition(role)
                    .moveTo(previousRoleAscPos + 1)
                    .queue(success -> {
                        System.out.println("Successfully moved role " + role.getName() + " to position just below " + previousRole.getName() + " role.");
                        guild.loadMembers().onSuccess(members -> {
                            Role updatedRole = guild.getRoleById(role.getId());
                            if (updatedRole != null) {
                                System.out.println("New position for role " + updatedRole.getName() + " is " + updatedRole.getPosition());
                                scheduleNext(guild, rolesInOrder, nextIndex, updatedRole, 10);
                            } else {
                                System.out.println("Role not found in updated roles list.");
                            }
                        });
                    }, failure -> System.err.println("Failed to move role " + role.getName() + ": " + failure.getMessage()));
        } catch (Exception e) {
            System.err.println("Failed to adjust role position for " + role.getName());
            e.printStackTrace();
        }
    }

    /**
     * Schedules the next role position adjustment after a delay.
     *
     * @param guild The guild where the roles are located.
     * @param rolesInOrder The list of roles in the desired order.
     * @param nextIndex The index of the next role to process in the list.
     * @param aboveRole The role that should be above the next role.
     * @param delaySeconds The delay in seconds before processing the next role.
     */
    private void scheduleNext(Guild guild, List<Role> rolesInOrder, int nextIndex, Role aboveRole, int delaySeconds) {
        if (nextIndex < rolesInOrder.size()) {
            Role nextRole = rolesInOrder.get(nextIndex);
            scheduler.schedule(() -> ensureRolePosition(guild, nextRole, rolesInOrder, nextIndex + 1, aboveRole), delaySeconds, TimeUnit.SECONDS);
        }
    }
}
