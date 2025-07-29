package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

/**
 * Command to get the current state of the NSFW clean feature.
 * This command allows server administrators to check whether the automatic cleaning of NSFW channels is enabled or disabled.
 * Usage: /get-nsfw-clean-toggle
 *
 * @author Derrick Eberlein
 */
public class GetNSFWCleanToggleCommand extends Command {

    /**
     * Constructs a new GetNSFWCleanToggleCommand instance.
     * Initializes the command with its name, description, permission requirements, and category.
     *
     * @param bot The Redacted bot instance to register this command with.
     */
    public GetNSFWCleanToggleCommand(Redacted bot) {
        super(bot);
        this.name = "get-nsfw-clean-toggle";
        this.description = "Get the current state of the NSFW clean feature.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
    }

    /**
     * Executes the GetNSFWCleanToggleCommand.
     * This method retrieves the current state of the NSFW clean feature for the server.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData guildData = GuildData.get(event.getGuild(), bot);
        boolean isEnabled = guildData.isNSFWCleanEnabled();

        event.reply("NSFW Clean feature is currently " + (isEnabled ? "enabled" : "disabled") + ".")
                .setEphemeral(true)
                .queue();
    }
}
