package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.MessageSchedulerListener;

/**
 * Command to schedule a message to be sent in a specified channel at a specified time.
 * This command allows users with the appropriate permissions to set up a message that will be sent
 * automatically at a later time, enhancing the utility of the bot for reminders or scheduled announcements.
 *
 * @author Derrick Eberlein
 */
public class MessageSchedulerCommand extends Command {

    /**
     * Constructor for the MessageSchedulerCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public MessageSchedulerCommand(Redacted bot) {
        super(bot);
        this.name = "schedule";
        this.description = "Schedule a message to be sent in a specified channel at a specified time.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    /**
     * Executes the MessageSchedulerCommand.
     * This method handles the interaction when the command is invoked.
     * It initializes the MessageSchedulerListener to start listening for user input.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Pass the event to the MessageSchedulerListener
        MessageSchedulerListener listener = new MessageSchedulerListener(bot, bot.database);
        event.reply("Let's get started with scheduling your message!").queue();
        listener.startListening(event.getChannel().asTextChannel(), event.getUser());
    }
}
