package org.redacted.Commands.Music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

/**
 * Command that changes volume of the music player.
 *
 * @author Derrick Eberlein
 */
public class VolumeCommand extends Command {

    /**
     * Constructor for the VolumeCommand.
     *
     * @param bot The Redacted bot instance.
     */
    public VolumeCommand(Redacted bot) {
        super(bot);
        this.name = "volume";
        this.description = "Changes the volume of the music.";
        this.category = Category.MUSIC;
        this.args.add(new OptionData(OptionType.INTEGER, "amount", "Enter value between 0-100 to set", true)
                .setMinValue(0)
                .setMaxValue(100));
    }

    /**
     * Executes the volume command, changing the volume of the music player.
     *
     * @param event The SlashCommandInteractionEvent that triggered this command.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int volume = event.getOption("amount").getAsInt();
        MusicHandler music = bot.getMusicListener().getMusic(event, true);
        if (music == null) return;
        try {
            if (volume < 0 || volume > 100) {
                throw new NumberFormatException();
            }
            music.setVolume(volume);
            String text = String.format(":loud_sound: Set the volume to `%s%%`", volume);
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        } catch (@NotNull NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}

        String text = "You must specify a volume between 0-100";
        event.replyEmbeds(EmbedUtils.createError(text)).queue();
    }
}
