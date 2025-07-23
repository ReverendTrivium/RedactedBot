package org.redacted.RedactedStartup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.listeners.MessageSchedulerListener;
import org.redacted.listeners.MusicListener;
import org.redacted.util.googleCalendar.CalendarSyncUtil;

/**
 * ShardReadyListener is responsible for handling the event when all shards of the bot are ready.
 * It loads and reschedules messages for each guild the bot is part of.
 *
 * @author Derrick Eberlein
 */
public class ShardReadyListener extends ListenerAdapter {
    private final Redacted bot;
    private final MessageSchedulerListener schedulerListener;
    private final MusicListener musicListener;

    /**
     * Constructor for ShardReadyListener.
     *
     * @param bot The Redacted bot instance.
     * @param schedulerListener The MessageSchedulerListener instance to handle message scheduling.
     * @param musicListener The MusicListener instance to handle music-related events.
     */
    public ShardReadyListener(Redacted bot, MessageSchedulerListener schedulerListener, MusicListener musicListener) {
        this.bot = bot;
        this.schedulerListener = schedulerListener;
        this.musicListener = musicListener;
    }

    /**
     * This method is called when all shards are ready.
     * It loads and reschedules messages for each guild the bot is part of.
     *
     * @param event The ReadyEvent containing information about the bot's readiness.
     */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("All shards are ready. Loading and rescheduling messages for each guild...");

        // Sync existing Discord events to Google Calendar
        CalendarSyncUtil.syncExistingDiscordEvents(bot);

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
