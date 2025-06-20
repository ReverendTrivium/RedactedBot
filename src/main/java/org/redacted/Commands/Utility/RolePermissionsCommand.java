package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command to print all permissions of a specified role in the server.
 * This command retrieves the permissions of a role by its name and displays them in an embed.
 * It is useful for server administrators to check role permissions.
 *
 * @author Derrick Eberlein
 */
public class RolePermissionsCommand extends Command {

    /**
     * Constructor for the RolePermissionsCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public RolePermissionsCommand(Redacted bot) {
        super(bot);
        this.name = "rolepermissions";
        this.description = "Prints all permissions of the requested role.";
        this.category = Category.UTILITY;
        this.permission = Permission.MANAGE_CHANNEL;
        this.args.add(new OptionData(OptionType.STRING, "rolename", "The name of the role to check permissions for", true));
    }

    /**
     * Executes the RolePermissionsCommand.
     * This method handles the interaction when the command is invoked.
     * It retrieves the role by its name and lists all permissions associated with it.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping roleOption = event.getOption("rolename");
        if (roleOption == null) {
            event.reply("Role name option is missing.").setEphemeral(true).queue();
            return;
        }

        String roleName = roleOption.getAsString();
        List<Role> roles = Objects.requireNonNull(event.getGuild()).getRolesByName(roleName, true);

        if (roles.isEmpty()) {
            event.reply("Role '" + roleName + "' was not found.").setEphemeral(true).queue();
            return;
        }

        Role role = roles.get(0); // Assume we only care about the first matching role

        List<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        String permissionsList = String.join("\n", permissions);

        event.replyEmbeds(
                EmbedUtils.createEmbed(
                        "Permissions for Role: " + role.getName(),
                        permissionsList.isEmpty() ? "No permissions found." : permissionsList
                )
        ).queue();
    }
}


