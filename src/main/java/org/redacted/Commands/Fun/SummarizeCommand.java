package org.redacted.Commands.Fun;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.OpenAIClient;

import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command that summarizes recent messages in a channel using OpenAI's summarization capabilities.
 * It retrieves a specified number of recent messages and generates a summary.
 *
 * @author Derrick Eberlein
 */
public class SummarizeCommand extends Command {

    /**
     * Constructor for the SummarizeCommand.
     * Initializes the command with its name, description, and required arguments.
     *
     * @param bot The Redacted bot instance.
     */
    public SummarizeCommand(Redacted bot) {
        super(bot);

        this.name = "summarize";
        this.description = "Summarize recent messages in this channel.";
        this.category = Category.FUN;

        this.args.add(new OptionData(OptionType.INTEGER, "amount", "Number of recent messages to summarize (1–100)", false));
    }

    /**
     * Executes the summarize command.
     * Retrieves recent messages from the channel and generates a summary using OpenAI.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();

        int amount; // default
        if (event.getOption("amount") != null) {
            amount = Math.min(Math.max(1, Objects.requireNonNull(event.getOption("amount")).getAsInt()), 100); // clamp 1–100
        } else {
            amount = 50;
        }

        event.deferReply().setEphemeral(true).queue();

        channel.getHistory().retrievePast(amount).queue(messages -> {
            String context = messages.stream()
                    .sorted(Comparator.comparing(ISnowflake::getTimeCreated))
                    .map(msg -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(msg.getAuthor().getName()).append(": ");

                        // Append message content if any
                        if (!msg.getContentDisplay().isBlank()) {
                            sb.append(msg.getContentDisplay());
                        }

                        // Append attachment links
                        if (!msg.getAttachments().isEmpty()) {
                            for (var attachment : msg.getAttachments()) {
                                if (attachment.isImage()) {
                                    sb.append(" [Image Attachment] ").append(attachment.getUrl());
                                } else {
                                    sb.append(" [File Attachment] ").append(attachment.getUrl());
                                }
                            }
                        }

                        return sb.toString();
                    })
                    .collect(Collectors.joining("\n"));

            if (context.isBlank()) {
                event.getHook().sendMessage("❌ No messages to summarize.").queue();
                return;
            }

            bot.getThreadPool().submit(() -> {
                try {
                    String summary = OpenAIClient.summarize(context);
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("📝 **Summary of the Last " + amount + " Messages**")
                            .setDescription(summary.length() > 4000 ? summary.substring(0, 3997) + "..." : summary)
                            .setColor(new Color(88, 101, 242))
                            .setTimestamp(Instant.now())
                            .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ Failed to summarize due to an error.").queue();
                }
            });
        });
    }
}
