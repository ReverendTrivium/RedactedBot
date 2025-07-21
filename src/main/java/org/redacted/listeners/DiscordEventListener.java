package org.redacted.listeners;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventCreateEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventDeleteEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.redacted.Redacted;

import java.io.IOException;
import java.time.ZoneId;

/**
 * DiscordEventListener Class
 * This listener handles scheduled events in Discord and syncs them with Google Calendar.
 * It listens for event creation, updates, and deletions, and manages the corresponding
 * Google Calendar events.
 *
 * @author Derrick Eberlein
 */
public class DiscordEventListener extends ListenerAdapter {

    private final Redacted bot;
    private final String calendarId;

    /**
     * Constructs a DiscordEventListener with the provided Redacted bot instance.
     *
     * @param bot the Redacted bot instance
     */
    public DiscordEventListener(Redacted bot) {
        this.bot = bot;
        calendarId = bot.getConfig().get("GOOGLE_CALENDAR_ID");
    }

    /**
     * Updates the Google Calendar event corresponding to the given scheduled event.
     * This method is called when a scheduled event is updated in Discord.
     *
     * @param scheduledEvent the scheduled event that was updated
     */
    private void updateGoogleCalendarEvent(ScheduledEvent scheduledEvent) {
        try {
            System.out.println("New scheduled event update received: " + scheduledEvent.getName());
            long guildId = scheduledEvent.getGuild().getIdLong();
            long discordEventId = scheduledEvent.getIdLong();

            Document doc = getEventCollection(guildId)
                    .find(Filters.eq("discordEventId", discordEventId)).first();

            if (doc == null) {
                System.out.println("No matching document found for Discord Event ID: " + discordEventId);
                return;
            }

            String calendarEventId = doc.getString("calendarEventId");
            String calendarId = bot.getConfig().get("GOOGLE_CALENDAR_ID");

            String title = scheduledEvent.getName();
            String description = scheduledEvent.getDescription() != null ? scheduledEvent.getDescription() : "No description.";
            String location = scheduledEvent.getLocation() != null ? scheduledEvent.getLocation() : "Discord";

            String timeZone = "UTC";
            var startTime = scheduledEvent.getStartTime().atZoneSameInstant(ZoneId.of(timeZone));
            var endTime = scheduledEvent.getEndTime() != null
                    ? scheduledEvent.getEndTime().atZoneSameInstant(ZoneId.of(timeZone))
                    : startTime.plusHours(1);

            Event googleEvent = new Event()
                    .setSummary(title)
                    .setDescription(description)
                    .setLocation(location)
                    .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startTime.toInstant().toEpochMilli())).setTimeZone(timeZone))
                    .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endTime.toInstant().toEpochMilli())).setTimeZone(timeZone));

            Event updatedEvent = bot.getCalendarAPI().getService().events()
                    .update(calendarId, calendarEventId, googleEvent)
                    .execute();

            System.out.println("Google Calendar event updated: " + updatedEvent.getHtmlLink());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the creation of a new scheduled event in Discord.
     * This method creates a corresponding Google Calendar event and saves the mapping in MongoDB.
     *
     * @param event the ScheduledEventCreateEvent containing the new scheduled event
     */
    @Override
    public void onScheduledEventCreate(ScheduledEventCreateEvent event) {
        var scheduledEvent = event.getScheduledEvent();
        System.out.println("Scheduled event created: " + scheduledEvent.getName());

        try {
            long guildId = scheduledEvent.getGuild().getIdLong();
            long discordEventId = scheduledEvent.getIdLong();

            String title = scheduledEvent.getName();
            String description = scheduledEvent.getDescription() != null ? scheduledEvent.getDescription() : "No description.";
            String location = scheduledEvent.getLocation() != null ? scheduledEvent.getLocation() : "Discord";
            String timeZone = "UTC";

            var startTime = scheduledEvent.getStartTime().atZoneSameInstant(ZoneId.of(timeZone));
            var endTime = scheduledEvent.getEndTime() != null
                    ? scheduledEvent.getEndTime().atZoneSameInstant(ZoneId.of(timeZone))
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

            // Use your actual calendar ID here
            Event created = bot.getCalendarAPI().getService().events()
                    .insert(calendarId, googleEvent)
                    .execute();

            // Log the created event link
            System.out.println("Google Calendar event created: " + created.getHtmlLink());

            // Save mapping in MongoDB
            Document doc = new Document("guildId", guildId)
                    .append("discordEventId", discordEventId)
                    .append("calendarEventId", created.getId());

            getEventCollection(guildId).insertOne(doc);
            System.out.println("Created calendar event and saved mapping: " + created.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Handles updates to the name of scheduled events in Discord.
     * This method updates the corresponding Google Calendar event.
     *
     * @param event the ScheduledEventUpdateEvent containing the updated scheduled event
     */
    @Override
    public void onScheduledEventUpdateName(ScheduledEventUpdateNameEvent event) {
        System.out.println("ScheduledEventUpdateNameEvent triggered: " + event.getScheduledEvent().getName());
        updateGoogleCalendarEvent(event.getScheduledEvent());
    }

    /**
     * Handles updates to the description of scheduled events in Discord.
     * This method updates the corresponding Google Calendar event.
     *
     * @param event the ScheduledEventUpdateDescriptionEvent containing the updated scheduled event
     */
    @Override
    public void onScheduledEventUpdateDescription(ScheduledEventUpdateDescriptionEvent event) {
        System.out.println("ScheduledEventUpdateDescriptionEvent triggered: " + event.getScheduledEvent().getName());
        updateGoogleCalendarEvent(event.getScheduledEvent());
    }

    /**
     * Handles updates to the location of scheduled events in Discord.
     * This method updates the corresponding Google Calendar event.
     *
     * @param event the ScheduledEventUpdateLocationEvent containing the updated scheduled event
     */
    @Override
    public void onScheduledEventUpdateLocation(ScheduledEventUpdateLocationEvent event) {
        System.out.println("ScheduledEventUpdateLocationEvent triggered: " + event.getScheduledEvent().getName());
        updateGoogleCalendarEvent(event.getScheduledEvent());
    }

    /**
     * Handles updates to the start time of scheduled events in Discord.
     * This method updates the corresponding Google Calendar event.
     *
     * @param event the ScheduledEventUpdateStartTimeEvent containing the updated scheduled event
     */
    @Override
    public void onScheduledEventUpdateStartTime(ScheduledEventUpdateStartTimeEvent event) {
        System.out.println("ScheduledEventUpdateStartTimeEvent triggered: " + event.getScheduledEvent().getName());
        updateGoogleCalendarEvent(event.getScheduledEvent());
    }

    /**
     * Handles updates to the end time of scheduled events in Discord.
     * This method updates the corresponding Google Calendar event.
     *
     * @param event the ScheduledEventUpdateEndTimeEvent containing the updated scheduled event
     */
    @Override
    public void onScheduledEventUpdateEndTime(ScheduledEventUpdateEndTimeEvent event) {
        System.out.println("ScheduledEventUpdateEndTimeEvent triggered: " + event.getScheduledEvent().getName());
        updateGoogleCalendarEvent(event.getScheduledEvent());
    }

    /**
     * Handles the deletion of scheduled events in Discord.
     * This method deletes the corresponding Google Calendar event and removes the mapping from MongoDB.
     *
     * @param event the ScheduledEventDeleteEvent containing the deleted scheduled event
     */
    @Override
    public void onScheduledEventDelete(ScheduledEventDeleteEvent event) {
        try {
            System.out.println("ScheduledEventDeleteEvent triggered: " + event.getScheduledEvent().getName());
            var scheduledEvent = event.getScheduledEvent();
            long guildId = scheduledEvent.getGuild().getIdLong();
            long discordEventId = scheduledEvent.getIdLong();

            Document doc = getEventCollection(guildId)
                    .find(Filters.eq("discordEventId", discordEventId)).first();
            if (doc == null) return;

            String calendarEventId = doc.getString("calendarEventId");

            bot.getCalendarAPI().getService().events()
                    .delete(calendarId, calendarEventId)
                    .execute();

            getEventCollection(guildId).deleteOne(Filters.eq("discordEventId", discordEventId));

            System.out.println("Deleted Google Calendar event: " + calendarEventId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the MongoDB collection for calendar events for a specific guild.
     *
     * @param guildId the ID of the guild
     * @return the MongoCollection<Document> for calendar events
     */
    private MongoCollection<Document> getEventCollection(long guildId) {
        return bot.getDatabase().getCalendarEventCollection(guildId);
    }
}