package org.redacted.util.GoogleCalendar;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import org.bson.Document;
import org.redacted.Redacted;

import java.time.ZoneId;
import java.util.List;

/**
 * Utility class for syncing existing Discord events to Google Calendar.
 * This class iterates through all guilds and scheduled events, syncing them
 * to the configured Google Calendar.
 */
public class CalendarSyncUtil {

    /**
     * Syncs existing Discord scheduled events to Google Calendar.
     * This method retrieves all scheduled events from each guild and creates
     * corresponding events in Google Calendar if they do not already exist.
     *
     * @param bot The Redacted bot instance containing the necessary configurations and services.
     */
    public static void syncExistingDiscordEvents(Redacted bot) {
        System.out.println("🔄 Syncing existing Discord events to Google Calendar...");

        for (JDA shard : bot.getShardManager().getShardCache()) {
            for (Guild guild : shard.getGuilds()) {
                long guildId = guild.getIdLong();
                List<ScheduledEvent> events = guild.getScheduledEvents(); // synchronous call

                MongoCollection<Document> collection = bot.getDatabase().getCalendarEventCollection(guildId);

                for (ScheduledEvent event : events) {
                    long discordEventId = event.getIdLong();

                    // ✅ Skip if the title contains "birthday" (case-insensitive)
                    if (event.getName().toLowerCase().contains("birthday")) {
                        System.out.println("🎉 Skipping birthday event: " + event.getName());
                        continue;
                    }

                    // Skip if already in MongoDB
                    Document existing = collection.find(Filters.eq("discordEventId", discordEventId)).first();
                    if (existing != null) continue;

                    try {
                        String title = event.getName();
                        String description = event.getDescription() != null ? event.getDescription() : "No description.";
                        String location = event.getLocation() != null ? event.getLocation() : "Discord";
                        String timeZone = "UTC";

                        var startTime = event.getStartTime().atZoneSameInstant(ZoneId.of(timeZone));
                        var endTime = event.getEndTime() != null
                                ? event.getEndTime().atZoneSameInstant(ZoneId.of(timeZone))
                                : startTime.plusHours(1);

                        Event googleEvent = new Event()
                                .setSummary(title)
                                .setDescription(description)
                                .setLocation(location)
                                .setStart(new EventDateTime()
                                        .setDateTime(new com.google.api.client.util.DateTime(startTime.toInstant().toEpochMilli()))
                                        .setTimeZone(timeZone))
                                .setEnd(new EventDateTime()
                                        .setDateTime(new com.google.api.client.util.DateTime(endTime.toInstant().toEpochMilli()))
                                        .setTimeZone(timeZone));

                        // Replace with your actual calendar ID (or store per guild if configured)
                        String calendarId = bot.getConfig().get("GOOGLE_CALENDAR_ID");

                        Event created = bot.getCalendarAPI().getService().events()
                                .insert(calendarId, googleEvent)
                                .execute();

                        Document doc = new Document("guildId", guildId)
                                .append("discordEventId", discordEventId)
                                .append("calendarEventId", created.getId())
                                .append("calendarId", calendarId);

                        collection.insertOne(doc);

                        System.out.println("📅 Synced event: " + title + " (" + created.getHtmlLink() + ")");

                    } catch (Exception e) {
                        System.err.println("❌ Failed to sync event: " + event.getName());
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("✅ Sync complete.");
    }
}

