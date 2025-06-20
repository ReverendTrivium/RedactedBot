package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
 * Command that transfer cash from one user to another.
 *
 * @author Derrick Eberlein
 */
public class PayCommand extends Command {

    /**
     * Constructor for the PayCommand.
     * Initializes the command with its name, description, category, and options.
     *
     * @param bot The Redacted bot instance.
     */
    public PayCommand(Redacted bot) {
        super(bot);
        this.name = "pay";
        this.description = "Send money to another user.";
        this.category = Category.ECONOMY;
        this.args.add(new OptionData(OptionType.USER, "user", "The user you want to send money to.", true));
        this.args.add(new OptionData(OptionType.INTEGER, "amount", "The amount of money to send.", true).setMinValue(1));
    }

    /**
     * Executes the pay command.
     * Handles the logic for transferring money from one user to another.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    public void execute(SlashCommandInteractionEvent event) {
        // Get command data
        User user = event.getUser();
        User target = Objects.requireNonNull(event.getOption("user")).getAsUser();
        EmbedBuilder embed = new EmbedBuilder().setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
        if (user.getIdLong() == target.getIdLong()) {
            // Check for invalid target
            embed.setDescription(EmbedUtils.RED_X + " You cannot pay yourself!");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        if (target.isBot()) {
            // Check if target is a bot
            embed.setDescription(EmbedUtils.RED_X + " You cannot pay bots!");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        long amount = Objects.requireNonNull(event.getOption("amount")).getAsLong();
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        String currency = economyHandler.getCurrency();

        // Check that user has necessary funds
        long balance = economyHandler.getBalance(user.getIdLong());
        if (amount > balance) {
            String value = currency + " " + EconomyHandler.FORMATTER.format(balance);
            String text = "You don't have that much money to give. You currently have " + value + " on hand.";
            embed.setDescription(EmbedUtils.RED_X + text);
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // Pay target
        economyHandler.pay(user.getIdLong(), target.getIdLong(), amount);
        String value = currency + " " + EconomyHandler.FORMATTER.format(amount);

        // Send embed message
        embed.setDescription(EmbedUtils.GREEN_TICK + " <@" + target.getId() + "> has received your " + value + ".");
        embed.setColor(EmbedColor.SUCCESS.color);
        event.replyEmbeds(embed.build()).queue();
    }
}
