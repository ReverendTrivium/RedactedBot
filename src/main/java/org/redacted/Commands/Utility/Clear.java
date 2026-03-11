package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.IntroductionListener;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command that clears a set amount or all the messages in a channel or
 * a specific amount or all the messages from a specific user in the channel.
 * This command is typically used by staff members to manage message clutter.
 *
 * @author Derrick Eberlein
 */
public class Clear extends Command {

    // Discord bulk delete constraints
    private static final int MAX_BULK_DELETE = 100;
    private static final int MIN_AMOUNT = 1;

    /**
     * Constructor for the Clear command.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public Clear(Redacted bot) {
        super(bot);
        this.name = "clear";
        this.description = "Clears messages in the channel (optionally only from a specific user, optionally limited by amount).";
        this.category = Category.STAFF;
        this.permission = Permission.KICK_MEMBERS;

        this.args.add(new OptionData(OptionType.USER, "user", "The user whose messages you want to delete"));
        this.args.add(new OptionData(OptionType.INTEGER, "amount", "How many recent messages to delete (1-100)")
                .setRequired(false)
                .setMinValue(MIN_AMOUNT)
                .setMaxValue(MAX_BULK_DELETE));
    }

    /**
     * Executes the Clear command.
     * This method handles the interaction when the command is invoked.
     * It clears messages in the channel or messages from a specific user in the channel,
     * optionally limited by the specified amount.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();

        Member member = event.getOption("user") != null
                ? Objects.requireNonNull(event.getOption("user")).getAsMember()
                : null;

        Integer amount = event.getOption("amount") != null
                ? (int) Objects.requireNonNull(event.getOption("amount")).getAsLong()
                : null;

        if (amount != null) {
            // Clamp just in case
            amount = Math.max(MIN_AMOUNT, Math.min(MAX_BULK_DELETE, amount));
        }

        if (member == null && amount == null) {
            event.reply("Clearing all messages in this channel...").setEphemeral(true).queue();
            clearAllMessagesInChannel(channel);
            return;
        }

        if (member == null) {
            event.reply("Clearing the last " + amount + " messages in this channel...").setEphemeral(true).queue();
            clearLastNMessagesInChannel(channel, amount);
            return;
        }

        if (amount == null) {
            event.reply("Clearing all messages from " + member.getEffectiveName() + " in this channel...")
                    .setEphemeral(true).queue();
            clearMessagesFromUserInChannel(channel, member);
            return;
        }

        event.reply("Clearing the last " + amount + " messages from " + member.getEffectiveName() + " in this channel...")
                .setEphemeral(true).queue();
        clearLastNMessagesFromUserInChannel(channel, member, amount);
    }

    /**
     * Marks a message as deleted by staff.
     * This is used to keep track of messages that have been cleared by staff members.
     *
     * @param messageId The ID of the message to mark as deleted.
     */
    private void markMessageAsStaffDeleted(String messageId) {
        IntroductionListener.markAsStaffDeleted(messageId);
    }

    /**
     * Clears ALL messages in the channel (iterates entire history).
     * Warning: This can take a long time on big channels and will rate-limit heavily.
     *
     * @param channel The TextChannel from which to clear messages.
     */
    private void clearAllMessagesInChannel(TextChannel channel) {
        channel.getIterableHistory().queue(messages -> {
            for (Message message : messages) {
                markMessageAsStaffDeleted(message.getId());
                message.delete().queue();
            }
        });
    }

    /**
     * Clears ALL messages from a user in the channel (iterates entire history).
     *
     * @param channel The TextChannel from which to clear messages.
     * @param member  The Member whose messages should be deleted.
     */
    private void clearMessagesFromUserInChannel(TextChannel channel, Member member) {
        channel.getIterableHistory().queue(messages -> {
            for (Message message : messages) {
                if (message.getAuthor().equals(member.getUser())) {
                    markMessageAsStaffDeleted(message.getId());
                    message.delete().queue();
                }
            }
        });
    }

    /**
     * Clears the last N messages in the channel.
     * Uses bulk delete when possible; falls back to individual deletes for older messages.
     *
     * @param channel The TextChannel from which to clear messages.
     * @param amount  The number of recent messages to delete.
     */
    private void clearLastNMessagesInChannel(TextChannel channel, int amount) {
        channel.getHistory().retrievePast(amount).queue(messages -> deleteMessagesSmart(channel, messages));
    }

    /**
     * Clears the last N messages from a user in the channel.
     * Scans recent history in chunks until it collects N matching messages or history runs out.
     *
     * @param channel The TextChannel from which to clear messages.
     * @param member  The Member whose messages should be deleted.
     * @param amount  The number of recent messages from the user to delete.
     */
    private void clearLastNMessagesFromUserInChannel(TextChannel channel, Member member, int amount) {
        List<Message> toDelete = new ArrayList<>(amount);
        collectRecentUserMessages(channel, member, amount, null, toDelete);
    }

    /**
     * Recursively collects messages from history in chunks (up to 100 each call) until we have enough.
     *
     * @param channel   The TextChannel to scan.
     * @param member    The Member whose messages we are looking for.
     * @param target    The total number of messages to collect.
     * @param before    The message before which to continue scanning (null to start from latest).
     * @param collected The list to accumulate collected messages into.
     */
    private void collectRecentUserMessages(TextChannel channel,
                                           Member member,
                                           int target,
                                           Message before,
                                           List<Message> collected) {

        int chunkSize = Math.min(MAX_BULK_DELETE, Math.max(10, target - collected.size()));
        var action = (before == null)
                ? channel.getHistory().retrievePast(chunkSize)
                : channel.getHistoryBefore(before, 1).flatMap(h -> h.retrievePast(chunkSize));

        action.queue(messages -> {
            if (messages.isEmpty()) {
                // Nothing left to scan
                if (!collected.isEmpty()) deleteMessagesSmart(channel, collected);
                return;
            }

            for (Message m : messages) {
                if (m.getAuthor().equals(member.getUser())) {
                    collected.add(m);
                    if (collected.size() >= target) break;
                }
            }

            if (collected.size() >= target) {
                deleteMessagesSmart(channel, collected);
                return;
            }

            // Continue scanning older messages
            Message oldestInThisBatch = messages.get(messages.size() - 1);
            collectRecentUserMessages(channel, member, target, oldestInThisBatch, collected);
        });
    }

    /**
     * Bulk deletes messages newer than 14 days; individually deletes older ones.
     *
     * @param channel  The TextChannel from which to delete messages.
     * @param messages The list of messages to delete.
     */
    private void deleteMessagesSmart(TextChannel channel, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(14);

        List<Message> recent = new ArrayList<>();
        List<Message> old = new ArrayList<>();

        for (Message m : messages) {
            // Mark first (your custom tracking)
            markMessageAsStaffDeleted(m.getId());

            if (m.getTimeCreated().isAfter(cutoff)) recent.add(m);
            else old.add(m);
        }

        // Bulk delete recent ones (2+ messages required by Discord for bulk delete)
        if (recent.size() >= 2) {
            channel.deleteMessages(recent).queue(
                    success -> {},
                    err -> {
                        // If bulk delete fails for any reason, fall back to single deletes
                        for (Message m : recent) {
                            m.delete().queue(null, e -> {});
                        }
                    }
            );
        } else if (recent.size() == 1) {
            recent.get(0).delete().queue(null, e -> {});
        }

        // Old messages must be deleted one-by-one
        for (Message m : old) {
            m.delete().queue(null, e -> {});
        }
    }
}
