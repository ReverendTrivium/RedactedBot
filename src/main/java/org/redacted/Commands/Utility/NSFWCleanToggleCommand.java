package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

/**
 * Command to toggle the NSFW clean feature on or off.
 * This command allows server administrators to enable or disable the automatic cleaning of NSFW channels.
 * Usage: /nsfw-clean-toggle
 *
 * @author Derrick Eberlein
 */
public class NSFWCleanToggleCommand extends Command {

    /**
     * Constructs a new NSFWCleanToggleCommand instance.
     * Initializes the command with its name, description, permission requirements, and category.
     *
     * @param bot The Redacted bot instance to register this command with.
     */
    public NSFWCleanToggleCommand(Redacted bot) {
        super(bot);
        this.name = "nsfw-clean-toggle";
        this.description = "Toggle the NSFW clean feature on or off.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    /**
     * Executes the NSFW clean toggle command.
     * This method toggles the state of the NSFW clean feature for the server.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData guildData = GuildData.get(event.getGuild(), bot);

        boolean current = guildData.isNSFWCleanEnabled();
        boolean newState = !current;
        guildData.setNSFWCleanToggle(newState);

        event.reply("NSFW Clean feature has been " + (newState ? "enabled" : "disabled") + ".")
                .setEphemeral(true)
                .queue();
    }
}
