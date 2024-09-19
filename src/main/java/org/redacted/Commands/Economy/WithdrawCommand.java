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
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;

/**
 * Command that withdraws cash from the user's bank.
 *
 * @author Derrick Eberlein
 */
public class WithdrawCommand extends Command {

    public WithdrawCommand(Redacted bot) {
        super(bot);
        this.name = "withdraw";
        this.description = "Withdraw your money from the bank.";
        this.category = Category.ECONOMY;
        this.args.add(new OptionData(OptionType.INTEGER, "amount", "The amount of money you want to deposit.").setMinValue(1));
    }

    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        String currency = economyHandler.getCurrency();
        long bank = economyHandler.getBank(user.getIdLong());

        EmbedBuilder embed = new EmbedBuilder().setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
        if (bank <= 0) {
            // Bank is at 0
            embed.setDescription(EmbedUtils.RED_X + " You don't have any money in your bank to withdraw!");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        OptionMapping amountOption = event.getOption("amount");
        long amount;
        if (amountOption != null) {
            amount = amountOption.getAsLong();
            if (amount > bank) {
                // Amount is higher than balance
                String value = currency + " " + EconomyHandler.FORMATTER.format(bank);
                embed.setDescription(EmbedUtils.RED_X + " You cannot withdraw more than " + value + "!");
                embed.setColor(EmbedColor.ERROR.color);
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                return;
            }
        } else {
            amount = bank;
        }
        economyHandler.withdraw(user.getIdLong(), amount);

        // Send embed message
        String value = currency + " " + EconomyHandler.FORMATTER.format(amount);
        embed.setDescription(EmbedUtils.GREEN_TICK + " Withdrew " + value + " from your bank!");
        embed.setColor(EmbedColor.SUCCESS.color);
        event.replyEmbeds(embed.build()).queue();
    }
}
