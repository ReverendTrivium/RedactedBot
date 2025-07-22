package org.redacted.Commands.Utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.redacted.Commands.BotCommands;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;
import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.embeds.EmbedUtils;

import java.util.*;

/**
 * Help command that displays a list of all commands and categories.
 * It can also show details for a specific command or commands in a specific category.
 * <p>
 * Usage:
 * - `/help` to display all categories and commands.
 * - `/help category` to display commands in a specific category.
 * - `/help command` to display details for a specific command.
 *
 * @author Derrick Eberlein
 */
public class Help extends Command {

    private static final int COMMANDS_PER_PAGE = 6;

    /**
     * Constructor for the Help command.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public Help(Redacted bot) {
        super(bot);
        this.name = "help";
        this.description = "Display a list of all commands and categories.";
        this.category = Category.UTILITY;
        OptionData data = new OptionData(OptionType.STRING, "category", "See commands under this category");
        for (Category c : Category.values()) {
            String name = c.name.toLowerCase();
            data.addChoice(name, name);
        }
        this.args.add(data);
        this.args.add(new OptionData(OptionType.STRING, "command", "See details for this command"));
    }

    /**
     * Executes the help command.
     * This method is called when the command is invoked.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Create a hashmap that groups commands by categories.
        HashMap<Category, List<Command>> categories = new LinkedHashMap<>();
        EmbedBuilder builder = new EmbedBuilder().setColor(EmbedColor.DEFAULT.color);
        for (Category category : Category.values()) {
            categories.put(category, new ArrayList<>());
        }
        for (Command cmd : BotCommands.commands) {
            if (cmd.category != null && categories.containsKey(cmd.category)) {
                categories.get(cmd.category).add(cmd);
            }
        }

        OptionMapping option = event.getOption("category");
        OptionMapping option2 = event.getOption("command");
        if (option != null && option2 != null) {
            event.replyEmbeds(EmbedUtils.createError("Please only give one optional argument and try again.")).queue();
        } else if (option != null) {
            // Display category commands menu
            Category category = Category.valueOf(option.getAsString().toUpperCase());
            List<MessageEmbed> embeds = buildCategoryMenu(category, categories.get(category), event.getMember());
            if (embeds.isEmpty()) {
                // No commands for this category
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(category.emoji + "  **%s Commands**".formatted(category.name))
                        .setDescription("Coming soon...")
                        .setColor(EmbedColor.DEFAULT.color);
                event.replyEmbeds(embed.build()).queue();
                return;
            }
            // Send paginated help menu
            ReplyCallbackAction action = event.replyEmbeds(embeds.get(0));
            if (embeds.size() > 1) {
                ButtonListener.sendPaginatedMenu(event.getUser().getId(), action, embeds);
                return;
            }
            action.queue();
        } else if (option2 != null) {
            // Display command details menu
            Command cmd = BotCommands.commandsMap.get(option2.getAsString());
            if (cmd != null) {
                if (cmd.permission == null || Objects.requireNonNull(event.getMember()).hasPermission(cmd.permission)) {
                    builder.setTitle("Command: " + cmd.name);
                    builder.setDescription(cmd.description);
                    StringBuilder usages = new StringBuilder();
                    if (cmd.subCommands.isEmpty()) {
                        usages.append("`").append(getUsage(cmd)).append("`");
                    } else {
                        for (SubcommandData sub : cmd.subCommands) {
                            usages.append("`").append(getUsage(sub, cmd.name)).append("`\n");
                        }
                    }
                    builder.addField("Usage:", usages.toString(), false);
                    builder.addField("Permission:", getPermissions(cmd), false);
                    event.replyEmbeds(builder.build()).queue();
                } else {
                    event.replyEmbeds(EmbedUtils.createError("No command called \"" + option2.getAsString() + "\" found.")).setEphemeral(true).queue();
                }
            } else {
                // Command specified doesn't exist.
                event.replyEmbeds(EmbedUtils.createError("No command called \"" + option2.getAsString() + "\" found.")).queue();
            }
        } else {
            // Display default menu
            builder.setTitle("Redacted Commands");
            categories.forEach((category, commands) -> {
                String categoryName = category.name().toLowerCase();
                String value = "`/" + categoryName + " help`";
                builder.addField(category.emoji + " " + category.name, value, true);
            });
            event.replyEmbeds(builder.build()).queue();
        }
    }

    /**
     * Builds a menu with all the commands in a specified category.
     *
     * @param category the category to build a menu for.
     * @param commands a list of the commands in this category.
     * @param member the member to check permissions for.
     * @return a list of MessageEmbed objects for pagination.
     */
    public List<MessageEmbed> buildCategoryMenu(Category category, List<Command> commands, Member member) {
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(category.emoji + "  **%s Commands**".formatted(category.name));
        embed.setColor(EmbedColor.DEFAULT.color);

        int counter = 0;
        for (Command cmd : commands) {
            if (cmd.subCommands.isEmpty()) {
                if (cmd.permission == null || member.hasPermission(cmd.permission)) {
                    embed.appendDescription("`" + getUsage(cmd) + "`\n" + cmd.description + "\n\n");
                    counter++;
                    if (counter % COMMANDS_PER_PAGE == 0) {
                        embeds.add(embed.build());
                        embed.setDescription("");
                        counter = 0;
                    }
                }
            } else {
                for (SubcommandData sub : cmd.subCommands) {
                    if (cmd.permission == null || member.hasPermission(cmd.permission)) {
                        embed.appendDescription("`" + getUsage(sub, cmd.name) + "`\n" + sub.getDescription() + "\n\n");
                        counter++;
                        if (counter % COMMANDS_PER_PAGE == 0) {
                            embeds.add(embed.build());
                            embed.setDescription("");
                            counter = 0;
                        }
                    }
                }
            }
        }
        if (counter != 0) embeds.add(embed.build());
        return embeds;
    }

    /**
     * Creates a string of command usage.
     *
     * @param cmd Command to build usage for.
     * @return String with name and args stitched together.
     */
    public String getUsage(Command cmd) {
        StringBuilder usage = new StringBuilder("/" + cmd.name);
        if (cmd.args.isEmpty()) return usage.toString();
        for (int i = 0; i < cmd.args.size(); i++) {
            boolean isRequired = cmd.args.get(i).isRequired();
            if (isRequired) {
                usage.append(" <");
            } else {
                usage.append(" [");
            }
            usage.append(cmd.args.get(i).getName());
            if (isRequired) {
                usage.append(">");
            } else {
                usage.append("]");
            }
        }
        return usage.toString();
    }

    /**
     * Creates a string of subcommand usage.
     *
     * @param cmd sub command data from a command.
     * @return String with name and args stitched together.
     */
    public String getUsage(SubcommandData cmd, String commandName) {
        StringBuilder usage = new StringBuilder("/" + commandName + " " + cmd.getName());
        if (cmd.getOptions().isEmpty()) return usage.toString();
        for (OptionData arg : cmd.getOptions()) {
            boolean isRequired = arg.isRequired();
            if (isRequired) {
                usage.append(" <");
            } else {
                usage.append(" [");
            }
            usage.append(arg.getName());
            if (isRequired) {
                usage.append(">");
            } else {
                usage.append("]");
            }
        }
        return usage.toString();
    }

    /**
     * Builds a string of permissions from command.
     *
     * @param cmd the command to draw perms from.
     * @return A string of command perms.
     */
    private String getPermissions(Command cmd) {
        if (cmd.permission == null) {
            return "None";
        }
        return cmd.permission.getName();
    }
}

