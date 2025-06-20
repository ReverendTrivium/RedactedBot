package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Economy;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;

import java.util.Objects;

/**
 * Command that shows your current cash and bank balance on the server.
 *
 * @author Derrick Eberlein
 */
public class BalanceCommand extends Command {

    /**
     * Constructor for the BalanceCommand.
     * Initializes the command with its name, description, category, and options.
     *
     * @param bot The Redacted bot instance.
     */
    public BalanceCommand(Redacted bot) {
        super(bot);
        this.name = "balance";
        this.description = "Check your current balance.";
        this.category = Category.ECONOMY;
        this.args.add(new OptionData(OptionType.USER, "user", "See another user's balance"));
    }

    /**
     * Executes the balance command.
     * Retrieves the user's balance and bank values, then sends an embed message with the information.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    public void execute(SlashCommandInteractionEvent event) {
        // Get user
        OptionMapping userOption = event.getOption("user");
        User user = (userOption != null) ? userOption.getAsUser() : event.getUser();

        // Get balance and bank values
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        Economy profile = economyHandler.getProfile(user.getIdLong());
        Long balance;
        Long bank;
        long total;
        if (profile != null) {
            balance = profile.getBalance();
            if (balance == null) balance = 0L;
            bank = profile.getBank();
            if (bank == null) bank = 0L;
        } else {
            balance = 0L;
            bank = 0L;
        }
        total = balance + bank;

        // Send embed message
        String currency = economyHandler.getCurrency();
        EmbedBuilder embed = new EmbedBuilder()
            .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
            .setDescription("Leaderboard Rank: #" + economyHandler.getRank(user.getIdLong()))
            .addField("Cash:", currency + " " + EconomyHandler.FORMATTER.format(balance), true)
            .addField("Bank:", currency + " " + EconomyHandler.FORMATTER.format(bank), true)
            .addField("Total:", currency + " " + EconomyHandler.FORMATTER.format(total), true)
            .setColor(EmbedColor.DEFAULT.color);
        event.replyEmbeds(embed.build()).queue();
    }
}
