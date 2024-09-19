package org.redacted.Commands.Utility.HelpSubCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.redacted.Commands.BotCommands;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;
import org.redacted.util.embeds.EmbedColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CategoryHelpCommand extends Command {

    private static final int COMMANDS_PER_PAGE = 6;
    private final Category category;

    public CategoryHelpCommand(Redacted bot, Category category) {
        super(bot);
        this.name = category.name().toLowerCase();
        this.description = "Display a list of all " + category.name().toLowerCase() + " commands.";
        this.category = category;
        if (category.equals(Category.STAFF)) {
            this.permission = Permission.BAN_MEMBERS;
        }

        // Add subcommands
        this.subCommands.add(new SubcommandData("help", "Display a list of all " + category.name().toLowerCase() + " commands"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (Objects.equals(event.getSubcommandName(), "help")) {
            displayCategoryCommands(event);
        }
    }

    private void displayCategoryCommands(SlashCommandInteractionEvent event) {
        // Create a hashmap that groups commands by categories.
        HashMap<Category, List<Command>> categories = new HashMap<>();
        for (Category category : Category.values()) {
            categories.put(category, new ArrayList<>());
        }
        for (Command cmd : BotCommands.commands) {
            if (cmd.category != null && categories.containsKey(cmd.category)) {
                categories.get(cmd.category).add(cmd);
            }
        }

        // Display category commands menu
        List<MessageEmbed> embeds = buildCategoryMenu(this.category, categories.get(this.category), event);
        if (embeds.isEmpty()) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(this.category.emoji + "  **%s Commands**".formatted(this.category.name))
                    .setDescription("Coming soon...")
                    .setColor(EmbedColor.DEFAULT.color);
            event.replyEmbeds(embed.build()).queue();
            return;
        }
        // Send paginated help menu
        ButtonListener.sendPaginatedMenu(event.getUser().getId(), event.replyEmbeds(embeds.get(0)), embeds);
    }

    /**
     * Builds a menu with all the commands in a specified category.
     *
     * @param category the category to build a menu for.
     * @param commands a list of the commands in this category.
     * @param event the event to check the member's permissions.
     * @return a list of MessageEmbed objects for pagination.
     */
    private List<MessageEmbed> buildCategoryMenu(Category category, List<Command> commands, SlashCommandInteractionEvent event) {
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(category.emoji + "  **%s Commands**".formatted(category.name));
        embed.setColor(EmbedColor.DEFAULT.color);

        int counter = 0;
        for (Command cmd : commands) {
            if (cmd.permission == null || Objects.requireNonNull(event.getMember()).hasPermission(cmd.permission)) {
                embed.appendDescription("`/" + cmd.name + " " + getArgs(cmd) + "`\n" + cmd.description + "\n\n");
                counter++;
                if (counter % COMMANDS_PER_PAGE == 0) {
                    embeds.add(embed.build());
                    embed.setDescription("");
                    counter = 0;
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
     * @return String with the command's name and arguments.
     */
    private String getArgs(Command cmd) {
        StringBuilder args = new StringBuilder();
        if (cmd.args.isEmpty()) return args.toString();
        for (int i = 0; i < cmd.args.size(); i++) {
            boolean isRequired = cmd.args.get(i).isRequired();
            if (isRequired) {
                args.append(" <").append(cmd.args.get(i).getName()).append(">");
            } else {
                args.append(" [").append(cmd.args.get(i).getName()).append("]");
            }
        }
        return args.toString();
    }
}
