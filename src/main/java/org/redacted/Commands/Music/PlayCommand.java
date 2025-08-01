package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.listeners.MusicListener;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that plays a song from YouTube, Spotify, or Apple Music.
 * Accepts either direct links or a search query (YouTube search supported).
 * <p>
 * Usage: /play <query>
 * Examples:
 *   /play <a href="https://www.youtube.com/watch?v=abc123">...</a>
 *   /play who let the dogs out
 *
 * @author Derrick Eberlein
 */
public class PlayCommand extends Command {

    /**
     * Constructor for the PlayCommand.
     * @param bot The Redacted bot instance.
     */
    public PlayCommand(Redacted bot) {
        super(bot);
        this.name = "play";
        this.description = "Play a song from YouTube, or Spotify.";
        this.category = Category.MUSIC;
        this.args.add(new OptionData(OptionType.STRING, "query", "A YouTube/Spotify link or search query.")
        .setRequired(true));
    }

    /**
     * Executes the play command.
     * This method is called when the command is invoked.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String query = event.getOption("query") != null
                ? event.getOption("query").getAsString()
                : null;

        MusicHandler music = bot.musicListener.getMusic(event, true);

        if (query == null || query.isBlank()) {
            event.replyEmbeds(EmbedUtils.createError("Please provide a song name or link.")).setEphemeral(true).queue();
            return;
        }

        // If not a URL, use YouTube search
        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            query = "ytsearch:" + query;
        }


        // ✅ Defer response while LavaPlayer loads
        event.deferReply().queue();

        MusicListener musicListener = bot.getMusicListener();

        System.out.println("MusicListener in bot: " + bot.getMusicListener());
        if (musicListener == null) {
            event.replyEmbeds(EmbedUtils.createError("Music service is not available. Please try again later.")).setEphemeral(true).queue();
            return;
        }
        try {
            musicListener.addTrack(event, query, event.getUser().getId());
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().setEphemeral(true).editOriginal("❌ An error occurred while loading the track.").queue();
        }
    }
}
