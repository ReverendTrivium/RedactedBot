package org.redacted.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.Database;
import org.redacted.Redacted;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSchedulerListener extends ListenerAdapter {

    private final Redacted bot;
    private User user;
    private final ScheduledExecutorService scheduler;
    private String title;
    private String content;
    private TextChannel destination;
    private long repeat = -1;
    private LocalDateTime time;
    private boolean awaitingTimeInput = false;

    public MessageSchedulerListener(Redacted bot, Database database) {
        this.bot = bot;
        this.scheduler = Executors.newScheduledThreadPool(1);
        System.out.println("MessageSchedulerListener initialized");

        // Load and execute any pending messages from the database
        bot.getShardManager().getGuilds().forEach(guild -> {
            GuildData guildData = GuildData.get(guild, bot);
            loadAndRescheduleMessages(guildData);
        });
    }

    public void startListening(TextChannel channel, User user) {
        this.user = user;
        System.out.println("Start listening for user: " + user.getName() + " in channel: " + channel.getName());
        askForTitle(channel);
        bot.getShardManager().addEventListener(this);
    }

    public void loadAndRescheduleMessages(GuildData guildData) {
        System.out.println("Loading and rescheduling messages from the database for guild: " + guildData.getGuildId());

        List<Document> scheduledMessages = guildData.getScheduledMessagesCollection().find().into(new ArrayList<>());
        for (Document messageDoc : scheduledMessages) {
            String userId = messageDoc.getString("userId");
            String channelId = messageDoc.getString("channelId");
            String title = messageDoc.getString("title");
            String content = messageDoc.getString("content");
            String repeatStr = messageDoc.getString("repeat");
            LocalDateTime time = LocalDateTime.parse(messageDoc.getString("time"));

            long repeat;
            try {
                repeat = (repeatStr == null) ? 0 : Long.parseLong(repeatStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid repeat interval found in database for message with ID: " + messageDoc.getObjectId("_id").toHexString());
                System.err.println("Setting repeat interval to 0 to prevent repetition.");
                repeat = 0;
            }

            TextChannel destinationChannel = bot.getShardManager().getTextChannelById(channelId);
            System.out.println("Destination Channel is: " + destinationChannel + " or " + channelId);

            if (destinationChannel != null) {
                long delay = Duration.between(LocalDateTime.now(), time).getSeconds();

                if (delay <= 0) {
                    System.out.println("Scheduled time for message with ID " + messageDoc.getObjectId("_id").toHexString() + " has passed. Running the message immediately.");
                    // Run the message immediately
                    destinationChannel.sendMessage(formatMessage(title, content)).queue();

                    // If the message does not repeat, remove it from the database
                    if (repeat == 0) {
                        guildData.getScheduledMessagesCollection().deleteOne(new Document("_id", messageDoc.getObjectId("_id")));
                    } else {
                        // Schedule the next occurrence based on the repeat interval
                        rescheduleMessage(destinationChannel, title, content, repeat, time.plusSeconds(repeat), messageDoc.getObjectId("_id").toHexString(), guildData);
                    }
                } else {
                    // Schedule the message to be sent in the future
                    rescheduleMessage(destinationChannel, title, content, repeat, time, messageDoc.getObjectId("_id").toHexString(), guildData);
                }
            } else {
                System.out.println("Destination channel not found for message with ID: " + messageDoc.getObjectId("_id").toHexString());
            }
        }
    }

    private String formatMessage(String title, String content) {
        StringBuilder formattedMessage = new StringBuilder();
        if (title != null) {
            formattedMessage.append("**").append(title).append("**\n\n");
        }
        formattedMessage.append(content);
        return formattedMessage.toString();
    }

    private void rescheduleMessage(TextChannel destination, String title, String content, long repeat, LocalDateTime time, String messageId, GuildData guildData) {
        StringBuilder formattedMessage = new StringBuilder();
        if (title != null) {
            formattedMessage.append("**").append(title).append("**\n\n");
        }
        formattedMessage.append(content);

        long delay = Duration.between(LocalDateTime.now(), time).getSeconds();
        if (delay <= 0) {
            delay = 1;
        }

        scheduler.schedule(() -> {
            destination.sendMessage(formattedMessage.toString()).queue();
            if (repeat == 0) {
                guildData.getScheduledMessagesCollection().deleteOne(new Document("_id", new org.bson.types.ObjectId(messageId)));
            }
        }, delay, TimeUnit.SECONDS);

        if (repeat > 0) {
            scheduler.scheduleAtFixedRate(() -> destination.sendMessage(formattedMessage.toString()).queue(), delay, repeat, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getAuthor().equals(user)) {
            System.out.println("Ignoring message from different user: " + event.getAuthor().getName());
            return;
        }

        String message = event.getMessage().getContentRaw().trim();
        TextChannel channel = event.getChannel().asTextChannel();
        System.out.println("Message received: " + message);

        if (title == null) {
            System.out.println("Awaiting title input...");
            if (message.equalsIgnoreCase("cancel")) {
                cancel(channel);
                return;
            }
            title = message.equalsIgnoreCase("none") ? null : message;
            System.out.println("Title set to: " + title);
            askForContent(channel);
        } else if (content == null) {
            System.out.println("Awaiting content input...");
            if (message.equalsIgnoreCase("cancel")) {
                cancel(channel);
                return;
            }
            content = message;
            System.out.println("Content set to: " + content);
            askForChannel(channel);
        } else if (destination == null) {
            System.out.println("Awaiting channel input...");
            if (message.equalsIgnoreCase("cancel")) {
                cancel(channel);
                return;
            }
            destination = bot.getShardManager().getTextChannelById(message.replaceAll("[^0-9]", ""));
            if (destination == null) {
                System.out.println("Invalid channel entered: " + message);
                channel.sendMessage("Invalid channel. Please try again.").queue();
                return;
            }
            System.out.println("Destination channel set to: " + destination.getName());
            askForRepeat(channel);
        } else if (repeat == -1) {
            System.out.println("Awaiting repeat interval input...");
            if (message.equalsIgnoreCase("cancel")) {
                cancel(channel);
                return;
            }
            if (message.equalsIgnoreCase("none")) {
                repeat = 0;
                System.out.println("No repeat interval selected. Repeat set to 0.");
            } else {
                repeat = parseRepeatInterval(message);
                System.out.println("Repeat interval set to: " + repeat + " seconds.");
            }
            awaitingTimeInput = true;
            askForTime(channel);
        } else if (awaitingTimeInput && time == null) {
            System.out.println("Awaiting time input...");
            if (message.equalsIgnoreCase("cancel")) {
                cancel(channel);
                return;
            }

            try {
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd hh:mma", Locale.ENGLISH);
                Date date = formatter.parse(message.toUpperCase().trim());
                LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                if (localDateTime.isBefore(LocalDateTime.now())) {
                    localDateTime = localDateTime.withYear(LocalDateTime.now().getYear());
                }

                time = localDateTime;
                awaitingTimeInput = false;
                System.out.println("Successfully parsed time: " + time);
                scheduleMessage(channel);
            } catch (ParseException e) {
                System.out.println("Failed to parse time: " + message);
                channel.sendMessage("Invalid time format. Please use MM/DD hh:mmam/pm").queue();
            }
        }
    }

    private void askForTitle(TextChannel channel) {
        System.out.println("Asking for title...");
        channel.sendMessage("\uD83D\uDCDD **What is the message's title?**\n" +
                "example: My Daily Message\n\n" +
                "Type none if you want to send your message without a separate title.\n" +
                "Type cancel to stop.").queue();
    }

    private void askForContent(TextChannel channel) {
        System.out.println("Asking for content...");
        channel.sendMessage("\uD83D\uDCDD**What is the message's content?**\n" +
                "example: This is my daily message!\n\n" +
                "Type cancel to stop.").queue();
    }

    private void askForChannel(TextChannel channel) {
        System.out.println("Asking for channel...");
        channel.sendMessage("\uD83D\uDCDD **Which channel should this message send in?**\n" +
                "example: #bot-commands\n\n" +
                "Type cancel to stop.").queue();
    }

    private void askForRepeat(TextChannel channel) {
        System.out.println("Asking for repeat interval...");
        channel.sendMessage("\uD83D\uDD01 **How often do you want this message to repeat?**\n" +
                "example: 6h 30m\n" +
                "example: 5d\n\n" +
                "Type none if you only want to send it once.\n" +
                "Type cancel to stop.").queue();
    }

    private void askForTime(TextChannel channel) {
        System.out.println("Asking for time...");
        channel.sendMessage(":alarm_clock: **When do you want to send the message?**\n" +
                "format: MM/DD hh:mmam/pm\n" +
                "example: 08/12 01:45PM\n\n" +
                "Type cancel to stop.").queue();
    }

    private void scheduleMessage(TextChannel schedulingChannel) {
        System.out.println("Scheduling message...");

        StringBuilder formattedMessage = new StringBuilder();
        if (title != null) {
            formattedMessage.append("**").append(title).append("**\n\n");
        }
        formattedMessage.append(content);

        long delay = Duration.between(LocalDateTime.now(), time).getSeconds();

        if (delay <= 0) {
            System.out.println("The scheduled time is in the past or right now. Setting delay to 1 second.");
            delay = 1;
        }

        System.out.println("Message scheduled to send in: " + delay + " seconds");

        GuildData guildData = GuildData.get(schedulingChannel.getGuild(), bot);
        guildData.getScheduledMessagesCollection().insertOne(new Document("userId", user.getId())
                .append("channelId", destination.getId())
                .append("title", title)
                .append("content", content)
                .append("repeat", repeat == 0 ? null : String.valueOf(repeat))
                .append("time", time.toString()));

        System.out.println("Scheduled message saved to the database.");

        String confirmationMessage = "**Message Scheduled**\n" +
                "**Title:** " + (title != null ? title : "None") + "\n" +
                "**Content:** " + content + "\n" +
                "**Destination Channel:** " + destination.getAsMention() + "\n" +
                "**First Message Time:** " + time.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mma")) + "\n" +
                "**Message Interval:** " + (repeat == 0 ? "not repeating" : "every " + repeat + " seconds") + "\n";

        schedulingChannel.sendMessage(confirmationMessage).queue();

        scheduler.schedule(() -> {
            System.out.println("Sending scheduled message to channel: " + destination.getName());
            destination.sendMessage(formattedMessage.toString()).queue();
        }, delay, TimeUnit.SECONDS);

        if (repeat > 0) {
            System.out.println("Message will repeat every: " + repeat + " seconds");
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("Sending repeated message to channel: " + destination.getName());
                destination.sendMessage(formattedMessage.toString()).queue();
            }, delay, repeat, TimeUnit.SECONDS);
        }
    }

    private long parseRepeatInterval(String repeat) {
        System.out.println("Parsing repeat interval: " + repeat);
        long seconds = 0;
        if (repeat.endsWith("d")) {
            seconds = Long.parseLong(repeat.replace("d", "")) * 86400;
        } else if (repeat.endsWith("h")) {
            seconds = Long.parseLong(repeat.replace("h", "")) * 3600;
        } else if (repeat.contains("h") && repeat.contains("m")) {
            String[] parts = repeat.split("h");
            seconds = Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1].replace("m", "")) * 60;
        }
        System.out.println("Repeat interval parsed as: " + seconds + " seconds");
        return seconds;
    }

    private void cancel(TextChannel channel) {
        System.out.println("Cancelling message scheduling...");
        channel.sendMessage("Message scheduling has been canceled.").queue();
        bot.getShardManager().removeEventListener(this);
    }
}
