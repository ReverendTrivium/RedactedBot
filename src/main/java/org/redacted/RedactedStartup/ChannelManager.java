package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.redacted.Roles.RoleHierarchyManager;

import java.util.Objects;

/**
 * ChannelManager Class
 * Manages the creation and permission settings of text channels and categories in a Discord guild.
 * It ensures that channels are created with the correct permissions based on their category.
 *
 * @author Derrick Eberlein
 */
public class ChannelManager {

    /**
     * Default constructor for ChannelManager.
     * Initializes a new instance of the ChannelManager class.
     */
    public ChannelManager() {
    }

    /**
     * Gets or creates a text channel in the specified guild with the given name and category.
     * If the channel already exists, it returns the existing channel.
     * If the channel does not exist, it creates a new one under the specified category.
     *
     * @param guild The guild where the text channel should be created or found.
     * @param name The name of the text channel to create or find.
     * @param categoryName The name of the category under which the text channel should be created.
     * @return The TextChannel object representing the created or found text channel.
     */
    public TextChannel getOrCreateTextChannel(Guild guild, String name, String categoryName) {
        Category category = getOrCreateCategory(guild, categoryName);
        TextChannel channel = guild.getTextChannelsByName(name, true).stream().findFirst().orElse(null);
        if (channel == null) {
            try {
                channel = guild.createTextChannel(name)
                        .setParent(category)
                        .complete();
                System.out.println("Created new text channel: " + name + " under category: " + category.getName());
            } catch (Exception e) {
                System.err.println("Failed to create text channel: " + name);
                e.printStackTrace();
            }
        }

        switch (categoryName) {
            case "Information" -> {
                try {
                    Objects.requireNonNull(channel).upsertPermissionOverride(guild.getPublicRole())
                            .grant(Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.VIEW_CHANNEL)
                            .queue();
                    System.out.println("Updated permissions for text channel: " + name + " under category: " + categoryName);
                } catch (Exception e) {
                    System.err.println("Failed to update permissions for text channel: " + name);
                    e.printStackTrace();
                }
            }
            case "Moderation", "Staff" -> {
                try {
                    RoleManager roleManager = new RoleManager();
                    Role adminRole = roleManager.getOrCreateRole(guild, "Admin", RoleHierarchyManager.ALL_PERMISSIONS, RoleHierarchyManager.ADMIN_COLOR);
                    Role headDJRole = roleManager.getOrCreateRole(guild, "Head DJ", RoleHierarchyManager.HEAD_DJ_PERMISSIONS, RoleHierarchyManager.HEAD_DJ_COLOR);
                    Role eventStaffRole = roleManager.getOrCreateRole(guild, "Event Staff", RoleHierarchyManager.EVENT_STAFF_PERMISSIONS, RoleHierarchyManager.EVENT_STAFF_COLOR);

                    Objects.requireNonNull(channel).upsertPermissionOverride(guild.getPublicRole())
                            .deny(Permission.VIEW_CHANNEL)
                            .queue();
                    channel.upsertPermissionOverride(adminRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();
                    channel.upsertPermissionOverride(headDJRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();
                    channel.upsertPermissionOverride(eventStaffRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();
                    System.out.println("Updated permissions for text channel: " + name + " under category: " + categoryName);
                } catch (Exception e) {
                    System.err.println("Failed to update permissions for text channel: " + name);
                    e.printStackTrace();
                }
            }
        }

        return channel;
    }

    /**
     * Gets or creates a category in the specified guild with the given name.
     * If the category already exists, it returns the existing category.
     * If the category does not exist, it creates a new one and sets appropriate permissions.
     *
     * @param guild The guild where the category should be created or found.
     * @param name The name of the category to create or find.
     * @return The Category object representing the created or found category.
     */
    public Category getOrCreateCategory(Guild guild, String name) {
        Category category = guild.getCategoriesByName(name, true).stream().findFirst().orElse(null);
        if (category == null) {
            try {
                category = guild.createCategory(name)
                        .complete();
                System.out.println("Created new category: " + name);
            } catch (Exception e) {
                System.err.println("Failed to create category: " + name);
                e.printStackTrace();
            }
        }

        switch (name) {
            case "Information" -> {
                try {
                    Objects.requireNonNull(category).upsertPermissionOverride(guild.getPublicRole())
                            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                            .queue();
                    System.out.println("Updated permissions for category: " + name);
                } catch (Exception e) {
                    System.err.println("Failed to update permissions for category: " + name);
                    e.printStackTrace();
                }
            }
            case "Moderation", "Staff" -> {
                try {
                    RoleManager roleManager = new RoleManager();
                    Role adminRole = roleManager.getOrCreateRole(guild, "Admin", RoleHierarchyManager.ALL_PERMISSIONS, RoleHierarchyManager.ADMIN_COLOR);
                    Role headDJRole = roleManager.getOrCreateRole(guild, "Head DJ", RoleHierarchyManager.HEAD_DJ_PERMISSIONS, RoleHierarchyManager.HEAD_DJ_COLOR);
                    Role eventStaffRole = roleManager.getOrCreateRole(guild, "Event Staff", RoleHierarchyManager.EVENT_STAFF_PERMISSIONS, RoleHierarchyManager.EVENT_STAFF_COLOR);

                    // Deny access to everyone
                    Objects.requireNonNull(category).upsertPermissionOverride(guild.getPublicRole())
                            .deny(Permission.VIEW_CHANNEL)
                            .queue();

                    // Grant access to specific staff roles
                    category.upsertPermissionOverride(adminRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();

                    category.upsertPermissionOverride(headDJRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();

                    category.upsertPermissionOverride(eventStaffRole)
                            .grant(Permission.VIEW_CHANNEL)
                            .queue();

                    System.out.println("Updated permissions for category: " + name);
                } catch (Exception e) {
                    System.err.println("Failed to update permissions for category: " + name);
                    e.printStackTrace();
                }
            }
        }

        return category;
    }
}

