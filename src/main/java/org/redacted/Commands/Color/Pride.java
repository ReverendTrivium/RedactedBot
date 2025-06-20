package org.redacted.Commands.Color;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;

import java.util.Objects;

/**
 * Command that starts cycling rainbow pride roles for Pride Month.
 * This command allows administrators to initiate a color cycling effect for a specific role.
 *
 * @author Derrick Eberlein
 */
public class Pride extends Command {

    /**
     * Constructor for the Pride command.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public Pride(Redacted bot) {
        super(bot);
        this.name = "pride";
        this.description = "Start Cycling Rainbow Pride Roles for Pride Month";
        this.category = Category.FUN;
        this.permission = Permission.ADMINISTRATOR;
    }

    /**
     * Executes the command when invoked.
     * It retrieves the role by name, sets the interval for color cycling, and starts the color cycling process.
     *
     * @param event The SlashCommandInteractionEvent containing the command invocation details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {

        event.deferReply().setEphemeral(true).queue();
        ColorCyclerPride pride = new ColorCyclerPride();

        // get roleID
        Role role;
        getRolesByName roles = new getRolesByName();
        role = roles.getRoleByName(Objects.requireNonNull(event.getGuild()), "Pride Role");
        if (role == null) {
            event.getHook().sendMessage("[ Error ] Couldn't find the role with name 'Pride Role'.").queue();
            return;
        }
        String roleID = role.getId();

        // get Interval
        long interval = 6000L;

        // start Pride Role Command
        pride.changeColorsPride(event.getGuild(), interval, roleID);
        event.getHook().sendMessage("Starting to cycle Pride Colors for Role!").queue();
    }
}
