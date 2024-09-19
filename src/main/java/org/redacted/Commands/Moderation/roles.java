package org.redacted.Commands.Moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command that returns all roles in a server.
 *
 * @author Derrick Eberlein
 */
public class roles extends Command {
    public roles (Redacted bot) {
        super(bot);
        this.name = "role";
        this.description = "Get all Server Roles";
        this.category = Category.STAFF;
        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // run the '/roles' command
        event.deferReply().setEphemeral(true).queue();

        StringBuilder response = new StringBuilder();
        for (Role role : Objects.requireNonNull(event.getGuild()).getRoles())
            response.append(role.getAsMention()).append("\n");
        event.getHook().sendMessage(response.toString()).queue();
    }
}
