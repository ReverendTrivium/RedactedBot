package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.redacted.Commands.BotCommands;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Config;
import org.redacted.Redacted;
import org.redacted.Roles.RoleHierarchyManager;

import java.util.ArrayList;
import java.util.Arrays;

public class BotEventListener extends ListenerAdapter {

    private final Redacted bot;
    private final BotCommands botCommands; // Reference to the BotCommands instance

    public BotEventListener(Redacted bot, BotCommands botCommands) {
        this.bot = bot;
        this.botCommands = botCommands;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready and connected as " + event.getJDA().getSelfUser().getName());

        // Initialize for all guilds the bot is already in
        for (Guild guild : event.getJDA().getGuilds()) {
            initializeGuild(guild);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        System.out.println("Joined new guild: " + guild.getName());

        // Initialize the newly joined guild
        initializeGuild(guild);
    }

    private void initializeGuild(Guild guild) {
        // Step 1: Initialize the database for the new guild
        setupDatabaseForGuild(guild);

        // Step 2: Set up roles for the guild
        setupRolesForGuild(guild);

        // Step 3: Register commands for the guild using BotCommands
        botCommands.registerCommandsForGuild(guild);

        System.out.println("Setup completed for guild: " + guild.getName());
    }

    private void setupDatabaseForGuild(Guild guild) {
        System.out.println("Setting up database for guild: " + guild.getName());

        // Check and create default configuration if it doesn't exist
        Config guildConfig = bot.database.config.find(new Document("guildId", guild.getIdLong())).first();
        if (guildConfig == null) {
            // Create default config for the guild
            Config defaultConfig = new Config(guild.getIdLong());
            bot.database.config.insertOne(defaultConfig);
            System.out.println("Created default configuration for guild: " + guild.getName());
        } else {
            System.out.println("Configuration already exists for guild: " + guild.getName());
        }

        // Check and create a greetings collection entry if it doesn't exist
        bot.database.initializeGreetingsForGuild(guild.getIdLong());

        // Initialize blacklist collection for the guild if needed
        Document blacklistEntry = GuildData.get(guild, bot).getBlacklistCollection().find(new Document("guildId", guild.getIdLong())).first();
        if (blacklistEntry == null) {
            // Initialize an empty blacklist entry for the guild
            Document newBlacklistEntry = new Document("guildId", guild.getIdLong()).append("blacklist", new ArrayList<>());
            GuildData.get(guild, bot).getBlacklistCollection().insertOne(newBlacklistEntry);
            System.out.println("Initialized blacklist for guild: " + guild.getName());
        } else {
            System.out.println("Blacklist already exists for guild: " + guild.getName());
        }

        // Initialize other necessary collections (user_intro_message, sticky_messages, etc.)
        Document introMessageEntry = GuildData.get(guild, bot).getUserIntroMessagesCollection().find(new Document("guildId", guild.getIdLong())).first();
        if (introMessageEntry == null) {
            Document newIntroMessageEntry = new Document("guildId", guild.getIdLong()).append("messages", new ArrayList<>());
            GuildData.get(guild, bot).getUserIntroMessagesCollection().insertOne(newIntroMessageEntry);
            System.out.println("Initialized introduction messages for guild: " + guild.getName());
        }

        Document stickyMessageEntry = GuildData.get(guild, bot).getStickyMessagesCollection().find(new Document("guildId", guild.getIdLong())).first();
        if (stickyMessageEntry == null) {
            Document newStickyMessageEntry = new Document("guildId", guild.getIdLong()).append("stickyMessages", new ArrayList<>());
            GuildData.get(guild, bot).getStickyMessagesCollection().insertOne(newStickyMessageEntry);
            System.out.println("Initialized sticky messages for guild: " + guild.getName());
        }
    }

    private void setupRolesForGuild(Guild guild) {
        System.out.println("Setting up roles for guild: " + guild.getName());

        RoleManager roleManager = new RoleManager();

        // Create or get required roles
        roleManager.getOrCreateRole(guild, "Member", RoleHierarchyManager.MEMBER_PERMISSIONS, RoleHierarchyManager.MEMBER_COLOR);
        roleManager.getOrCreateRole(guild, "Flagged", RoleHierarchyManager.NO_PERMISSIONS, RoleHierarchyManager.FLAGGED_COLOR);

        // Adjust role hierarchy
        RoleHierarchyManager roleHierarchyManager = new RoleHierarchyManager();
        roleHierarchyManager.adjustRoleHierarchy(guild, Arrays.asList(
                roleManager.getOrCreateRole(guild, "Admin", RoleHierarchyManager.ALL_PERMISSIONS, RoleHierarchyManager.ADMIN_COLOR),
                roleManager.getOrCreateRole(guild, "Head DJ", RoleHierarchyManager.HEAD_DJ_PERMISSIONS, RoleHierarchyManager.HEAD_DJ_COLOR),
                roleManager.getOrCreateRole(guild, "Event Staff", RoleHierarchyManager.EVENT_STAFF_PERMISSIONS, RoleHierarchyManager.EVENT_STAFF_COLOR)
        ));

        System.out.println("Roles setup completed for guild: " + guild.getName());
    }
}
