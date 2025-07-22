package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;


/**
 * Command that toggles repeat mode for music queue.
 *
 * @author Derrick Eberlein
 */
public class RepeatCommand extends Command {

    /**
     * Constructor for RepeatCommand.
     * @param bot An instance of Redacted.
     */
    public RepeatCommand(Redacted bot) {
        super(bot);
        this.name = "repeat";
        this.description = "Toggles the repeat mode.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the repeat command.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.getMusicListener().getMusic(event, false);
        if (music == null) return;

        music.loop();
        String text;
        if (music.isLoop()) {
            text = ":repeat: Repeat has been enabled.";
        } else {
            text = ":repeat: Repeat has been disabled.";
        }
        event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
