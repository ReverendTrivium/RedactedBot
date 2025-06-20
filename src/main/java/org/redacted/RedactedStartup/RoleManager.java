package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.redacted.Roles.RoleHierarchyManager;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

/**
 * RoleManager Class
 * Manages the creation and permission settings of roles in a Discord guild.
 * It ensures that roles are created with the correct permissions and color, and can adjust role hierarchy.
 *
 * @author Derrick Eberlein
 */
public class RoleManager {

    /**
     * Default constructor for RoleManager.
     * Initializes a new instance of the RoleManager class.
     */
    public Role getOrCreateRole(Guild guild, String name, EnumSet<Permission> permissions, Color color) {
        Role role = guild.getRolesByName(name, true).stream().findFirst().orElse(null);
        Role botRole = guild.getBotRole();

        if (role == null) {
            try {
                role = guild.createRole()
                        .setName(name)
                        .setPermissions(permissions)
                        .setColor(color)
                        .setHoisted(true) // Display role separately from other online members
                        .complete();
                System.out.println("Created new role: " + name);
            } catch (Exception e) {
                System.err.println("Failed to create role: " + name);
                e.printStackTrace();
            }
        } else {
            // Check hierarchy before updating
            if (botRole != null && botRole.canInteract(role)) {
                // Update the role if it already exists to ensure it has the correct permissions and color
                boolean updateRequired = !role.getPermissions().containsAll(permissions);
                Color currentColor = role.getColor();
                if (currentColor == null || !currentColor.equals(color)) {
                    updateRequired = true;
                }
                if (!role.isHoisted()) {
                    updateRequired = true;
                }
                if (updateRequired) {
                    try {
                        role.getManager()
                                .setPermissions(permissions)
                                .setColor(color)
                                .setHoisted(true) // Display role separately from other online members
                                .queue();
                        System.out.println("Updated role: " + name);
                    } catch (Exception e) {
                        System.err.println("Failed to update role: " + name);
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Skipping update for role: " + name + " as it is higher or equal in hierarchy to the bot's role.");
            }
        }
        return role;
    }

    /**
     * Adjusts the role hierarchy in the guild based on the provided list of roles.
     * The roles should be provided in the desired order of hierarchy.
     *
     * @param guild The guild where the role hierarchy should be adjusted.
     * @param rolesInOrder The list of roles in the desired order of hierarchy.
     */
    public void adjustRoleHierarchy(Guild guild, List<Role> rolesInOrder) {
        if (rolesInOrder == null || rolesInOrder.isEmpty()) {
            System.out.println("No roles provided to adjust hierarchy.");
            return;
        }

        RoleHierarchyManager roleHierarchyManager = new RoleHierarchyManager();
        roleHierarchyManager.adjustRoleHierarchy(guild, rolesInOrder);
    }
}
