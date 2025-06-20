package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command to create and send a custom embed message to a specified channel.
 * This command allows users with the appropriate permissions to create an embed
 * with a title, description, image, and thumbnail.
 *
 * @author Derrick Eberlein
 */
public class EmbedCommand extends Command {

    /**
     * Constructor for the EmbedCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public EmbedCommand(Redacted bot) {
        super(bot);
        this.name = "embed";
        this.description = "Sends a custom embed message to a specified channel";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL; // Optional: Only allow mods/staff
        this.botPermission = Permission.MESSAGE_SEND;

        this.args.add(new OptionData(OptionType.CHANNEL, "channel", "The channel to send the embed to", true)
                .setChannelTypes(ChannelType.TEXT));
        this.args.add(new OptionData(OptionType.STRING, "image", "Image URL to display in the embed", false));
        this.args.add(new OptionData(OptionType.STRING, "thumbnail", "Thumbnail URL for top-right of embed", false));
    }

    /**
     * Executes the EmbedCommand.
     * This method handles the interaction when the command is invoked.
     * It prompts the user for a title and description, then creates and sends the embed.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        TextChannel targetChannel = Objects.requireNonNull(event.getOption("channel")).getAsChannel().asTextChannel();
        String imageUrl = event.getOption("image") != null ? Objects.requireNonNull(event.getOption("image")).getAsString() : null;
        String thumbnailUrl = event.getOption("thumbnail") != null ? Objects.requireNonNull(event.getOption("thumbnail")).getAsString() : null;

        event.reply("📌 What should the **title** be? Please type it below.").setEphemeral(true).queue();

        bot.getShardManager().addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent titleEvent) {
                if (!titleEvent.getAuthor().equals(event.getUser())) return;
                if (!titleEvent.getChannel().equals(event.getChannel())) return;

                String title = titleEvent.getMessage().getContentRaw();
                titleEvent.getMessage().delete().queue();

                titleEvent.getChannel().sendMessage("✍️ Got the title! Now send the **description**.").queue();

                bot.getShardManager().addEventListener(new ListenerAdapter() {
                    @Override
                    public void onMessageReceived(@NotNull MessageReceivedEvent descEvent) {
                        if (!descEvent.getAuthor().equals(event.getUser())) return;
                        if (!descEvent.getChannel().equals(event.getChannel())) return;

                        String description = descEvent.getMessage().getContentRaw();
                        descEvent.getMessage().delete().queue();

                        // Build the embed
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle(title);
                        embed.setDescription(description);
                        embed.setColor(new Color(88, 101, 242));
                        embed.setFooter("Posted by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                        embed.setTimestamp(Instant.now());

                        if (imageUrl != null) embed.setImage(imageUrl);
                        if (thumbnailUrl != null) embed.setThumbnail(thumbnailUrl);

                        // Send embed and save to DB
                        targetChannel.sendMessageEmbeds(embed.build()).queue(sentMessage -> {
                            SavedEmbed saved = new SavedEmbed();
                            saved.setMessageId(sentMessage.getId());
                            saved.setChannelId(targetChannel.getId());
                            saved.setGuildId(Objects.requireNonNull(event.getGuild()).getIdLong());
                            saved.setTitle(title);
                            saved.setDescription(description);
                            saved.setAuthorId(event.getUser().getId());
                            saved.setTimestamp(Instant.now());
                            saved.setImageUrl(imageUrl);
                            saved.setThumbnailUrl(thumbnailUrl);
                            saved.setEmojiRoleMap(new HashMap<>());

                            GuildData.getDatabase()
                                    .getSavedEmbedsCollection(event.getGuild().getIdLong())
                                    .insertOne(saved);

                            descEvent.getChannel().sendMessage("✅ Embed created and sent to " + targetChannel.getAsMention()).queue();
                        });

                        bot.getShardManager().removeEventListener(this);
                    }
                });

                bot.getShardManager().removeEventListener(this);
            }
        });
    }

    /**
     * Formats a multiline string for better readability in embeds.
     * It can optionally format headers like "1. Title" into bold.
     *
     * @param input The input string to format.
     * @return The formatted string.
     */
    private String formatMultiline(String input) {
        // Optionally format headers like "1. Title" into bold
        return Arrays.stream(input.split("\n"))
                .map(line -> line.matches("^\\d+\\.\\s.*") ? "**" + line + "**" : line)
                .collect(Collectors.joining("\n"));
    }
}
