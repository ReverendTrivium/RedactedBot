package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.listeners.MessageSchedulerListener;

/**
 * ShardReadyListener Class
 * This listener is triggered when all shards of the bot are ready.
 * It loads and reschedules messages for each guild the bot is part of.
 *
 * @author Derrick Eberlein
 */
public class ShardReadyListener extends ListenerAdapter {
    private final Redacted bot;
    private final MessageSchedulerListener schedulerListener;

    /**
     * Constructs a ShardReadyListener with the provided Redacted bot instance and MessageSchedulerListener.
     *
     * @param bot the Redacted bot instance
     * @param schedulerListener the MessageSchedulerListener instance for message scheduling
     */
    public ShardReadyListener(Redacted bot, MessageSchedulerListener schedulerListener) {
        this.bot = bot;
        this.schedulerListener = schedulerListener;
    }

    /**
     * Called when all shards are ready.
     * This method iterates over each guild the bot is part of and loads/reschedules messages.
     *
     * @param event the ReadyEvent containing information about the bot's readiness
     */
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
