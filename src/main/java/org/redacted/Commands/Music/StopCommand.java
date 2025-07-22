package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that clears the music queue and stops music
 *
 * @author Derrick Eberlein
 */
public class StopCommand extends Command {

    /**
     * Constructor for the StopCommand.
     *
     * @param bot The Redacted bot instance.
     */
    public StopCommand(Redacted bot) {
        super(bot);
        this.name = "stop";
        this.description = "Stop the current song and clear the entire music queue.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the stop command, which stops the current song and clears the music queue.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler musicHandler = GuildData.get(event.getGuild(), bot).musicHandler;
        if (musicHandler == null) {
            String text = "The music player is already stopped!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        } else {
            musicHandler.disconnect();
            event.getGuild().getAudioManager().closeAudioConnection();
            String text = ":stop_button: Stopped the music player.";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        }
    }
}
