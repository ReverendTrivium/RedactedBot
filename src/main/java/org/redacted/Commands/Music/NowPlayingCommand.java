package org.redacted.Commands.Music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that displays the currently playing song.
 *
 * @author Derrick Eberlein
 */
public class NowPlayingCommand extends Command {

    /**
     * Constructor for the NowPlayingCommand.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public NowPlayingCommand(Redacted bot) {
        super(bot);
        this.name = "now-playing";
        this.description = "Check what song is currently playing.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the now-playing command.
     * This method is called when the command is invoked.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Verify the Music Manager isn't null.
        MusicHandler music = GuildData.get(event.getGuild(), bot).musicHandler;
        if (music == null) {
            String text = ":sound: Not currently playing any music!";
            event.replyEmbeds(EmbedUtils.createDefault(text)).setEphemeral(true).queue();
            return;
        }

        // Get currently playing track
        AudioTrack nowPlaying = !music.getQueue().isEmpty() ? music.getQueue().getFirst() : null;
        if (nowPlaying == null) {
            String text = ":sound: Not currently playing any music!";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }
        event.replyEmbeds(MusicHandler.displayTrack(nowPlaying, music)).queue();
    }
}
