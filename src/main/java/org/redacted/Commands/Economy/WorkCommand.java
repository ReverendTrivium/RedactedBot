package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Handlers.economy.EconomyReply;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;

import java.util.Objects;

/**
 * Command that adds money to your balance.
 *
 * @author Derrick Eberlein
 */
public class WorkCommand extends Command {

    /**
     * Constructor for the WorkCommand.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public WorkCommand(Redacted bot) {
        super(bot);
        this.name = "work";
        this.description = "Work for some extra money.";
        this.category = Category.ECONOMY;
    }

    /**
     * Executes the work command.
     * Checks if the user is on timeout, and if not, allows them to work and receive money.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    public void execute(SlashCommandInteractionEvent event) {
        long user = event.getUser().getIdLong();
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        Long timeout = economyHandler.getTimeout(user, EconomyHandler.TIMEOUT_TYPE.WORK);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
        if (timeout != null && System.currentTimeMillis() < timeout) {
            // On timeout
            String timestamp = economyHandler.formatTimeout(timeout);
            embed.setDescription(":stopwatch: You can next work " + timestamp + ".");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            // Work
            EconomyReply reply = economyHandler.work(user);
            embed.setDescription(reply.getResponse());
            embed.setColor(EmbedColor.SUCCESS.color);
            embed.setFooter("Reply #"+reply.getId());
            event.replyEmbeds(embed.build()).queue();
        }
    }
}
