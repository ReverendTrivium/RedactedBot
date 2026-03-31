package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Command;
import org.redacted.Commands.Category;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to list all emoji-role mappings for a given saved embed message.
 * This command retrieves and displays the reaction roles associated with a specific saved embed message.
 * It is useful for managing reaction roles in the server.
 *
 * @author Derrick Eberlein
 */
public class ListReactionRolesCommand extends Command {

    /**
     * Constructor for the ListReactionRolesCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public ListReactionRolesCommand(Redacted bot) {
        super(bot);
        this.name = "listreactionroles";
        this.description = "Lists all emoji-role mappings for a given saved embed message.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_ROLES;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "The ID of the saved embed", true));
    }

    /**
     * Executes the ListReactionRolesCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embed message by its ID and lists all associated reaction roles.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("messageid").getAsString();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("❌ Guild not found.").setEphemeral(true).queue();
            return;
        }

        MongoCollection<SavedEmbed> collection =
                GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());

        SavedEmbed embed = collection.find(Filters.eq("messageId", messageId)).first();

        if (embed == null || embed.getEmojiRoleMap() == null || embed.getEmojiRoleMap().isEmpty()) {
            event.reply("❌ No reaction roles found for this message.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> emojiRoleMap = embed.getEmojiRoleMap();

        String mapped = emojiRoleMap.entrySet().stream()
                .map(e -> {
                    String roleName = e.getValue();
                    Role role = guild.getRolesByName(roleName, true).stream().findFirst().orElse(null);

                    String roleMention = role != null
                            ? role.getAsMention()
                            : "`" + roleName + " [missing role]`";

                    return formatEmojiKey(e.getKey(), guild) + " → " + roleMention;
                })
                .collect(Collectors.joining("\n"));

        event.reply("📝 Reaction Roles for message `" + messageId + "`:\n" + mapped)
                .setEphemeral(true)
                .queue();
    }

    /**
     * Formats the emoji key for display.
     * If the key is a unicode emoji, it returns the unicode character.
     * If the key is a custom emoji, it retrieves the formatted emoji from the guild.
     * Otherwise, it returns the key as is.
     *
     * @param emojiKey The raw emoji key from the database.
     * @param guild The guild to retrieve custom emojis from.
     * @return A formatted string representing the emoji for display.
     */
    private String formatEmojiKey(String emojiKey, Guild guild) {
        if (emojiKey.startsWith("unicode:")) {
            return emojiKey.substring("unicode:".length());
        }

        if (emojiKey.startsWith("custom:")) {
            String emojiId = emojiKey.substring("custom:".length());
            RichCustomEmoji customEmoji = guild.getEmojiById(emojiId);

            if (customEmoji != null) {
                return customEmoji.getFormatted();
            }

            return "`custom:" + emojiId + "`";
        }

        return emojiKey;
    }
}
