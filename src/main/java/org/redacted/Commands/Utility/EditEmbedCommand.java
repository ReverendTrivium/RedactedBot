package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
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
import org.redacted.listeners.ButtonListener;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command to edit a previously saved embed message.
 * This command allows users with the appropriate permissions to modify an embed message
 * that was saved in the database.
 *
 * @author Derrick Eberlein
 */
public class EditEmbedCommand extends Command {

    /**
     * Constructor for the EditEmbedCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public EditEmbedCommand(Redacted bot) {
        super(bot);
        this.name = "editembed";
        this.description = "Edits a previously saved embed message";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL; // Optional: Only allow mods/staff
        this.botPermission = Permission.MESSAGE_MANAGE;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "The ID of the embed message to edit", true));
        this.args.add(new OptionData(OptionType.STRING, "image", "New image URL (optional)", false));
        this.args.add(new OptionData(OptionType.STRING, "thumbnail", "New thumbnail URL (optional)", false));
    }

    /**
     * Executes the EditEmbedCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embed message by its ID and allows the user to edit its title and description.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // ✅ Defer so event.getHook() is valid later
        event.deferReply(true).queue();

        String messageId = event.getOption("messageid").getAsString();
        String imageUrl = event.getOption("image") != null ? event.getOption("image").getAsString() : null;
        String thumbnailUrl = event.getOption("thumbnail") != null ? event.getOption("thumbnail").getAsString() : null;

        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().sendMessage("❌ This command can only be used in a server.").queue();
            return;
        }

        MongoCollection<SavedEmbed> collection = GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());
        SavedEmbed saved = collection.find(Filters.eq("messageId", messageId)).first();

        if (saved == null) {
            event.getHook().sendMessage("❌ Embed not found for that message ID.").queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(saved.getChannelId());
        if (channel == null) {
            event.getHook().sendMessage("❌ Original channel not found.").queue();
            return;
        }

        event.getHook().sendMessage("📌 Please type the **new title** for the embed.").queue();

        ListenerAdapter titleListener = new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent titleEvent) {
                if (!titleEvent.getAuthor().equals(event.getUser())) return;
                if (!titleEvent.getChannel().equals(event.getChannel())) return;

                String newTitle = titleEvent.getMessage().getContentRaw();
                titleEvent.getMessage().delete().queue();

                bot.getShardManager().removeEventListener(this);

                titleEvent.getChannel().sendMessage("✍️ Got the title. Now send the **new description**.").queue();

                ListenerAdapter descListener = new ListenerAdapter() {
                    @Override
                    public void onMessageReceived(@NotNull MessageReceivedEvent descEvent) {
                        if (!descEvent.getAuthor().equals(event.getUser())) return;
                        if (!descEvent.getChannel().equals(event.getChannel())) return;

                        String newDescription = descEvent.getMessage().getContentRaw();
                        descEvent.getMessage().delete().queue();

                        bot.getShardManager().removeEventListener(this);

                        String finalImageUrl = imageUrl != null ? imageUrl : saved.getImageUrl();
                        String finalThumbnailUrl = thumbnailUrl != null ? thumbnailUrl : saved.getThumbnailUrl();

                        EmbedBuilder updated = new EmbedBuilder()
                                .setTitle(newTitle)
                                .setDescription(newDescription)
                                .setColor(new Color(88, 101, 242))
                                .setTimestamp(Instant.now())
                                .setFooter("Edited by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

                        if (finalImageUrl != null && !finalImageUrl.isBlank()) updated.setImage(finalImageUrl);
                        if (finalThumbnailUrl != null && !finalThumbnailUrl.isBlank()) updated.setThumbnail(finalThumbnailUrl);

                        String embedUUID = UUID.randomUUID().toString();
                        String userId = event.getUser().getId();

                        // ✅ JDA 6: use setComponents(ActionRow.of(...)) instead of addActionRow(...)
                        event.getHook()
                                .sendMessageEmbeds(updated.build())
                                .setComponents(ActionRow.of(
                                        Button.primary("editembed:confirm:" + userId + ":" + embedUUID + ":" + messageId, "✅ Confirm"),
                                        Button.danger("editembed:cancel:" + userId + ":" + embedUUID + ":" + messageId, "❌ Cancel")
                                ))
                                .queue();

                        ButtonListener.tempEmbeds.put(embedUUID,
                                new ButtonListener.TempEmbed(updated, saved, finalImageUrl, finalThumbnailUrl));
                    }
                };

                bot.getShardManager().addEventListener(descListener);
            }
        };

        bot.getShardManager().addEventListener(titleListener);
    }

    /**
     * Formats a multiline string for better readability in embeds.
     * This method can be used to format headers or other multiline text.
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
