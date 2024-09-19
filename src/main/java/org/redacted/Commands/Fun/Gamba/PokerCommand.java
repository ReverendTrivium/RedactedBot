package org.redacted.Commands.Fun.Gamba;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;

import java.util.*;

public class PokerCommand extends Command {

    private static final Map<String, TexasHoldemGame> activeGames = new HashMap<>();

    public PokerCommand(Redacted bot) {
        super(bot);
        this.name = "poker";
        this.description = "Play Texas Hold'em against the bot!";
    }

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

    public static TexasHoldemGame getGame(User user) {
        return activeGames.get(user.getId());
    }

    public void endGame(User user) {
        activeGames.remove(user.getId());
    }
}
