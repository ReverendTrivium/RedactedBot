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
import org.redacted.listeners.Ticket.TicketAddUserHandler;
import org.redacted.listeners.Ticket.TicketCloseHandler;
import org.redacted.listeners.Ticket.TicketRemoveUserHandler;

import java.util.Arrays;

/**
 * BotInitializer Class
 * Initializes the Discord bot with the provided token and registers event listeners.
 * This class is responsible for setting up the bot's status, activity, and caching policies.
 *
 * @author Derrick Eberlein
 */
public class BotInitializer {

    /**
     * Initializes the bot with the provided token and sets up the shard manager.
     *
     * @param token The bot token to authenticate with Discord.
     * @return A ShardManager instance for managing shards of the bot.
     */
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

    /**
     * Registers all event listeners with the ShardManager.
     *
     * @param shardManager The ShardManager instance to register listeners with.
     * @param bot The Redacted bot instance to pass to the listeners.
     */
    public static void registerListeners(ShardManager shardManager, Redacted bot) {
        BotCommands botCommands = new BotCommands(bot); // Create a single instance of BotCommands

        shardManager.addEventListener(
                new EventListener(), // Register EventListener
                new GalleryReactionListener(bot), // Register GalleryReactionListener
                new ButtonListener(bot), // Register ButtonListener
                new NSFWCleanListener(), // Register NSFWCleanListener
                new MessageListener(bot), // Register MessageListener
                new ReactionRoleListener(), // Register ReactionRoleListener
                new TicketListener(bot), // Register TicketListener
                new TicketCloseHandler(bot), // Register TicketCloseHandler
                new TicketAddUserHandler(), // Register TicketAddUserHandler
                new TicketRemoveUserHandler(), // Register TicketRemoveUserHandler
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

    /**
     * Waits for the GuildData and its configuration to be initialized.
     * This method will retry until the GuildData is available or a timeout occurs.
     *
     * @param guild The guild to check for initialization.
     * @param bot The Redacted bot instance.
     * @return The initialized GuildData, or null if it fails to initialize within the timeout.
     */
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
