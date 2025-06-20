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
 * Command that configures auto join DMs.
 *
 * @author Derrick Eberlein
 */
public class JoinDMCommand extends Command {

    /**
     * Constructor for the JoinDMCommand.
     * Initializes the command with its name, description, category, options, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public JoinDMCommand(Redacted bot) {
        super(bot);
        this.name = "join-dm";
        this.description = "Set a private message to be sent when a member joins.";
        this.category = Category.GREETINGS;
        this.args.add(new OptionData(OptionType.STRING, "message", "The message to send as a DM"));
        this.permission = Permission.MANAGE_SERVER;
    }

    /**
     * Executes the join DM command.
     * This method handles the interaction when the command is invoked.
     * It either sets or removes a join DM message based on the provided options.
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
            greetingHandler.removeJoinDM();
            String text = EmbedUtils.BLUE_X + " Join DM message successfully removed!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }

        // Set greeting message
        greetingHandler.setJoinDM(farewellOption.getAsString());
        String text = EmbedUtils.BLUE_TICK + " Join DM message successfully updated!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
