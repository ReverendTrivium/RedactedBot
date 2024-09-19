package org.redacted.Commands.Blacklist;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class BlacklistClearCommand extends Command {

    private static final Map<String, Long> confirmationRequests = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 60 * 1000; // 60 seconds

    public BlacklistClearCommand(Redacted bot) {
        super(bot);
        this.name = "clear"; // Subcommand name
        this.description = "Clears all entries in the blacklist.";
        this.category = Category.STAFF;
        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (confirmationRequests.containsKey(userId)) {
            long requestTime = confirmationRequests.get(userId);
            if (System.currentTimeMillis() - requestTime < CONFIRMATION_TIMEOUT) {
                // Clear the guild-specific blacklist
                MongoCollection<Document> blacklistCollection = bot.database.getGuildCollection(guildId, "blacklist");
                long deletedCount = blacklistCollection.deleteMany(new Document()).getDeletedCount();

                if (deletedCount > 0) {
                    event.reply("Successfully cleared " + deletedCount + " entries from the blacklist.").setEphemeral(true).queue();
                } else {
                    event.reply("The blacklist is already empty.").setEphemeral(true).queue();
                }

                // Remove confirmation request
                confirmationRequests.remove(userId);
                return;
            } else {
                // Remove expired confirmation request
                confirmationRequests.remove(userId);
            }
        }

        // Ask for confirmation
        event.reply("Are you sure you want to clear the blacklist? Type /blacklist-clear again within 60 seconds to confirm.").setEphemeral(true).queue();
        confirmationRequests.put(userId, System.currentTimeMillis());

        // Schedule a task to remove the confirmation request after the timeout
        event.getJDA().getGatewayPool().schedule(() -> confirmationRequests.remove(userId), CONFIRMATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }
}