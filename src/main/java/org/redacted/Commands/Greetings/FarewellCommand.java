package org.redacted.Commands.Greetings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.GreetingHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;

/**
 * Command that configures auto farewells.
 *
 * @author Derrick Eberlein
 */
public class FarewellCommand extends Command {

    /**
     * Constructor for the FarewellCommand.
     * Initializes the command with its name, description, category, options, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public FarewellCommand(Redacted bot) {
        super(bot);
        this.name = "farewell";
        this.description = "Set a farewell to be sent to the welcome channel when a member leaves.";
        this.category = Category.GREETINGS;
        this.args.add(new OptionData(OptionType.STRING, "message", "The message to send as a farewell"));
        this.permission = Permission.MANAGE_SERVER;
    }

    /**
     * Executes the farewell command.
     * This method handles the interaction when the command is invoked.
     * It either sets or removes a farewell message based on the provided options.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        GreetingHandler greetingHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getGreetingHandler();
        OptionMapping farewellOption = event.getOption("message");

        // Remove farewell message
        if (farewellOption == null) {
            greetingHandler.removeFarewell();
            String text = EmbedUtils.BLUE_X + " Farewell message successfully removed!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }

        // Set greeting message
        greetingHandler.setFarewell(farewellOption.getAsString());
        String text = EmbedUtils.BLUE_TICK + " Farewell message successfully updated!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
