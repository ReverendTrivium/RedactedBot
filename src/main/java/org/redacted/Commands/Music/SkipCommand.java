package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that skips the current song.
 *
 * @author Derrick Eberlein
 */
public class SkipCommand extends Command {

    /**
     * Constructor for the SkipCommand.
     *
     * @param bot The Redacted bot instance.
     */
    public SkipCommand(Redacted bot) {
        super(bot);
        this.name = "skip";
        this.description = "Skip the current song.";
        this.category = Category.MUSIC;
    }

    /**
     * Executes the skip command.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.getMusicListener().getMusic(event, false);
        if (music == null) return;

        music.skipTrack();
        ReplyCallbackAction action = event.reply(":fast_forward: Skipping...");
        if (music.getQueue().size() == 1) {
            action = action.addEmbeds(EmbedUtils.createDefault(":sound: The music queue is now empty!"));
        }
        action.queue();
    }
}
