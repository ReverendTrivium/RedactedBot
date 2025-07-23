package org.redacted.Commands.Moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.util.moderation.MuteService;
import org.redacted.util.moderation.RoleService;

import java.time.Duration;

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

        muteService.mute(target, muteRole, duration, guild);
        event.reply("Muted " + target.getEffectiveName() + " for " + durationStr).setEphemeral(true).queue();
    }
}

