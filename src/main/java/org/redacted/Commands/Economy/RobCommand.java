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
import org.redacted.Handlers.economy.EconomyReply;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;

/**
 * Command that steals money from another user.
 *
 * @author Derrick Eberlein
 */
public class RobCommand extends Command {

    public RobCommand(Redacted bot) {
        super(bot);
        this.name = "rob";
        this.description = "Attempt to steal money from another user.";
        this.category = Category.ECONOMY;
        this.args.add(new OptionData(OptionType.USER, "user", "The user you want to rob.", true));
    }

    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        User target = Objects.requireNonNull(event.getOption("user")).getAsUser();
        EmbedBuilder embed = new EmbedBuilder().setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
        if (user.getIdLong() == target.getIdLong()) {
            // Check for invalid target
            embed.setDescription(EmbedUtils.RED_X + " You cannot rob yourself!");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        if (target.isBot()) {
            // Check if target is a bot
            embed.setDescription(EmbedUtils.RED_X + " You cannot rob bots, they are too powerful for you!");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        // Check for timeout
        EconomyHandler economyHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getEconomyHandler();
        Long timeout = economyHandler.getTimeout(user.getIdLong(), EconomyHandler.TIMEOUT_TYPE.ROB);
        if (timeout != null && System.currentTimeMillis() < timeout) {
            // On timeout
            String timestamp = economyHandler.formatTimeout(timeout);
            embed.setDescription(":stopwatch: You can attempt to rob another member " + timestamp + ".");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            // Rob target
            EconomyReply reply = economyHandler.rob(user.getIdLong(), target.getIdLong());
            embed.setColor(reply.isSuccess() ? EmbedColor.SUCCESS.color : EmbedColor.ERROR.color);
            embed.setDescription(reply.getResponse());
            event.replyEmbeds(embed.build()).queue();
        }
    }
}
