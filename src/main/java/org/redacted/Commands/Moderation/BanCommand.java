package org.redacted.Commands.Moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * BanCommand.java
 * Handles the ban command for moderating members in a Discord server.
 * Allows moderators to ban users with an optional reason and logs the action.
 * This command requires the BAN_MEMBERS permission.
 * Usage: /ban <user> [reason]
 * Parameters:
 * - user: The user to ban from the server (required).
 * - reason: The reason for the ban (optional).
 * Logs the ban action in a designated mod-log channel if available.
 * Handles role hierarchy checks to ensure the bot and the moderator have permission to ban the user.
 *
 * @author Derrick Eberlein
 */
public class BanCommand extends Command {

    /**
     * Constructs a new BanCommand instance.
     * Initializes the command with its name, description, permission requirements, and arguments.
     *
     * @param bot The Redacted bot instance to register this command with.
     */
    public BanCommand(Redacted bot) {
        super(bot);
        this.name = "ban";
        this.description = "Bans a user from the server";
        this.permission = Permission.BAN_MEMBERS;
        this.category = Category.STAFF;

        this.args.add(new OptionData(OptionType.USER, "user", "User to ban").setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "reason", "Reason for the ban").setRequired(false));
    }

    /**
     * Executes the ban command.
     * Checks permissions, attempts to ban the specified user, and logs the action.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided.";
        Member moderator = event.getMember();

        if (target == null || guild == null || moderator == null) {
            event.reply("❌ Could not resolve the target or guild.").setEphemeral(true).queue();
            return;
        }

        if (!guild.getSelfMember().canInteract(target)) {
            event.reply("❌ I cannot ban this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!moderator.canInteract(target)) {
            event.reply("❌ You cannot ban this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        // Attempt to DM the user
        User user = target.getUser();
        user.openPrivateChannel().flatMap(channel ->
                channel.sendMessage("You have been **banned** from **" + guild.getName() + "**.\nReason: `" + reason + "`")
        ).queue(null, error -> {
            // Ignore if DM fails
        });

        try {
            guild.ban(target.getUser(), 0, TimeUnit.DAYS)
                .reason(reason)
                .queue(
                        success -> {
                            event.reply("✅ Banned **" + user.getAsTag() + "**.").setEphemeral(true).queue();
                            sendBanLog(event, target, moderator, reason);
                        },
                        error -> event.reply("❌ Failed to ban the user: " + error.getMessage()).setEphemeral(true).queue()
                );
        } catch (HierarchyException e) {
            event.reply("❌ I can't ban this user due to role hierarchy.").setEphemeral(true).queue();
        }
    }

    /**
     * Sends a log message to the mod-log channel when a member is banned.
     * The log includes the user who was banned, the moderator who performed the action, and the reason for the ban.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     * @param target The member who was banned.
     * @param moderator The member who executed the ban command.
     * @param reason The reason for the ban.
     */
    private void sendBanLog(SlashCommandInteractionEvent event, Member target, Member moderator, String reason) {
        TextChannel logChannel = event.getGuild().getTextChannelsByName("mod-log", true)
                .stream().findFirst().orElse(null);

        if (logChannel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔨 Member Banned")
                .addField("User", target.getUser().getAsTag() + " (`" + target.getId() + "`)", false)
                .addField("Moderator", moderator.getUser().getAsTag(), false)
                .addField("Reason", reason, false)
                .setColor(Color.RED)
                .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
}

