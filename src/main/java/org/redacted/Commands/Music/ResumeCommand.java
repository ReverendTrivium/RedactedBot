package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that unpauses the music player.
 *
 * @author Derrick Eberlein
 */
public class ResumeCommand extends Command {

    /**
     * Constructor for the ResumeCommand.
     * @param bot An instance of Redacted, the bot.
     */
    public ResumeCommand(Redacted bot) {
        super(bot);
        this.name = "resume";
        this.description = "Resumes the current paused track.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the resume command, resuming the music player
     * if it is paused.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.getMusicListener().getMusic(event, false);
        if (music == null) return;

        if (music.isPaused()) {
            music.unpause();
            String text = ":play_pause: Resuming the music player.";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        } else {
            String text = "The player is not paused!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        }
    }
}
