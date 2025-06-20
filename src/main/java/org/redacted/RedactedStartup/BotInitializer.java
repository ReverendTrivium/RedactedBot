package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.redacted.Commands.BotCommands;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.Roles.RoleHierarchyManager;
import org.redacted.listeners.*;

import java.util.Arrays;

public class BotInitializer {

    public static ShardManager initializeBot(String token) {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);

        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("Over All"));
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT);
        builder.enableCache(CacheFlag.ACTIVITY, CacheFlag.ONLINE_STATUS);

        return builder.build();
    }

    public static void registerListeners(ShardManager shardManager, Redacted bot) {
        BotCommands botCommands = new BotCommands(bot); // Create a single instance of BotCommands

        shardManager.addEventListener(
                new EventListener(),
                new GalleryReactionListener(bot),
                new ButtonListener(bot),
                new NSFWCleanListener(),
                new MessageListener(bot),
                new ReactionRoleListener(),
                botCommands,  // Register BotCommands as an event listener
                new BotEventListener(bot, botCommands) // Pass BotCommands to BotEventListener
        );

        shardManager.addEventListener(new ListenerAdapter() {
            @Override
            public void onGuildReady(@NotNull GuildReadyEvent event) {
                Guild guild = event.getGuild();
                if (guild == null) {
                    System.err.println("Guild is null in onGuildReady event!");
                    return; // Early exit to avoid NullPointerException
                }

                // Retry until GuildData/Config is initialized, without a hard retry limit
                GuildData guildData = waitForGuildDataInitialization(guild, bot);

                if (guildData == null || guildData.getConfig() == null) {
                    System.err.println("Config for guild " + guild.getIdLong() + " is still not initialized.");
                    return;
                }

                ChannelManager channelManager = new ChannelManager();
                RoleManager roleManager = new RoleManager();

                TextChannel introductionChannel = channelManager.getOrCreateTextChannel(guild, "introductions", "Information");
                TextChannel moderationChannel = channelManager.getOrCreateTextChannel(guild, "mod-log", "Moderation");
                TextChannel botCommands = channelManager.getOrCreateTextChannel(guild, "bot-commands", "Moderation");
                TextChannel botNotifications = channelManager.getOrCreateTextChannel(guild, "bot-notifications", "bot-fun");

                // Create Member and Flagged Roles
                Role memberRole = roleManager.getOrCreateRole(guild, "Member", RoleHierarchyManager.MEMBER_PERMISSIONS, RoleHierarchyManager.MEMBER_COLOR);
                Role flaggedRole = roleManager.getOrCreateRole(guild, "Flagged", RoleHierarchyManager.NO_PERMISSIONS, RoleHierarchyManager.FLAGGED_COLOR);

                // Adjust role positions
                roleManager.adjustRoleHierarchy(guild, Arrays.asList(
                        roleManager.getOrCreateRole(guild, "Admin", RoleHierarchyManager.ALL_PERMISSIONS, RoleHierarchyManager.ADMIN_COLOR),
                        roleManager.getOrCreateRole(guild, "Head DJ", RoleHierarchyManager.HEAD_DJ_PERMISSIONS, RoleHierarchyManager.HEAD_DJ_COLOR),
                        roleManager.getOrCreateRole(guild, "Event Staff", RoleHierarchyManager.EVENT_STAFF_PERMISSIONS, RoleHierarchyManager.EVENT_STAFF_COLOR)
                ));

                // Register the IntroductionListener with the retrieved channel IDs and role IDs
                shardManager.addEventListener(new IntroductionListener(bot, introductionChannel.getId(), moderationChannel.getId(), memberRole.getId(), flaggedRole.getId(), guild));
                System.out.println("Listeners registered for guild: " + guild.getName());
            }
        });
    }

    private static GuildData waitForGuildDataInitialization(Guild guild, Redacted bot) {
        GuildData guildData;
        long timeout = 5000L; // Maximum wait time in milliseconds
        long startTime = System.currentTimeMillis();
        do {
            guildData = GuildData.get(guild, bot);
            if (guildData != null && guildData.getConfig() != null) {
                return guildData;
            }
            System.out.println("Waiting for GuildData/Config to initialize for guild: " + guild.getName());
            try {
                Thread.sleep(500); // Exponential backoff could be applied if needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (System.currentTimeMillis() - startTime < timeout);

        return null; // Return null if the initialization fails after the timeout
    }
}
