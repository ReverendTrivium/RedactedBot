package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.redacted.Roles.RoleHierarchyManager;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class RoleManager {
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

    public void adjustRoleHierarchy(Guild guild, List<Role> rolesInOrder) {
        if (rolesInOrder == null || rolesInOrder.isEmpty()) {
            System.out.println("No roles provided to adjust hierarchy.");
            return;
        }

        RoleHierarchyManager roleHierarchyManager = new RoleHierarchyManager();
        roleHierarchyManager.adjustRoleHierarchy(guild, rolesInOrder);
    }
}
