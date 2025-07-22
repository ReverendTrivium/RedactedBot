package org.redacted.Commands;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.redacted.Commands.Blacklist.*;
import org.redacted.Commands.Color.Pride;
import org.redacted.Commands.Economy.*;
import org.redacted.Commands.Fun.*;
import org.redacted.Commands.Fun.Gamba.BlackJackCommand;
import org.redacted.Commands.Fun.Gamba.PokerCommand;
import org.redacted.Commands.Greetings.FarewellCommand;
import org.redacted.Commands.Greetings.GreetCommand;
import org.redacted.Commands.Greetings.GreetingsCommand;
import org.redacted.Commands.Greetings.JoinDMCommand;
import org.redacted.Commands.Music.*;
import org.redacted.Commands.Suggestions.RespondCommand;
import org.redacted.Commands.Suggestions.SuggestCommand;
import org.redacted.Commands.Suggestions.SuggestionsCommand;
import org.redacted.Commands.Utility.*;
import org.redacted.Commands.Color.rolecolors;
import org.redacted.Commands.Moderation.roles;
import org.redacted.Commands.Utility.HelpSubCommands.*;
import org.redacted.Redacted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redacted.util.GoogleSearch.GoogleSearchService;
import org.redacted.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import java.util.*;

/**
 * Command Manager of Bot, all commands will be added and controlled
 * through this class.
 *
 * @author Derrick Eberlein
 */
@Getter
@Setter
public class BotCommands extends ListenerAdapter {
    /** List of commands in the exact order registered */
    public static final List<Command> commands = new ArrayList<>();
    /** Map of command names to command objects */
    public static final Map<String, Command> commandsMap = new HashMap<>();
    private static boolean commandsRegistered = false;  // Static flag to ensure commands are registered only once

    /**
     * Adds commands to a global list and registers them as event listener.
     *
     * @param bot An instance of Redacted.
     */
    public BotCommands(Redacted bot) {

        if (!commandsRegistered) {
            GoogleSearchService googleSearchService = new GoogleSearchService();
            mapCommand(
                    // Role ColorChanger commands
                    new Pride(bot),
                    new rolecolors(bot),

                    // Fun commands
                    new NSFWCommand(bot),
                    new LoopNSFWCommand(bot),
                    new StopLoopCommand(bot),
                    new JokeCommand(bot),
                    new welcome(bot),
                    new EightBallCommand(bot),
                    new InspireCommand(bot),
                    new GoogleCommand(bot, googleSearchService),
                    new AnimeCommand(bot),
                    new SummarizeCommand(bot),

                    // Music commands
                    new NowPlayingCommand(bot),
                    new PauseCommand(bot),
                    new PlayCommand(bot),
                    new QueueCommand(bot),
                    new RepeatCommand(bot),
                    new ResumeCommand(bot),
                    new SeekCommand(bot),
                    new SkipCommand(bot),
                    new StopCommand(bot),
                    new VolumeCommand(bot),

                    // Gamba commands
                    new BlackJackCommand(bot),
                    new PokerCommand(bot),

                    // Suggestions commands
                    new RespondCommand(bot),
                    new SuggestCommand(bot),
                    new SuggestionsCommand(bot),

                    // Greetings commands
                    new FarewellCommand(bot),
                    new GreetCommand(bot),
                    new GreetingsCommand(bot),
                    new JoinDMCommand(bot),

                    // Staff commands
                    new roles(bot),
                    new BlacklistCommand(bot),

                    // Economy commands
                    new EconomyCommand(bot),
                    new BalanceCommand(bot),
                    new CrimeCommand(bot),
                    new DepositCommand(bot),
                    new PayCommand(bot),
                    new RobCommand(bot),
                    new WithdrawCommand(bot),
                    new WorkCommand(bot),
                    new LeaderboardCommand(bot),

                    // Utility commands
                    new Server(bot),
                    new EmbedCommand(bot),
                    new EditEmbedCommand(bot),
                    new DeleteEmbedCommand(bot),
                    new ReactionRoleCommand(bot),
                    new ListReactionRolesCommand(bot),
                    new ListEmbeddedMessagesCommand(bot),
                    new Ping(bot),
                    new Clear(bot),
                    new NSFWCleanCommand(bot),
                    new RolePermissionsCommand(bot),
                    new MessageSchedulerCommand(bot),
                    new ListScheduledMessagesCommand(bot),
                    new DeleteScheduledMessageCommand(bot),
                    new Help(bot)
            );

            // Register CategoryHelpCommand for each category
            for (Category category : Category.values()) {
                mapCommand(new CategoryHelpCommand(bot, category));
            }

            commandsRegistered = true;  // Mark commands as registered
        }
    }

    /**
     * Adds a command to the static list and map.
     *
     * @param cmds a spread list of command objects.
     */
    private void mapCommand(Command... cmds) {
        for (Command cmd : cmds) {
            commandsMap.put(cmd.name, cmd);
            commands.add(cmd);

            // Debug output to confirm command registration
            //System.out.println("Command registered: " + cmd.name);
        }
    }

    /**
     * Registers all commands for a specific guild.
     * This method should be called when the bot is ready or when a guild is joined.
     *
     * @param guild The guild to register commands for.
     */
    public void registerCommandsForGuild(Guild guild) {
        // Ensure commands are registered only once
        System.out.println("Registering commands for guild: " + guild.getName());

        // Register slash commands for the specific guild
        guild.updateCommands().addCommands(unpackCommandData()).queue(
                succ -> System.out.println("Commands registered successfully for guild: " + guild.getName()),
                fail -> System.out.println("Failed to register commands for guild: " + guild.getName())
        );
    }

    /**
     * Unpacks the command data into a list of CommandData for registration.
     *
     * @return List of CommandData to be registered with JDA.
     */
    public static List<CommandData> unpackCommandData() {
        List<CommandData> commandData = new ArrayList<>();
        for (Command command : commands) {
            SlashCommandData slashCommand = Commands.slash(command.name, command.description).addOptions(command.args);
            if (command.permission != null) {
                slashCommand.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL, Permission.MODERATE_MEMBERS));
            }
            if (!command.subCommands.isEmpty()) {
                slashCommand.addSubcommands(command.subCommands);
            }
            commandData.add(slashCommand);

            /*
            //Debug output to confirm command registration
            System.out.println("Registering command: " + command.name);

            for (SubcommandData subcommand : command.subCommands) {
                // Debug output to confirm subcommands registration
                System.out.println("  Subcommand: " + subcommand.getName() + " - " + subcommand.getDescription());
            }
             */
        }
        return commandData;
    }

    /**
     * Handles slash command interactions.
     * This method is called when a slash command is invoked.
     *
     * @param event The SlashCommandInteractionEvent containing the command details.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println("Received slash command interaction: " + event.getName());

        // Get command by name
        Command cmd = commandsMap.get(event.getName());
        if (cmd != null) {
            // Check for required bot permissions
            Role botRole = Objects.requireNonNull(event.getGuild()).getBotRole();
            if (cmd.botPermission != null) {
                assert botRole != null;
                if (!botRole.hasPermission(cmd.botPermission) && !botRole.hasPermission(Permission.ADMINISTRATOR)) {
                    String text = "I need the `" + cmd.botPermission.getName() + "` permission to execute that command.";
                    event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
                    return;
                }
            }
            // Run command
            cmd.execute(event);
        } else {
            System.out.println("Command not found: " + event.getName());
        }
    }

    /**
     * Fetches a command by its name.
     *
     * @param name The name of the command to fetch.
     * @return The Command object if found, null otherwise.
     */
    public Command getCommandByName(String name) {
        return commandsMap.get(name);  // Fetches command by its name
    }
}