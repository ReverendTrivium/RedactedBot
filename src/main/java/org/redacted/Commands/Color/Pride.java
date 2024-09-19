package org.redacted.Commands.Color;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;

import java.util.Objects;

public class Pride extends Command {

    public Pride(Redacted bot) {
        super(bot);
        this.name = "pride";
        this.description = "Start Cycling Rainbow Pride Roles for Pride Month";
        this.category = Category.FUN;
        this.permission = Permission.ADMINISTRATOR;
    }

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
