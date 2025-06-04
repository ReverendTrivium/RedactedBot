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

import java.util.Map;
import java.util.concurrent.*;

import java.util.Objects;

public class LoopNSFWCommand extends Command {

    private static final Map<String, ScheduledFuture<?>> loopTasks = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public LoopNSFWCommand(Redacted bot) {
        super(bot);
        this.name = "loopnsfw";
        this.description = "Loop NSFW posts from a specific category every minute in the current channel.";
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
                .addChoice("white", "white")
                .setRequired(true));
        this.permission = Permission.MANAGE_SERVER;
        this.category = Category.FUN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        String category = Objects.requireNonNull(event.getOption("category")).getAsString();
        NSFWCommand nsfwCommand = (NSFWCommand) BotCommands.commandsMap.get("nsfw");
        String channelId = event.getChannel().getId();

        if (!event.getChannel().asTextChannel().isNSFW()) {
            event.getHook().sendMessage("This is not an NSFW Channel, cannot run NSFW Command in this channel").queue();
            return;
        }

        if (loopTasks.containsKey(channelId)) {
            event.getHook().sendMessage("This channel is already looping NSFW posts. Use /stoploop to cancel.").queue();
            return;
        }

        TextChannel channel = bot.getShardManager().getTextChannelById(channelId);
        if (channel != null) {
            nsfwCommand.executeCategory(channel, category);
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                TextChannel repeatChannel = bot.getShardManager().getTextChannelById(channelId);
                if (repeatChannel != null) {
                    nsfwCommand.executeCategory(repeatChannel, category);
                }
            } catch (Exception e) {
                System.err.println("Error in scheduled NSFW loop for channel " + channelId);
                e.printStackTrace();
            }
        }, 6, 60, TimeUnit.SECONDS);

        loopTasks.put(channelId, task);
        event.getHook().sendMessage("Started looping NSFW posts from category: " + category + " in this channel.").queue();
    }

    public static boolean stopLoop(String channelId) {
        ScheduledFuture<?> task = loopTasks.remove(channelId);
        if (task != null) {
            task.cancel(false);
            return true;
        }
        return false;
    }

    public static void stopAllLoops() {
        for (Map.Entry<String, ScheduledFuture<?>> entry : loopTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        loopTasks.clear();
    }

    public static boolean isLooping(String channelId) {
        return loopTasks.containsKey(channelId);
    }
}