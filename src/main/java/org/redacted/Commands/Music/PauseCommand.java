package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that pauses music player.
 *
 * @author Derrick Eberlein
 */
public class PauseCommand extends Command {

    /**
     * Constructor for the PauseCommand.
     *
     * @param bot The Redacted bot instance.
     */
    public PauseCommand(Redacted bot) {
        super(bot);
        this.name = "pause";
        this.description = "Pause the current playing track.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the pause command to pause the music player.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.getMusicListener().getMusic(event, false);
        if (music == null) return;

        if (music.isPaused()) {
            String text = "The player is already paused!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        } else {
            String text = ":pause_button: Paused the music player.";
            music.pause();
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        }
    }
}
