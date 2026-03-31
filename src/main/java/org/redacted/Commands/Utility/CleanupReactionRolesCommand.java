package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to clean up old legacy reaction role mappings from saved embeds.
 * This command scans the saved embeds in the database and removes any reaction role mappings
 * that do not follow the new normalized format (i.e., keys that do not start with "unicode:" or "custom:").
 * It can be used to clean up old data and ensure that only valid reaction role mappings are stored.
 *
 * @author Derrick Eberlein
 */
public class CleanupReactionRolesCommand extends Command {

    /**
     * Constructor for the CleanupReactionRolesCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public CleanupReactionRolesCommand(Redacted bot) {
        super(bot);
        this.name = "cleanupreactionroles";
        this.description = "Removes old legacy reaction role mappings from saved embeds.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_ROLES;

        this.args.add(new OptionData(
                OptionType.STRING,
                "messageid",
                "Optional: clean only one saved embed message ID"
        ).setRequired(false));
    }

    /**
     * Executes the CleanupReactionRolesCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embeds from the database and removes any legacy reaction role mappings.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String messageId = event.getOption("messageid") != null
                ? event.getOption("messageid").getAsString()
                : null;

        MongoCollection<SavedEmbed> collection =
                GuildData.getDatabase().getSavedEmbedsCollection(event.getGuild().getIdLong());

        if (messageId != null && !messageId.isBlank()) {
            cleanSingleMessage(event, collection, messageId);
        } else {
            cleanAllMessages(event, collection);
        }
    }

    /**
     * Cleans a single saved embed message by its ID.
     * This method retrieves the saved embed from the database, checks for legacy reaction role mappings,
     * and removes any that do not follow the new normalized format.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     * @param collection The MongoCollection of SavedEmbed objects for the guild.
     * @param messageId The ID of the saved embed message to clean.
     */
    private void cleanSingleMessage(SlashCommandInteractionEvent event,
                                    MongoCollection<SavedEmbed> collection,
                                    String messageId) {

        SavedEmbed embed = collection.find(Filters.eq("messageId", messageId)).first();

        if (embed == null) {
            event.reply("❌ No saved embed found with message ID `" + messageId + "`.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> emojiRoleMap = embed.getEmojiRoleMap();
        if (emojiRoleMap == null || emojiRoleMap.isEmpty()) {
            event.reply("ℹ️ That saved embed has no reaction roles to clean.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> cleanedMap = new HashMap<>();
        int removed = 0;

        for (Map.Entry<String, String> entry : emojiRoleMap.entrySet()) {
            String key = entry.getKey();

            if (isNormalizedEmojiKey(key)) {
                cleanedMap.put(key, entry.getValue());
            } else {
                removed++;
                System.out.println("Removing legacy reaction role mapping from message "
                        + messageId + ": " + key + " -> " + entry.getValue());
            }
        }

        if (removed == 0) {
            event.reply("✅ No old reaction role mappings were found for message `" + messageId + "`.").setEphemeral(true).queue();
            return;
        }

        embed.setEmojiRoleMap(cleanedMap);
        collection.replaceOne(Filters.eq("messageId", messageId), embed, new ReplaceOptions().upsert(false));

        event.reply("✅ Cleaned message `" + messageId + "` and removed **" + removed + "** old reaction role mapping(s).")
                .setEphemeral(true)
                .queue();
    }

    /**
     * Cleans all saved embed messages in the database.
     * This method iterates through all saved embeds, checks for legacy reaction role mappings,
     * and removes any that do not follow the new normalized format. It keeps track of how many embeds were scanned,
     * how many were updated, and how many old mappings were removed in total.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     * @param collection The MongoCollection of SavedEmbed objects for the guild.
     */
    private void cleanAllMessages(SlashCommandInteractionEvent event,
                                  MongoCollection<SavedEmbed> collection) {

        int embedsScanned = 0;
        int embedsUpdated = 0;
        int totalRemoved = 0;

        try (MongoCursor<SavedEmbed> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                SavedEmbed embed = cursor.next();
                embedsScanned++;

                Map<String, String> emojiRoleMap = embed.getEmojiRoleMap();
                if (emojiRoleMap == null || emojiRoleMap.isEmpty()) {
                    continue;
                }

                Map<String, String> cleanedMap = new HashMap<>();
                int removedFromThisEmbed = 0;

                for (Map.Entry<String, String> entry : emojiRoleMap.entrySet()) {
                    String key = entry.getKey();

                    if (isNormalizedEmojiKey(key)) {
                        cleanedMap.put(key, entry.getValue());
                    } else {
                        removedFromThisEmbed++;
                        System.out.println("Removing legacy reaction role mapping from message "
                                + embed.getMessageId() + ": " + key + " -> " + entry.getValue());
                    }
                }

                if (removedFromThisEmbed > 0) {
                    embed.setEmojiRoleMap(cleanedMap);
                    collection.replaceOne(
                            Filters.eq("messageId", embed.getMessageId()),
                            embed,
                            new ReplaceOptions().upsert(false)
                    );

                    embedsUpdated++;
                    totalRemoved += removedFromThisEmbed;
                }
            }
        }

        event.reply(
                "✅ Reaction role cleanup complete.\n"
                        + "Scanned embeds: **" + embedsScanned + "**\n"
                        + "Updated embeds: **" + embedsUpdated + "**\n"
                        + "Removed old mappings: **" + totalRemoved + "**"
        ).setEphemeral(true).queue();
    }

    /**
     * Checks if the given emoji key is in a normalized format.
     * A normalized emoji key starts with either "unicode:" for unicode emojis or "custom:" for custom emojis.
     *
     * @param key The emoji key to check.
     * @return True if the key is in a normalized format, false otherwise.
     */
    private boolean isNormalizedEmojiKey(String key) {
        return key != null
                && (key.startsWith("unicode:") || key.startsWith("custom:"));
    }
}