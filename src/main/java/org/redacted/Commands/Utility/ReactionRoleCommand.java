package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Command;
import org.redacted.Commands.Category;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;
import java.util.regex.Pattern;

/**
 * Command to add a reaction role to a saved embed message.
 * This command allows users to link a reaction emoji to a specific role on an embed message
 * that has been previously saved in the database.
 *
 * @author Derrick Eberlein
 */
public class ReactionRoleCommand extends Command {

    /**
     * Constructor for the ReactionRoleCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public ReactionRoleCommand(Redacted bot) {
        super(bot);
        this.name = "reactionrole";
        this.description = "Links a reaction to a role on a saved embed message";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_ROLES;
        this.botPermission = Permission.MANAGE_ROLES;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "ID of the embed message", true));
        this.args.add(new OptionData(OptionType.STRING, "emoji", "Emoji to react with (unicode only)", true));
        this.args.add(new OptionData(OptionType.STRING, "role", "Role name to assign", true));
    }

    /**
     * Executes the ReactionRoleCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the saved embed message by its ID and adds a reaction role mapping.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("messageid").getAsString();
        String emojiInput = event.getOption("emoji").getAsString().trim();
        String roleName = event.getOption("role").getAsString();

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ Guild not found.").setEphemeral(true).queue();
            return;
        }

        Role role = new getRolesByName().getRoleByName(guild, roleName);
        if (role == null) {
            event.reply("❌ Could not find a role named `" + roleName + "`.").setEphemeral(true).queue();
            return;
        }

        MongoCollection<SavedEmbed> collection = GuildData
                .getDatabase()
                .getSavedEmbedsCollection(guild.getIdLong());

        SavedEmbed embed = collection.find(Filters.eq("messageId", messageId)).first();
        if (embed == null) {
            event.reply("❌ No saved embed found with that message ID.").setEphemeral(true).queue();
            return;
        }

        String emojiKey = toEmojiKey(emojiInput);
        System.out.println("Saving reaction role mapping: " + emojiKey + " -> " + role.getName());

        collection.updateOne(
                Filters.eq("messageId", messageId),
                Updates.set("emojiRoleMap." + emojiKey, role.getName())
        );

        TextChannel channel = guild.getTextChannelById(embed.getChannelId());
        if (channel == null) {
            event.reply("❌ Could not find the channel for this message.").setEphemeral(true).queue();
            return;
        }

        channel.retrieveMessageById(messageId).queue(message -> {
            Emoji emojiObj;
            try {
                if (isCustomEmojiFormat(emojiInput)) {
                    emojiObj = Emoji.fromFormatted(emojiInput);
                } else {
                    emojiObj = Emoji.fromUnicode(emojiInput);
                }
            } catch (Exception e) {
                event.reply("❌ Invalid emoji format.").setEphemeral(true).queue();
                return;
            }

            message.addReaction(emojiObj).queue(
                    success -> event.reply("✅ Reaction role added: " + emojiInput + " → " + role.getAsMention())
                            .setEphemeral(true).queue(),
                    failure -> {
                        failure.printStackTrace();
                        event.reply("❌ Failed to add reaction. Make sure the emoji is valid and the bot has access to it.")
                                .setEphemeral(true).queue();
                    }
            );
        }, failure -> {
            failure.printStackTrace();
            event.reply("❌ Failed to retrieve the target message.").setEphemeral(true).queue();
        });
    }

    /**
     * Checks if the provided emoji string is in the custom emoji format.
     *
     * @param emoji The emoji string to check.
     * @return true if the emoji is in custom format, false otherwise.
     */
    private boolean isCustomEmojiFormat(String emoji) {
        return Pattern.matches("<a?:\\w{2,32}:\\d{17,20}>", emoji);
    }

    /**
     * Converts the given emoji input into a standardized key format for database storage.
     * Custom emojis are prefixed with "custom:" followed by their ID, while unicode emojis
     * are prefixed with "unicode:" followed by the emoji itself.
     *
     * @param emojiInput The raw emoji input from the user.
     * @return A standardized string key representing the emoji for database mapping.
     */
    private String toEmojiKey(String emojiInput) {
        emojiInput = emojiInput.trim();

        if (isCustomEmojiFormat(emojiInput)) {
            String id = emojiInput.replaceAll("<a?:\\w{2,32}:(\\d{17,20})>", "$1");
            return "custom:" + id;
        }

        return "unicode:" + emojiInput;
    }
}