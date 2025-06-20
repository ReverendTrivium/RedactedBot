package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.conversions.Bson;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command to delete a previously saved embed message.
 * This command allows users with the appropriate permissions to remove an embed message
 * that was saved in the database.
 *
 * @author Derrick Eberlein
 */
public class DeleteEmbedCommand extends Command {

    /**
     * Constructor for the DeleteEmbedCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public DeleteEmbedCommand(Redacted bot) {
        super(bot);
        this.name = "deleteembed";
        this.description = "Deletes a previously saved embed message";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL; // Optional: Only allow mods/staff
        this.botPermission = Permission.MESSAGE_MANAGE;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "The ID of the embed message to delete", true));
    }

    /**
     * Executes the DeleteEmbedCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embed message by its ID and deletes it from the channel and database.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = Objects.requireNonNull(event.getOption("messageid")).getAsString();

        Guild guild = event.getGuild();
        MongoCollection<SavedEmbed> collection = GuildData
                .getDatabase()
                .getSavedEmbedsCollection(Objects.requireNonNull(guild).getIdLong());

        Bson filter = Filters.eq("messageId", messageId);
        SavedEmbed saved = collection.find(filter).first();

        if (saved == null) {
            event.reply("❌ No saved embed found with that message ID.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(saved.getChannelId());
        if (channel == null) {
            event.reply("❌ The channel for this message no longer exists.").setEphemeral(true).queue();
            return;
        }

        channel.deleteMessageById(messageId).queue(success -> {
            collection.deleteOne(filter);
            event.reply("✅ Embed message deleted.").setEphemeral(true).queue();
        }, failure -> event.reply("❌ Failed to delete the message.").setEphemeral(true).queue());
    }
}
