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

import java.util.Objects;

/**
 * Command that clears all messages in a channel or all messages from a specific user in the channel.
 * This command is typically used by staff members to manage message clutter.
 *
 * @author Derrick Eberlein
 */
public class Clear extends Command {

    /**
     * Constructor for the Clear command.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public Clear(Redacted bot) {
        super(bot);
        this.name = "clear";
        this.description = "Clears all messages in the channel or all messages from a specific user in the channel.";
        this.category = Category.STAFF;
        this.permission = Permission.KICK_MEMBERS;
        this.args.add(new OptionData(OptionType.USER, "user", "The user whose messages you want to delete"));
    }

    /**
     * Executes the Clear command.
     * This method handles the interaction when the command is invoked.
     * It clears all messages in the channel or all messages from a specific user in the channel.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Member member = event.getOption("user") != null ? Objects.requireNonNull(event.getOption("user")).getAsMember() : null;

        if (member == null) {
            // Clear all messages in the channel
            event.reply("Clearing all messages in this channel...").setEphemeral(true).queue();
            clearAllMessagesInChannel(channel);
        } else {
            // Clear all messages from the specified user in the channel
            event.reply("Clearing all messages from " + member.getEffectiveName() + " in this channel...").setEphemeral(true).queue();
            clearMessagesFromUserInChannel(channel, member);
        }
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
     * Clears all messages in the specified channel.
     * This method iterates through the message history and deletes each message.
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
     * Clears all messages from a specific user in the specified channel.
     * This method iterates through the message history and deletes messages authored by the specified user.
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
}
