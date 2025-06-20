package org.redacted.Commands.Utility;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Redacted;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ListReactionRolesCommand extends Command {

    public ListReactionRolesCommand(Redacted bot) {
        super(bot);
        this.name = "listreactionroles";
        this.description = "Lists all emoji-role mappings for a given saved embed message.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_ROLES;

        this.args.add(new OptionData(OptionType.STRING, "messageid", "The ID of the saved embed", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String messageId = Objects.requireNonNull(event.getOption("messageid")).getAsString();
        Guild guild = event.getGuild();

        MongoCollection<SavedEmbed> collection = GuildData.getDatabase().getSavedEmbedsCollection(Objects.requireNonNull(guild).getIdLong());
        SavedEmbed embed = collection.find(Filters.eq("messageId", messageId)).first();

        if (embed == null || embed.getEmojiRoleMap() == null || embed.getEmojiRoleMap().isEmpty()) {
            event.reply("❌ No reaction roles found for this message.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> emojiRoleMap = embed.getEmojiRoleMap();
        String mapped = emojiRoleMap.entrySet().stream()
                .map(e -> {
                    Role role = guild.getRoleById(e.getValue());
                    String roleMention = role != null ? role.getAsMention() : "`[deleted role]`";
                    return e.getKey() + " → " + roleMention;
                })
                .collect(Collectors.joining("\n"));

        event.reply("📝 Reaction Roles for message `" + messageId + "`:\n" + mapped).setEphemeral(true).queue();
    }
}
