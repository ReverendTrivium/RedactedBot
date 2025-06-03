package org.redacted.Commands.Fun;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
        this.description = "Loop NSFW posts from a specific category every minute.";
        this.args.add(new OptionData(OptionType.STRING, "category", "The type of nsfw image to generate")
                .addChoice("porn", "porn")
                .addChoice("boobs", "boobs")
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
                .addChoice("white", "white").setRequired(true));
        this.permission = Permission.MANAGE_SERVER;
        this.category = Category.FUN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        String category = Objects.requireNonNull(event.getOption("category")).getAsString();
        NSFWCommand nsfwCommand = (NSFWCommand) BotCommands.commandsMap.get("nsfw");

        // Check to ensure this is an NSFW Channel
        if (!event.getChannel().asTextChannel().isNSFW()) {
            System.out.println("This is not an NSFW Channel");
            event.getHook().sendMessage("This is not an NSFW Channel, cannot run NSFW Command in this channel").queue();
            return;
        }

        String channelId = event.getChannel().getId();

        // Print the first image immediately
        TextChannel channel = bot.getShardManager().getTextChannelById(channelId);
        if (channel != null) {
            nsfwCommand.executeCategory(channel, category);
        }

        // Start the timer for subsequent images
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                TextChannel repeatChannel = bot.getShardManager().getTextChannelById(channelId);
                if (repeatChannel != null) {
                    nsfwCommand.executeCategory(repeatChannel, category);
                }
            }
        }, 6000, 60000); // Delay 6 seconds, repeat every 10 minutes

        event.getHook().sendMessage("Looping NSFW posts from category: " + category).queue();
    }

    public static void stopLoop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}