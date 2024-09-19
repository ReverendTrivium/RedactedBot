package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;

import java.util.Objects;

/**
 * Command that displays relevant server information.
 *
 * @author Derrick Eberlein
 */
public class Server extends Command {

    public Server(Redacted bot) {
        super(bot);
        this.name = "server";
        this.description = "Display information about this discord server.";
        this.category = Category.UTILITY;
    }

    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        String guildTime = TimeFormat.RELATIVE.format(Objects.requireNonNull(guild).getTimeCreated().toInstant().toEpochMilli());

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle(guild.getName())
                .addField(":id: Server ID:", guild.getId(), true)
                .addField(":calendar: Created On", guildTime, true)
                .addField(":crown: Owned By", "<@" + guild.getOwnerId() + ">", true);

        String members = String.format("**%s** Boosts :sparkles:", guild.getBoostCount());
        embed.addField(":busts_in_silhouette: Members (" + guild.getMemberCount() + ")", members, true);

        int textChannels = guild.getTextChannels().size();
        int voiceChannels = guild.getVoiceChannels().size();
        String channels = String.format("**%s** Text\n**%s** Voice", textChannels, voiceChannels);
        embed.addField(":speech_balloon: Channels (" + (textChannels + voiceChannels) + ")", channels, true);

        String other = "**Verification Level:** " + guild.getVerificationLevel().getKey();
        embed.addField(":earth_africa: Other:", other, true);

        String roles = "To see a list with all roles use **/roles**";
        embed.addField(":closed_lock_with_key:  Roles (" + guild.getRoles().size() + ")", roles, true);

        event.replyEmbeds(embed.build()).queue();
    }
}
