package org.redacted.Commands.Blacklist;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

public class BlacklistCommand extends Command {

    public BlacklistCommand(Redacted bot) {
        super(bot);
        this.name = "blacklist";
        this.description = "Manage the blacklist";
        this.category = Category.STAFF;

        // Add the subcommands with their respective options
        this.subCommands.add(new SubcommandData("add", "Add a user to the blacklist")
                .addOptions(
                        new OptionData(OptionType.STRING, "firstname", "The first name of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "lastname", "The last name of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "social-media-handle", "The social media handle of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "platform", "The social media platform (instagram or facebook)").setRequired(true)
                                .addChoice("Instagram", "instagram")
                                .addChoice("Facebook", "facebook")
                ));
        this.subCommands.add(new SubcommandData("delete", "Delete a user from the blacklist")
                .addOptions(
                        new OptionData(OptionType.STRING, "firstname", "The first name of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "lastname", "The last name of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "social-media-handle", "The social media handle of the user").setRequired(true),
                        new OptionData(OptionType.STRING, "platform", "The social media platform (instagram or facebook)").setRequired(true)
                                .addChoice("Instagram", "instagram")
                                .addChoice("Facebook", "facebook")
                ));
        this.subCommands.add(new SubcommandData("clear", "Clear the blacklist"));
        this.subCommands.add(new SubcommandData("get", "Get all users in the blacklist"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommandName = event.getSubcommandName();

        if (subcommandName != null) {
            switch (subcommandName) {
                case "add" -> new BlacklistAddCommand(bot).execute(event);
                case "delete" -> new BlacklistDeleteCommand(bot).execute(event);
                case "clear" -> new BlacklistClearCommand(bot).execute(event);
                case "get" -> new BlacklistGetCommand(bot).execute(event);
                default -> event.reply("Unknown subcommand. Please use one of the available subcommands.").setEphemeral(true).queue();
            }
        } else {
            event.reply("Unknown subcommand. Please use one of the available subcommands.").setEphemeral(true).queue();
        }
    }
}
