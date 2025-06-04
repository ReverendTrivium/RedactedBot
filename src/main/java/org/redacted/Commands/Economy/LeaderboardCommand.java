package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Economy;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;
import org.redacted.util.embeds.EmbedColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command that displays the leaderboard of users with the highest net worth.
 *
 * @author Derrick Eberlein
 */
public class LeaderboardCommand extends Command {

    private static final int USERS_PER_PAGE = 10;

    public LeaderboardCommand(Redacted bot) {
        super(bot);
        this.name = "leaderboard";
        this.description = "Show the leaderboard of users with the most money.";
        this.category = Category.ECONOMY;
        this.subCommands.add(new SubcommandData("rank", "Shows the server's economy leaderboard"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (Objects.equals(event.getSubcommandName(), "rank")) {
            displayLeaderboard(event);
        }
    }

    private void displayLeaderboard(SlashCommandInteractionEvent event) {
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        List<Economy> leaderboard = economyHandler.getLeaderboardAsList();

        List<MessageEmbed> embeds = buildLeaderboardMenu(leaderboard, event, economyHandler);
        if (embeds.isEmpty()) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Economy Leaderboard")
                    .setDescription("No data to display.")
                    .setColor(EmbedColor.DEFAULT.color);
            event.replyEmbeds(embed.build()).queue();
            return;
        }

        // Send paginated leaderboard
        ButtonListener.sendPaginatedMenu(event.getUser().getId(), event.replyEmbeds(embeds.get(0)), embeds);
    }

    /**
     * Builds a menu with the top users on the leaderboard.
     *
     * @param leaderboard the sorted list of users by net worth.
     * @param event the command interaction event.
     * @return a list of MessageEmbed objects for pagination.
     */
    private List<MessageEmbed> buildLeaderboardMenu(List<Economy> leaderboard, SlashCommandInteractionEvent event, EconomyHandler economyHandler) {
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆  Economy Leaderboard");
        embed.setColor(EmbedColor.DEFAULT.color);

        int counter = 0;
        int rank = 1;
        for (Economy profile : leaderboard) {
            if (profile.getUser() == null) continue; // Still good to skip null user IDs

            Member member = event.getGuild().getMemberById(profile.getUser());
            String userName = member != null ? member.getEffectiveName() : "\uD83D\uDEAB Left the server";

            long balance = profile.getBalance() != null ? profile.getBalance() : 0;
            long bank = profile.getBank() != null ? profile.getBank() : 0;
            long total = balance + bank;

            embed.appendDescription("**" + rank + ". " + userName + "**\n" +
                    "Balance: " + economyHandler.getCurrency() + " " + EconomyHandler.FORMATTER.format(total) + "\n\n");

            counter++;
            rank++;

            if (counter % USERS_PER_PAGE == 0) {
                embeds.add(embed.build());
                embed.setDescription("");
                counter = 0;
            }
        }

        if (counter != 0) {
            embeds.add(embed.build()); // Add the last embed if not full
        }

        return embeds;
    }
}
