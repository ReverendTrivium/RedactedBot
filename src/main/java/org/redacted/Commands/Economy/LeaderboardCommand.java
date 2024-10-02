package org.redacted.Commands.Economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Economy;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;

import java.util.List;
import java.util.Objects;

/**
 * Command that shows the richest users in the server.
 *
 * @author Derrick Eberlein
 */
public class LeaderboardCommand extends Command {

    public LeaderboardCommand(Redacted bot) {
        super(bot);
        this.name = "leaderboard";
        this.description = "Shows the richest users in the server.";
        this.category = Category.ECONOMY;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();

        // Start from page 1 when first called
        showLeaderboard(event, economyHandler, 0);
    }

    /**
     * Show leaderboard paginated.
     *
     * @param event          The command interaction event.
     * @param economyHandler The handler to fetch leaderboard data.
     * @param page           The page number to display (0-indexed).
     */
    private void showLeaderboard(SlashCommandInteractionEvent event, EconomyHandler economyHandler, int page) {
        int pageSize = 10;
        int offset = page * pageSize;

        // Get the leaderboard data from the handler
        List<Economy> leaderboard = economyHandler.getLeaderboardAsList();

        // Calculate total pages
        int totalPages = (int) Math.ceil((double) leaderboard.size() / pageSize);

        // Ensure the page is valid
        if (page < 0 || page >= totalPages) {
            event.reply("Invalid page!").setEphemeral(true).queue();
            return;
        }

        // Get the current page data
        List<Economy> pageData = leaderboard.subList(offset, Math.min(offset + pageSize, leaderboard.size()));

        // Create an embed
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Leaderboard - Richest Users");
        embed.setColor(EmbedColor.DEFAULT.color);

        // Add users to the embed
        for (int i = 0; i < pageData.size(); i++) {
            Economy profile = pageData.get(i);

            // Fetch Member instead of User to get their guild nickname
            Member member = event.getGuild().retrieveMemberById(profile.getUser()).complete();
            String displayName = (member != null && member.getNickname() != null) ? member.getNickname() : member.getUser().getName();

            long total = profile.getBalance() + (profile.getBank() != null ? profile.getBank() : 0);

            embed.addField((offset + i + 1) + ". " + displayName,
                    "Balance: " + economyHandler.getCurrency() + " " + EconomyHandler.FORMATTER.format(total), false);
        }

        // Add footer with current page and total pages
        embed.setFooter("Page " + (page + 1) + " of " + totalPages);

        // Prepare buttons for pagination
        Button prevButton = Button.primary("leaderboard:prev:" + page, "Previous").asDisabled();
        Button nextButton = Button.primary("leaderboard:next:" + page, "Next").asDisabled();

        if (page > 0) {
            prevButton = prevButton.asEnabled();
        }
        if (page < totalPages - 1) {
            nextButton = nextButton.asEnabled();
        }

        // Reply with the embed and pagination buttons
        event.replyEmbeds(embed.build())
                .addActionRow(prevButton, nextButton)
                .queue();
    }
}
