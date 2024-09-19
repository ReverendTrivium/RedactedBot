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
 * Command that returns all members in the blacklist
 *
 * @author Derrick Eberlein
 */
public class BlacklistGetCommand extends Command {

    public BlacklistGetCommand(Redacted bot) {
        super(bot);
        this.name = "get"; // Subcommand name
        this.description = "Gets all users in the blacklist";
        this.category = Category.STAFF;
        this.args = new ArrayList<>(); // No additional arguments needed
        this.permission = Permission.MANAGE_SERVER;
    }

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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
