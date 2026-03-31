package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to delete a specific reaction role mapping from a saved embed message.
 * This command allows users to remove a reaction role association from a saved embed message
 * by specifying the message ID and the emoji linked to the role they wish to delete.
 *
 * @author Derrick Eberlein
 */
public class DeleteReactionRoleCommand extends Command {

    /**
     * Constructor for the DeleteReactionRoleCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public DeleteReactionRoleCommand(Redacted bot) {
        super(bot);
        this.name = "deletereactionrole";
        this.description = "Deletes a specific reaction role mapping from a saved embed.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_ROLES;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "The message ID of the saved embed", true));
        this.args.add(new OptionData(OptionType.STRING, "emoji", "The emoji shown in /listreactionroles", true));
    }

    /**
     * Executes the DeleteReactionRoleCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embed message by its ID, identifies the reaction role mapping for the specified emoji,
     * and removes that mapping from the database.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String messageId = event.getOption("messageid").getAsString().trim();
        String emojiInput = event.getOption("emoji").getAsString().trim();

        MongoCollection<SavedEmbed> collection =
                GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());

        SavedEmbed embed = collection.find(Filters.eq("messageId", messageId)).first();

        if (embed == null) {
            event.reply("❌ No saved embed found with message ID `" + messageId + "`.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> emojiRoleMap = embed.getEmojiRoleMap();
        if (emojiRoleMap == null || emojiRoleMap.isEmpty()) {
            event.reply("❌ No reaction roles found for this message.").setEphemeral(true).queue();
            return;
        }

        String keyToDelete = resolveEmojiKeyFromInput(guild, emojiInput, emojiRoleMap);

        if (keyToDelete == null) {
            event.reply(
                    "❌ Could not match that emoji to a saved reaction role entry.\n" +
                            "Try copying the exact emoji shown in `/listreactionroles`."
            ).setEphemeral(true).queue();
            return;
        }

        String removedRole = emojiRoleMap.get(keyToDelete);

        collection.updateOne(
                Filters.eq("messageId", messageId),
                Updates.unset("emojiRoleMap." + keyToDelete)
        );

        event.reply(
                "✅ Deleted reaction role mapping from message `" + messageId + "`:\n" +
                        formatEmojiKeyForDisplay(guild, keyToDelete) + " → `" + removedRole + "`"
        ).setEphemeral(true).queue();
    }

    /**
     * Resolves the stored emoji key from the user input.
     * This method attempts to match the user-provided emoji input to the keys stored in the database,
     * which may be in various formats (unicode, custom ID, etc.).
     *
     * @param guild The guild to retrieve custom emojis from.
     * @param input The raw emoji input provided by the user.
     * @param emojiRoleMap The map of stored emoji keys to role names for the saved embed message.
     * @return The resolved emoji key that matches the user input, or null if no match is found.
     */
    private String resolveEmojiKeyFromInput(Guild guild, String input, Map<String, String> emojiRoleMap) {
        if (input == null || input.isBlank()) {
            return null;
        }

        input = input.trim();

        // 1. Exact stored key already provided
        if (emojiRoleMap.containsKey(input)) {
            return input;
        }

        // 2. Unicode normalized key
        String unicodeKey = "unicode:" + input;
        if (emojiRoleMap.containsKey(unicodeKey)) {
            return unicodeKey;
        }

        // 3. Custom mention format <:name:id> or <a:name:id>
        Matcher formattedMatcher = Pattern.compile("^<a?:\\w{2,32}:(\\d{17,20})>$").matcher(input);
        if (formattedMatcher.matches()) {
            String customKey = "custom:" + formattedMatcher.group(1);
            if (emojiRoleMap.containsKey(customKey)) {
                return customKey;
            }
        }

        // 4. Colon-name format from displayed list, e.g. :zenlesszonezero: or :zenlesszonezero~1:
        Matcher colonMatcher = Pattern.compile("^:([^:]{2,32}):$").matcher(input);
        if (colonMatcher.matches()) {
            String emojiName = colonMatcher.group(1);

            List<RichCustomEmoji> matches = guild.getEmojisByName(emojiName, true);
            for (RichCustomEmoji emoji : matches) {
                String customKey = "custom:" + emoji.getId();
                if (emojiRoleMap.containsKey(customKey)) {
                    return customKey;
                }
            }

            // Fallback: compare against how the key is displayed
            for (String key : emojiRoleMap.keySet()) {
                String display = formatEmojiKeyForDisplay(guild, key);
                if (display.equalsIgnoreCase(input)) {
                    return key;
                }
            }
        }

        // 5. Last fallback: compare exact display output for every saved key
        for (String key : emojiRoleMap.keySet()) {
            String display = formatEmojiKeyForDisplay(guild, key);
            if (display.equals(input)) {
                return key;
            }
        }

        return null;
    }

    /**
     * Formats the emoji key for display.
     * If the key is a unicode emoji, it returns the unicode character.
     * If the key is a custom emoji, it retrieves the formatted emoji from the guild.
     * Otherwise, it returns the key as is.
     *
     * @param guild The guild to retrieve custom emojis from.
     * @param emojiKey The raw emoji key from the database.
     * @return A formatted string representing the emoji for display.
     */
    private String formatEmojiKeyForDisplay(Guild guild, String emojiKey) {
        if (emojiKey == null) {
            return "`[unknown]`";
        }

        if (emojiKey.startsWith("unicode:")) {
            return emojiKey.substring("unicode:".length());
        }

        if (emojiKey.startsWith("custom:")) {
            String emojiId = emojiKey.substring("custom:".length());
            RichCustomEmoji customEmoji = guild.getEmojiById(emojiId);

            if (customEmoji != null) {
                return ":" + customEmoji.getName() + ":";
            }

            return "`custom:" + emojiId + "`";
        }

        return emojiKey;
    }
}
