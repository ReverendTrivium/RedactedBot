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
 * Command that risks losing money for a greater potential reward.
 *
 * @author Derrick Eberlein
 */
public class CrimeCommand extends Command {

    /**
     * Constructor for the CrimeCommand.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public CrimeCommand(Redacted bot) {
        super(bot);
        this.name = "crime";
        this.description = "Commit a crime for a chance at some extra money.";
        this.category = Category.ECONOMY;
    }

    /**
     * Executes the crime command.
     * Checks if the user is on timeout, and if not, attempts to commit a crime.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    public void execute(SlashCommandInteractionEvent event) {
        long user = event.getUser().getIdLong();
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        Long timeout = economyHandler.getTimeout(user, EconomyHandler.TIMEOUT_TYPE.CRIME);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
        if (timeout != null && System.currentTimeMillis() < timeout) {
            // On timeout
            String timestamp = economyHandler.formatTimeout(timeout);
            embed.setDescription(":stopwatch: You can next commit a crime " + timestamp + ".");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            // Commit crime
            EconomyReply reply = economyHandler.crime(user);
            int color = reply.isSuccess() ? EmbedColor.SUCCESS.color : EmbedColor.ERROR.color;
            embed.setDescription(reply.getResponse());
            embed.setColor(color);
            embed.setFooter("Reply #" + reply.getId());
            event.replyEmbeds(embed.build()).queue();
        }
    }
}
