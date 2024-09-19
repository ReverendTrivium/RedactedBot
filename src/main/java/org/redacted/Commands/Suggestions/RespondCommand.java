package org.redacted.Commands.Suggestions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.SuggestionHandler;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command that responds to suggestions on the suggestion board.
 *
 * @author Derrick Eberlein
 */
public class RespondCommand extends Command {

    public RespondCommand(Redacted bot) {
        super(bot);
        this.name = "respond";
        this.description = "Respond to a suggestion on the suggestion board.";
        this.category = Category.SUGGESTIONS;
        this.permission = Permission.MANAGE_SERVER;
        this.args.add(new OptionData(OptionType.STRING, "response", "The response to the suggestion", true)
                .addChoice("Approve", "APPROVE")
                .addChoice("Consider", "CONSIDER")
                .addChoice("Deny", "DENY")
                .addChoice("Implement", "IMPLEMENT"));
        this.args.add(new OptionData(OptionType.INTEGER, "number", "The suggestion number to respond to", true)
                .setMinValue(1)
                .setMaxValue(Integer.MAX_VALUE));
        this.args.add(new OptionData(OptionType.STRING, "reason", "The reason for your response"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String responseString = Objects.requireNonNull(event.getOption("response")).getAsString();
        SuggestionHandler.SuggestionResponse response = SuggestionHandler.SuggestionResponse.valueOf(responseString);

        int id = Objects.requireNonNull(event.getOption("number")).getAsInt() - 1;
        OptionMapping reason = event.getOption("reason");

        GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getSuggestionHandler().respond(event, id, reason, response);
    }
}
