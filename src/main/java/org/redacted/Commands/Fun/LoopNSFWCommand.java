package org.redacted.Commands.Fun;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.BotCommands;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class LoopNSFWCommand extends Command {

    private static Timer timer;

    public LoopNSFWCommand(Redacted bot) {
        super(bot);
        this.name = "loopnsfw";
        this.description = "Loop NSFW posts from a specific category every 10 minutes.";
        this.args.add(new OptionData(OptionType.STRING, "category", "The type of nsfw image to generate")
                .addChoice("porn", "porn")
                .addChoice("boobs", "boobs")
                .addChoice("gay", "gay")
                .addChoice("lesbian", "lesbian")
                .addChoice("furry", "furry")
                .addChoice("hentai", "hentai")
                .addChoice("public", "public")
                .addChoice("bg3", "bg3")
                .addChoice("raven", "raven")
                .addChoice("mihoyo", "mihoyo")
                .addChoice("cyberpunk", "cyberpunk")
                .addChoice("milf", "milf")
                .addChoice("japanese", "japanese")
                .addChoice("asian", "asian")
                .addChoice("black", "black")
                .addChoice("white", "white")
                .addChoice("india", "india")
                .addChoice("arab", "arab")
                .addChoice("native", "native").setRequired(true));
        this.permission = Permission.MANAGE_SERVER;
        this.category = Category.FUN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        String category = Objects.requireNonNull(event.getOption("category")).getAsString();
        NSFWCommand nsfwCommand = (NSFWCommand) BotCommands.commandsMap.get("nsfw");

        // Print the first image immediately
        nsfwCommand.executeCategory(event.getChannel().getId(), category, event);

        // Start the timer for subsequent images
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                nsfwCommand.executeCategory(event.getChannel().getId(), category, event);
            }
        }, 6000, 6000); // 600000ms = 10 minutes

        event.getHook().sendMessage("Looping NSFW posts from category: " + category).queue();
    }

    public static void stopLoop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}