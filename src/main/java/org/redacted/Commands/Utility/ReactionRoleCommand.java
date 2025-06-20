package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;

import java.util.Objects;
import java.util.regex.Pattern;

public class ReactionRoleCommand extends Command {

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

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = Objects.requireNonNull(event.getOption("messageid")).getAsString();
        String emoji = Objects.requireNonNull(event.getOption("emoji")).getAsString();
        String roleName = Objects.requireNonNull(event.getOption("role")).getAsString();

        Guild guild = event.getGuild();
        Role role = new getRolesByName().getRoleByName(Objects.requireNonNull(guild), roleName);

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

        collection.updateOne(
                Filters.eq("messageId", messageId),
                Updates.set("emojiRoleMap." + emoji, role.getName()) // ← role name instead of ID
        );

        TextChannel channel = guild.getTextChannelById(embed.getChannelId());
        if (channel == null) {
            event.reply("❌ Could not find the channel for this message.").setEphemeral(true).queue();
            return;
        }

        channel.retrieveMessageById(messageId).queue(message -> {
            // Determine if emoji is custom (e.g. <a:name:id> or <:name:id>)
            Emoji emojiObj;
            if (isCustomEmojiFormat(emoji)) {
                emojiObj = Emoji.fromFormatted(emoji);
            } else {
                emojiObj = Emoji.fromUnicode(emoji);
            }

            message.addReaction(emojiObj).queue(
                    success -> event.reply("✅ Reaction role added: " + emoji + " → " + role.getAsMention()).setEphemeral(true).queue(),
                    failure -> event.reply("❌ Failed to add reaction. Make sure the emoji is valid and the bot has access to it.").setEphemeral(true).queue()
            );
        });
    }

    private boolean isCustomEmojiFormat(String emoji) {
        return Pattern.matches("<a?:\\w{2,32}:\\d{17,20}>", emoji);
    }
}