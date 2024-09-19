package org.redacted.Commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.redacted.Redacted;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a general slash command with properties.
 *
 * @author Derrick Eberlein
 */
public abstract class Command {

    public Redacted bot;
    public String name;
    public String description;
    public Category category;
    public List<OptionData> args;
    public List<SubcommandData> subCommands;
    public Permission permission; //Permission user needs to execute this command
    public Permission botPermission; //Permission bot needs to execute this command

    public Command(Redacted bot) {
        this.bot = bot;
        this.args = new ArrayList<>();
        this.subCommands = new ArrayList<>();
    }

    public abstract void execute(SlashCommandInteractionEvent event);
}