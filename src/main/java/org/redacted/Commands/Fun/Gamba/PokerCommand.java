package org.redacted.Commands.Fun.Gamba;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;

import java.util.*;

import java.util.List;

/**
 * Command that allows users to play Texas Hold'em poker against the bot.
 * Manages active games and handles user interactions with buttons for betting and folding.
 *
 * @author Derrick Eberlein
 */
public class PokerCommand extends Command {

    private static final Map<String, TexasHoldemGame> activeGames = new HashMap<>();

    /**
     * Constructor for the PokerCommand.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public PokerCommand(Redacted bot) {
        super(bot);
        this.name = "poker";
        this.description = "Play Texas Hold'em against the bot!";
    }

    /**
     * Executes the poker command.
     * Checks if the user is already in a game, creates a new game if not, deals the flop,
     * and sends the game status with buttons for betting and folding.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getUser();

        if (activeGames.containsKey(user.getId())) {
            event.reply("You're already in a game!").setEphemeral(true).queue();
            return;
        }

        List<User> players = new ArrayList<>();
        players.add(user);

        TexasHoldemGame game = new TexasHoldemGame(players);
        activeGames.put(user.getId(), game);

        // Deal the flop (first three community cards)
        game.dealFlop();

        EmbedBuilder embed = game.getGameStatus(user, false);
        String uuid = user.getId() + ":" + UUID.randomUUID();

        // Buttons now include UUID for tracking
        List<Button> buttons = List.of(
                Button.primary("poker:bet:" + uuid, "Bet"),
                Button.danger("poker:fold:" + uuid, "Fold")
        );

        ButtonListener.buttons.put(uuid, buttons);

        event.replyEmbeds(embed.build())
                .addActionRow(buttons)
                .queue(interactionHook -> ButtonListener.disableButtons(uuid, interactionHook));
    }

    /**
     * Retrieves the active game for a user.
     *
     * @param user The user for whom to retrieve the game.
     * @return The TexasHoldemGame instance if found, null otherwise.
     */
    public static TexasHoldemGame getGame(User user) {
        return activeGames.get(user.getId());
    }

    /**
     * Ends the game for a user and removes it from the active games map.
     *
     * @param user The user whose game is to be ended.
     */
    public void endGame(User user) {
        activeGames.remove(user.getId());
    }
}
