package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;

/**
 * Command that displays and modifies a guild's economy config.
 *
 * @author Derrick Eberlein
 */
public class EconomyCommand extends Command {

    /**
     * Constructor for the EconomyCommand.
     * Initializes the command with its name, description, category, and subcommands.
     *
     * @param bot The Redacted bot instance.
     */
    public EconomyCommand(Redacted bot) {
        super(bot);
        this.name = "manage-economy";
        this.description = "Modify this server's economy config.";
        this.category = Category.ECONOMY;
        this.permission = Permission.MANAGE_SERVER;
        this.subCommands.add(new SubcommandData("currency", "Set the currency symbol.")
                .addOptions(new OptionData(OptionType.STRING, "symbol", "The emoji or symbol to set as the currency.")));
        this.subCommands.add(new SubcommandData("add", "Add money to the balance of a user.")
                .addOptions(new OptionData(OptionType.USER, "user", "The user you want to add money to.", true))
                .addOptions(new OptionData(OptionType.INTEGER, "amount", "The amount of money to add", true)));
        this.subCommands.add(new SubcommandData("remove", "Remove money from the balance of a user.")
                .addOptions(new OptionData(OptionType.USER, "user", "The user you want to remove money from.", true))
                .addOptions(new OptionData(OptionType.INTEGER, "amount", "The amount of money to remove", true)));
    }

    /**
     * Executes the economy command.
     * Handles subcommands for modifying the economy configuration, such as changing the currency symbol or adding/removing money from users.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();

        String text = "";
        switch(Objects.requireNonNull(event.getSubcommandName())) {
            case "currency" -> {
                OptionMapping symbolOption = event.getOption("symbol");
                if (symbolOption != null) {
                    // Update currency symbol
                    String symbol = symbolOption.getAsString();
                    if (symbol.length() > 100) {
                        text = "The maximum length for the currency symbol is 100 characters!";
                        event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
                        return;
                    } else if (!symbol.startsWith("<") && !symbol.endsWith(">") && symbol.matches(".*[0-9].*")) {
                        text = "The currency symbol cannot contain numbers!";
                        event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
                        return;
                    }
                    economyHandler.setCurrency(symbol);
                    text = EmbedUtils.BLUE_TICK + " The currency symbol has been updated to **" + symbol + "**";
                } else {
                    // Reset currency symbol to default
                    economyHandler.resetCurrency();
                    text = EmbedUtils.BLUE_TICK + " The currency symbol has been reset.";
                }
            }
            case "add" -> {
                User user = Objects.requireNonNull(event.getOption("user")).getAsUser();
                long amount = Objects.requireNonNull(event.getOption("amount")).getAsLong();
                economyHandler.addMoney(user.getIdLong(), amount);
                String currency = economyHandler.getCurrency() + " **" + EconomyHandler.FORMATTER.format(amount) + "**";
                text = EmbedUtils.BLUE_TICK + " Successfully added " + currency + " to " + user.getAsMention();
            }
            case "remove" -> {
                User user = Objects.requireNonNull(event.getOption("user")).getAsUser();
                long amount = Objects.requireNonNull(event.getOption("amount")).getAsLong();
                long networth = economyHandler.getNetworth(user.getIdLong());
                if (amount > networth) {
                    text = "You cannot remove more money than a user has!";
                    event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
                    return;
                }
                economyHandler.removeMoney(user.getIdLong(), amount);
                String currency = economyHandler.getCurrency() + " **" + EconomyHandler.FORMATTER.format(amount) + "**";
                text = EmbedUtils.BLUE_TICK + " Successfully removed " + currency + " from " + user.getAsMention();
            }
        }
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
