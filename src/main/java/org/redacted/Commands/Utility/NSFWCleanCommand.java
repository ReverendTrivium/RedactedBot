package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.NSFWCleanListener;

/**
 * Command to clean the NSFW channel.
 * This command allows users with the appropriate permissions to remove all messages from an NSFW channel,
 * effectively cleaning it up for future use.
 *
 * @author Derrick Eberlein
 */
public class NSFWCleanCommand extends Command {

    /**
     * Constructor for the NSFWCleanCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public NSFWCleanCommand(Redacted bot) {
        super(bot);
        this.name = "clean";
        this.description = "Clean the NSFW channel";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    /**
     * Executes the NSFWCleanCommand.
     * This method handles the interaction when the command is invoked.
     * It checks if the channel is NSFW and then cleans it using the NSFWCleanListener.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getChannel().asTextChannel().isNSFW()) {
            event.deferReply().setEphemeral(true).queue();
            NSFWCleanListener.cleanChannel(event.getChannel().asTextChannel(), bot);
            event.getHook().sendMessage("The NSFW channel has been cleaned.").queue();
        } else {
            event.reply("This command can only be used in NSFW channels.").setEphemeral(true).queue();
        }
    }
}

