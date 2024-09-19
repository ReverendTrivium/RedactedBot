package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.NSFWCleanListener;

public class NSFWCleanCommand extends Command {

    public NSFWCleanCommand(Redacted bot) {
        super(bot);
        this.name = "clean";
        this.description = "Clean the NSFW channel";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

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

