package org.redacted.Commands.Color;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.Roles.getRolesByName;

import java.util.Objects;

public class rolecolors extends Command {

    public rolecolors(Redacted bot) {
        super(bot);
        this.name = "rolecolors";
        this.description = "Start Cycling Role Colors in a Rainbow";
        this.category = Category.FUN;
        this.permission = Permission.ADMINISTRATOR;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();
        RoleColorChanger colors = new RoleColorChanger();

        // get roleID
        Role role;
        getRolesByName roles = new getRolesByName();
        role = roles.getRoleByName(Objects.requireNonNull(event.getGuild()), "Rainbow Role");
        if (role == null) {
            event.getHook().sendMessage("[ Error ] Couldn't find the role with name 'Rainbow Role'.").queue();
            return;
        }
        String rainbowID = role.getId();

        // get Interval
        long interval = 6000L;

        // start Role Color Command
        colors.changeColors(event.getGuild(), interval, rainbowID);
        event.getHook().sendMessage("Starting to Cycle Random Colors for Role!").queue();
    }
}
