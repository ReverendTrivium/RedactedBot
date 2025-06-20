package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command to delete a scheduled message by its ID.
 * This command allows users with the appropriate permissions to remove a scheduled message
 * that was previously set up in the database.
 *
 * @author Derrick Eberlein
 */
public class DeleteScheduledMessageCommand extends Command {

    /**
     * Constructor for the DeleteScheduledMessageCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public DeleteScheduledMessageCommand(Redacted bot) {
        super(bot);
        this.name = "delete-scheduled";
        this.description = "Delete a scheduled message by its ID.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
        this.args.add(new OptionData(OptionType.STRING, "id", "The ID of the scheduled message to delete.", true));
    }

    /**
     * Executes the DeleteScheduledMessageCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the scheduled message by its ID and deletes it from the database.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = Objects.requireNonNull(event.getOption("id")).getAsString();

        if (!ObjectId.isValid(messageId)) {
            event.reply("Invalid ID format. Please provide a valid message ID.").setEphemeral(true).queue();
            return;
        }

        // Convert the ID to ObjectId
        ObjectId objectId = new ObjectId(messageId);

        // Access the guild-specific collection for scheduled messages
        GuildData guildData = GuildData.get(Objects.requireNonNull(event.getGuild()), bot);
        long deletedCount = guildData.getScheduledMessagesCollection()
                .deleteOne(new Document("_id", objectId))
                .getDeletedCount();

        if (deletedCount > 0) {
            event.reply("Scheduled message with ID " + messageId + " has been successfully deleted.").setEphemeral(true).queue();
        } else {
            event.reply("No scheduled message found with the provided ID " + messageId + ".").setEphemeral(true).queue();
        }
    }
}
