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

/**
 * KickCommand.java
 * Handles the kick command for moderating members in a Discord server.
 * Allows moderators to kick users with an optional reason and logs the action.
 * This command requires the KICK_MEMBERS permission.
 * Usage: /kick <user> [reason]
 * Parameters:
 * - user: The user to kick from the server (required).
 * - reason: The reason for the kick (optional).
 * Logs the kick action in a designated mod-log channel if available.
 * Handles role hierarchy checks to ensure the bot and the moderator have permission to kick the user.
 *
 * @author Derrick Eberlein
 */
public class KickCommand extends Command {

    /**
     * Constructs a new KickCommand instance.
     * Initializes the command with its name, description, permission requirements, and arguments.
     *
     * @param bot The Redacted bot instance to register this command with.
     */
    public KickCommand(Redacted bot) {
        super(bot);
        this.name = "kick";
        this.description = "Kicks a member from the server";
        this.permission = Permission.KICK_MEMBERS;
        this.category = Category.STAFF;

        this.args.add(new OptionData(OptionType.USER, "user", "User to kick").setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "reason", "Reason for the kick").setRequired(false));
    }

    /**
     * Executes the kick command.
     * Checks permissions, attempts to kick the specified user, and logs the action.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided.";
        Member moderator = event.getMember();

        if (target == null) {
            event.reply("Could not find that member.").setEphemeral(true).queue();
            return;
        }

        if (!event.getGuild().getSelfMember().canInteract(target)) {
            event.reply("I cannot kick this member due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!moderator.canInteract(target)) {
            event.reply("You cannot kick this member due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        // Attempt to DM the user
        User user = target.getUser();
        user.openPrivateChannel().flatMap(channel ->
                channel.sendMessage("You were kicked from **" + event.getGuild().getName() + "**. Reason: `" + reason + "`")
        ).queue(null, error -> {
            // Silent failure if DMs are closed
        });

        // Kick the member
        try {
            target.kick()
                    .reason(reason)
                    .queue(
                            success -> {
                                event.reply("✅ Kicked **" + user.getAsTag() + "**.").setEphemeral(true).queue();
                                sendKickLog(event, target, moderator, reason); // ✅ Kick log
                            },
                            error -> event.reply("❌ Failed to kick the user: " + error.getMessage()).setEphemeral(true).queue()
                    );
        } catch (HierarchyException e) {
            event.reply("I can't kick this user due to role hierarchy.").setEphemeral(true).queue();
        }
    }

    /**
     * Sends a log message to the mod-log channel when a member is kicked.
     * The log includes the user kicked, the moderator who performed the action, and the reason for the kick.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     * @param target The member who was kicked.
     * @param moderator The member who performed the kick action.
     * @param reason The reason for kicking the member.
     */
    private void sendKickLog(SlashCommandInteractionEvent event, Member target, Member moderator, String reason) {
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
                .setTitle("🚫 Member Kicked")
                .addField("User", target.getUser().getAsTag() + " (`" + target.getId() + "`)", false)
                .addField("Moderator", moderator.getUser().getAsTag(), false)
                .addField("Reason", reason, false)
                .setColor(Color.RED)
                .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }

}

