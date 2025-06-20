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
 * Command that configures auto greetings.
 *
 * @author Derrick Eberlein
 */
public class GreetCommand extends Command {

    /**
     * Constructor for the GreetCommand.
     * Initializes the command with its name, description, category, options, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public GreetCommand(Redacted bot) {
        super(bot);
        this.name = "greet";
        this.description = "Set a greeting to be sent to the welcome channel when a member joins.";
        this.category = Category.GREETINGS;
        this.args.add(new OptionData(OptionType.STRING, "message", "The message to send as a greeting"));
        this.permission = Permission.MANAGE_SERVER;
    }

    /**
     * Executes the greet command.
     * This method handles the interaction when the command is invoked.
     * It either sets or removes a greeting message based on the provided options.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        GreetingHandler greetingHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getGreetingHandler();
        OptionMapping greetingOption = event.getOption("message");

        // Remove greeting message
        if (greetingOption == null) {
            greetingHandler.removeGreet();
            String text = EmbedUtils.BLUE_X + " Greeting message successfully removed!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }

        // Set greeting message
        greetingHandler.setGreet(greetingOption.getAsString());
        String text = EmbedUtils.BLUE_TICK + " Greeting message successfully updated!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
