package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.MessageSchedulerListener;

public class MessageSchedulerCommand extends Command {

    public MessageSchedulerCommand(Redacted bot) {
        super(bot);
        this.name = "schedule";
        this.description = "Schedule a message to be sent in a specified channel at a specified time.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Pass the event to the MessageSchedulerListener
        MessageSchedulerListener listener = new MessageSchedulerListener(bot, bot.database);
        event.reply("Let's get started with scheduling your message!").queue();
        listener.startListening(event.getChannel().asTextChannel(), event.getUser());
    }
}
