package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.redacted.Redacted;
import org.redacted.listeners.NSFWCleanListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SchedulerManager Class
 * Manages the scheduling of tasks for the Discord bot.
 * This class is responsible for initializing a scheduled task that cleans NSFW channels at a specified interval.
 *
 * @author Derrick Eberlein
 */
public class SchedulerManager {

    /**
     * Initializes a scheduled task that cleans NSFW channels in the guilds managed by the ShardManager.
     * The task runs every 72 hours (3 days).
     *
     * @param shardManager The ShardManager instance to manage guilds.
     * @param bot The Redacted bot instance to pass to the NSFWCleanListener.
     * @return A ScheduledExecutorService instance for managing scheduled tasks.
     */
    public static ScheduledExecutorService initializeScheduler(ShardManager shardManager, Redacted bot) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (Guild guild : shardManager.getGuilds()) {
                for (TextChannel channel : guild.getTextChannels()) {
                    if (channel.isNSFW() && channel.getName().equalsIgnoreCase("nsfw-enter-at-your-risk")) {
                        NSFWCleanListener.cleanChannel(channel, bot);
                    }
                }
            }
        }, 0, 3, TimeUnit.DAYS); // Change the interval to 72 hours
        return scheduler;
    }
}


