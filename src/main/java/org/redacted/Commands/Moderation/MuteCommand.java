package org.redacted.Commands.Moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Command;
import org.redacted.Commands.Category;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.util.moderation.MuteService;
import org.redacted.util.moderation.RoleService;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Command that temporarily mutes a user in the server.
 * Requires a "Muted" role to be present in the server.
 * <p>
 * Usage: /mute <user> <duration>
 * Examples:
 *   /mute @user 30m
 *   /mute @user 1h
 *
 * @author Derrick Eberlein
 */
public class MuteCommand extends Command {

    /**
     * Constructor for the MuteCommand.
     * Initializes the command with its name, description, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public MuteCommand(Redacted bot) {
        super(bot);
        this.name = "mute";
        this.description = "Temporarily mute a user";
        this.permission = Permission.KICK_MEMBERS;
        this.category = Category.STAFF;

        this.args.add(new OptionData(OptionType.USER, "username", "User to mute")
                .setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "duration", "Duration to mute the user (e.g. 30m, 1h, 1d)")
                .setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "reason", "Reason for being muted.")
                .setRequired(false)); // Optional reason for the mute
    }

    /**
     * Executes the mute command.
     * This method is called when the command is invoked.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member target = event.getOption("username").getAsMember();
        String durationStr = event.getOption("duration").getAsString();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided.";
        Member moderator = event.getMember();
        Guild guild = event.getGuild();

        GuildData guildData = GuildData.get(guild, bot);

        /// Instance of MuteService to handle mute logic
        MuteService muteService = new MuteService(new RoleService(), guildData);

        Role muteRole = guild.getRolesByName("Mute", true).stream().findFirst().orElse(null);
        if (muteRole == null) {
            event.reply("Mute role not found. Please create a 'Mute' role.").setEphemeral(true).queue();
            return;
        }

        Duration duration = muteService.parseDuration(durationStr);
        if (duration == null) {
            event.reply("Invalid duration format. Use m/h/d (e.g. 30m, 1h)").setEphemeral(true).queue();
            return;
        }

        // Attempt to DM the user
        User user = target.getUser();
        String formatted = formatDuration(duration);
        user.openPrivateChannel().flatMap(channel ->
                channel.sendMessage("You were muted from **" + event.getGuild().getName() + "** for " + formatted + " . Reason: `" + reason + "`")
        ).queue(null, error -> {
            // Silent failure if DMs are closed
        });

        // Send a message to the mod-log channel about the mute and who did it.
        sendMuteLog(event, target, moderator, reason, formatted); // ✅ Mute log

        // Mute the user
        muteService.mute(target, muteRole, duration, guild);
        event.reply("Muted " + target.getEffectiveName() + " for " + durationStr).setEphemeral(true).queue();
    }

    /**
     * Sends a log message to the mod-log channel when a member is muted.
     * The log includes the user muted, the moderator who performed the action, and the reason for the mute.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     * @param target The member who was Muted.
     * @param moderator The member who performed the mute action.
     * @param reason The reason for muting the member.
     */
    private void sendMuteLog(SlashCommandInteractionEvent event, Member target, Member moderator, String reason, String time) {
        Guild guild = event.getGuild();
        if (guild == null) return;

        TextChannel logChannel = guild.getTextChannelsByName("mod-log", true)
                .stream()
                .findFirst()
                .orElse(null);

        if (logChannel == null) {
            System.out.println("mod-log channel not found in guild " + guild.getName());
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83D\uDD07 Member Muted")
                .addField("User", target.getUser().getAsMention() + " (`" + target.getId() + "`)", false)
                .addField("Moderator", moderator.getUser().getAsMention(), false)
                .addField("Duration", time, false)
                .addField("Reason", reason, false)
                .setColor(Color.RED)
                .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }

    /**
     * Formats a Duration object into a human-readable string.
     * The format includes days, hours, and minutes, e.g., "2 days, 3 hours, 15 minutes".
     *
     * @param duration The Duration object to format.
     * @return A formatted string representing the duration.
     */
    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder result = new StringBuilder();

        if (days > 0) result.append(days).append(" day").append(days > 1 ? "s" : "");
        if (hours > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }

        return result.length() > 0 ? result.toString() : "less than a minute";
    }
}

