package org.redacted.Commands.Blacklist;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command that retrieves all users in the blacklist.
 * This command allows staff members to view all entries in the blacklist.
 *
 * @author Derrick Eberlein
 */
public class BlacklistGetCommand extends Command {

    /**
     * Constructor for the BlacklistGetCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public BlacklistGetCommand(Redacted bot) {
        super(bot);
        this.name = "get"; // Subcommand name
        this.description = "Gets all users in the blacklist";
        this.category = Category.STAFF;
        this.args = new ArrayList<>(); // No additional arguments needed
        this.permission = Permission.MANAGE_SERVER;
    }

    /**
     * Executes the command when invoked.
     * It retrieves all entries from the blacklist collection and formats them for display.
     *
     * @param event The SlashCommandInteractionEvent containing the command invocation details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        MongoCollection<Document> blacklistCollection = bot.database.getGuildCollection(guildId, "blacklist");

        MongoCursor<Document> cursor = blacklistCollection.find().iterator();
        List<String> blacklistEntries = new ArrayList<>();

        try {
            while (cursor.hasNext()) {
                Document entry = cursor.next();
                String firstName = entry.getString("firstname");
                String lastName = entry.getString("lastname");
                Document socialMedia = entry.get("socialmedia", Document.class);

                if (socialMedia != null) {
                    for (String platform : socialMedia.keySet()) {
                        String handle = socialMedia.getString(platform);
                        blacklistEntries.add(String.format("Name: %s %s, %s: %s", firstName, lastName, capitalize(platform), handle));
                    }
                }
            }
        } finally {
            cursor.close();
        }

        if (blacklistEntries.isEmpty()) {
            event.reply("The blacklist is currently empty.").queue();
        } else {
            event.reply(String.join("\n", blacklistEntries)).queue();
        }
    }

    /**
     * Capitalizes the first letter of a string and converts the rest to lowercase.
     *
     * @param str The string to capitalize.
     * @return The capitalized string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
