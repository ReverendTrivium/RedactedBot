package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.redacted.Redacted;
import org.redacted.listeners.NSFWCleanListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerManager {
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


