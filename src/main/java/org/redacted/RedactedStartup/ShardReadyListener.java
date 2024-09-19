package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.listeners.MessageSchedulerListener;

public class ShardReadyListener extends ListenerAdapter {
    private final Redacted bot;
    private final MessageSchedulerListener schedulerListener;

    public ShardReadyListener(Redacted bot, MessageSchedulerListener schedulerListener) {
        this.bot = bot;
        this.schedulerListener = schedulerListener;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("All shards are ready. Loading and rescheduling messages for each guild...");

        // Iterate over each guild the bot is part of
        for (Guild guild : event.getJDA().getGuilds()) {
            // Retrieve the GuildData instance for this guild
            GuildData guildData = GuildData.get(guild, bot);

            // Load and reschedule messages for this guild
            schedulerListener.loadAndRescheduleMessages(guildData);
        }

        // Optionally, remove this listener after it's done its job
        bot.getShardManager().removeEventListener(this);
    }
}
