package org.redacted.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.redacted.Commands.Fun.Gamba.BlackJackCommand;
import org.redacted.Commands.Fun.Gamba.PokerCommand;
import org.redacted.Commands.Fun.Gamba.TexasHoldemGame;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for button input and handles all button backend.
 *
 * @author Derrick Eberlein
 */
public class ButtonListener extends ListenerAdapter {

    public static final int MINUTES_TO_DISABLE = 3;
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(20);
    public static final Map<String, List<MessageEmbed>> menus = new HashMap<>();
    public static final Map<String, List<Button>> buttons = new HashMap<>();

    private final Redacted bot;

    public ButtonListener(Redacted bot) {
        this.bot = bot;
    }

    /**
     * Adds pagination buttons to a message action.
     *
     * @param userID the ID of the user who is accessing this menu.
     * @param action the ReplyCallbackAction to add components to.
     * @param embeds the embed pages.
     */
    public static void sendPaginatedMenu(String userID, ReplyCallbackAction action, List<MessageEmbed> embeds) {
        String uuid = userID + ":" + UUID.randomUUID();

        // If there's only one page, disable both buttons
        List<Button> components = getPaginationButtons(uuid, 0, embeds.size());
        buttons.put(uuid, components);
        menus.put(uuid, embeds);

        action.addActionRow(components).queue(interactionHook -> {
            if (embeds.size() > 1) {
                // Schedule button disabling only if there is more than one page
                ButtonListener.disableButtons(uuid, interactionHook);
            }
        });
    }


    /**
     * Get a list of buttons for paginated embeds.
     *
     * @param uuid the unique ID generated for these buttons.
     * @param maxPages the total number of embed pages.
     * @return A list of components to use on a paginated embed.
     */
    private static List<Button> getPaginationButtons(String uuid, int currentPage, int maxPages) {
        // Disable "Previous" button if on the first page, otherwise enable it
        Button previousButton = (currentPage == 0) ?
                Button.primary("pagination:prev:" + uuid, "Previous").asDisabled() :
                Button.primary("pagination:prev:" + uuid, "Previous").asEnabled();

        // Disable "Next" button if on the last page, otherwise enable it
        Button nextButton = (currentPage == maxPages - 1) ?
                Button.primary("pagination:next:" + uuid, "Next").asDisabled() :
                Button.primary("pagination:next:" + uuid, "Next").asEnabled();

        // The page label button is always disabled
        Button pageLabelButton = Button.of(ButtonStyle.SECONDARY, "pagination:page:" + currentPage, (currentPage + 1) + "/" + maxPages).asDisabled();

        return Arrays.asList(previousButton, pageLabelButton, nextButton);
    }


    /**
     * Schedules a timer task to disable buttons and clear cache after a set time.
     *
     * @param uuid the uuid of the components to disable.
     * @param hook a interaction hook pointing to original message.
     */
    public static void disableButtons(String uuid, InteractionHook hook) {
        Runnable task = () -> {
            List<Button> actionRow = ButtonListener.buttons.get(uuid);
            List<Button> newActionRow = new ArrayList<>();
            for (Button button : actionRow) {
                newActionRow.add(button.asDisabled());
            }
            hook.editOriginalComponents(ActionRow.of(newActionRow)).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            ButtonListener.buttons.remove(uuid);
            ButtonListener.menus.remove(uuid);
        };
        ButtonListener.executor.schedule(task, MINUTES_TO_DISABLE, TimeUnit.MINUTES);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getChannel().asTextChannel().isNSFW()) {
            return; // Ignore interactions in NSFW channels
        }

        // Split the componentId to identify the button action
        String[] pressedArgs = event.getComponentId().split(":");
        int length = pressedArgs.length;
        for (int i = 0; i < length; i++) {
            System.out.println("PressedArgs " + (i+1) + ": " + pressedArgs[i]);
        }

        String action = pressedArgs[0];

        // Ensure the user interacting is the one involved
        long userID = Long.parseLong(pressedArgs[2]);
        if (userID != event.getUser().getIdLong()) {
            return;
        }

        String uuid = userID + ":" + pressedArgs[3];

        // Handle different button actions
        switch (action) {
            case "pagination":
                handlePagination(event, pressedArgs, uuid);
                break;
            case "reset":
                handleReset(event, pressedArgs);
                break;
            case "blackjack":
                handleBlackjack(event, pressedArgs, uuid);
                break;
            case "poker":
                handlePoker(event, pressedArgs, uuid);
                break;
        }
    }

    /**
     * Handles the Pagination of Embedded Messages.
     *
     * @param uuid the uuid of the components to disable.
     * @param event a interaction event pointing to the original event.
     */
    private void handlePagination(ButtonInteractionEvent event, String[] pressedArgs, String uuid) {
        List<Button> components = buttons.get(uuid);
        if (components == null) return;

        List<MessageEmbed> embeds = menus.get(uuid);
        if (embeds == null) return;

        int currentPage = Integer.parseInt(Objects.requireNonNull(components.get(1).getId()).split(":")[2]);

        if (pressedArgs[1].equals("next")) {
            int nextPage = currentPage + 1;
            if (nextPage < embeds.size()) {
                // Generate new buttons with updated page number
                components = getPaginationButtons(uuid, nextPage, embeds.size());
                buttons.put(uuid, components);
                event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(nextPage)).queue();
            }
        } else if (pressedArgs[1].equals("prev")) {
            int prevPage = currentPage - 1;
            if (prevPage >= 0) {
                // Generate new buttons with updated page number
                components = getPaginationButtons(uuid, prevPage, embeds.size());
                buttons.put(uuid, components);
                event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(prevPage)).queue();
            }
        }
    }

    // Handle system reset buttons
    private void handleReset(ButtonInteractionEvent event, String[] pressedArgs) {
        String systemName = pressedArgs[4];
        if (pressedArgs[1].equals("yes")) {
            event.deferEdit().queue();
            GuildData data = GuildData.get(Objects.requireNonNull(event.getGuild()), bot);
            if (systemName.equalsIgnoreCase("Suggestion")) data.getSuggestionHandler().reset();
            else if (systemName.equalsIgnoreCase("Greeting")) data.getGreetingHandler().reset();
            MessageEmbed embed = EmbedUtils.createSuccess(systemName + " system was successfully reset!");
            event.getHook().editOriginalComponents(new ArrayList<>()).setEmbeds(embed).queue();
        } else if (pressedArgs[1].equals("no")) {
            event.deferEdit().queue();
            MessageEmbed embed = EmbedUtils.createError(systemName + " system was **NOT** reset!");
            event.getHook().editOriginalComponents(new ArrayList<>()).setEmbeds(embed).queue();
        }
    }

    // Handle blackjack buttons
    private void handleBlackjack(ButtonInteractionEvent event, String[] pressedArgs, String uuid) {
        long bet = Long.parseLong(pressedArgs[4]);
        MessageEmbed embed = null;
        if (pressedArgs[1].equals("hit")) {
            embed = BlackJackCommand.hit(event.getGuild(), event.getUser(), bet, uuid, bot);
        } else if (pressedArgs[1].equals("stand")) {
            embed = BlackJackCommand.stand(event.getGuild(), event.getUser(), bet, uuid, bot);
        }
        event.editComponents(ActionRow.of(buttons.get(uuid))).setEmbeds(embed).queue();
    }

    // Handle poker buttons
    public void handlePoker(ButtonInteractionEvent event, String[] pressedArgs, String uuid) {
        User user = event.getUser();
        PokerCommand pokerCommand = (PokerCommand) bot.getBotCommands().getCommandByName("poker");
        TexasHoldemGame game = PokerCommand.getGame(user);

        if (game == null) {
            event.reply("No active poker game found!").setEphemeral(true).queue();
            return;
        }

        switch (pressedArgs[1]) {
            case "bet" -> {
                // Trigger modal to ask for the bet amount
                event.replyModal(Modal.create("bet_modal", "Place Your Bet")
                        .addActionRow(TextInput.create("bet_input", "Bet Amount", TextInputStyle.SHORT)
                                .setPlaceholder("Enter your bet amount")
                                .setRequired(true)
                                .build())
                        .build()).queue();
            }
            case "fold" -> {
                event.reply(user.getName() + " folded.").queue();
                pokerCommand.endGame(user);  // End the game after fold
            }
            case "call" -> {
                // Handle the user's call to match the bot's raise
                // Since the player called, we proceed to the next phase
                EmbedBuilder embed = game.getGameStatus(user, false);
                event.getMessage().editMessageEmbeds(embed.build()).queue();
                dealNextPhase(event, game, embed);
            }
            case "raise" -> {
                // Trigger modal to ask for the bet amount when the bot raises
                event.replyModal(Modal.create("raise_modal", "Raise Amount")
                        .addActionRow(TextInput.create("raise_input", "Raise Amount", TextInputStyle.SHORT)
                                .setPlaceholder("Enter your raise amount")
                                .setRequired(true)
                                .build())
                        .build()).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("bet_modal") || event.getModalId().equals("raise_modal")) {
            String betAmountStr = Objects.requireNonNull(event.getValue("bet_input")).getAsString();
            try {
                long userBetAmount = Long.parseLong(betAmountStr);
                PokerCommand pokerCommand = (PokerCommand) bot.getBotCommands().getCommandByName("poker");
                TexasHoldemGame game = PokerCommand.getGame(event.getUser());

                if (game == null) {
                    event.reply("No active poker game found!").setEphemeral(true).queue();
                    return;
                }

                GuildData guildData = GuildData.get(Objects.requireNonNull(event.getGuild()), bot);
                EconomyHandler economyHandler = guildData.getEconomyHandler();

                long userBalance = economyHandler.getBalance(event.getUser().getIdLong());
                if (userBalance < userBetAmount) {
                    event.reply("You don't have enough money to place that bet.").setEphemeral(true).queue();
                    return;
                }

                // Deduct the user's bet from their balance
                economyHandler.removeMoney(event.getUser().getIdLong(), userBetAmount);

                // Bot logic for calling or raising
                long botBalance = 1000; // Example bot balance
                long botBetAmount = 0; // Bot's bet
                boolean botRaised = false;
                String botAction;

                if (userBetAmount > botBalance / 3) {
                    botAction = "Bot folds!";
                    pokerCommand.endGame(event.getUser());  // Bot folds, end the game
                } else if (userBetAmount <= botBalance / 3) {
                    botAction = "Bot calls your bet!";
                    botBetAmount = userBetAmount;
                } else {
                    long raiseAmount = (long) (userBetAmount * (0.1 + Math.random() * 0.2));
                    botBetAmount = userBetAmount + raiseAmount;
                    botAction = "Bot raises by " + raiseAmount + "!";
                    botRaised = true;
                }

                // Build Embed
                EmbedBuilder embed = game.getGameStatus(event.getUser(), false);
                embed.addField("Your Bet", String.valueOf(userBetAmount), true);
                embed.addField("Bot's Bet", String.valueOf(botBetAmount), true);
                embed.addField("Bot's Action", botAction, false);

                // If the bot raised, offer the user the ability to call or fold
                if (botRaised) {
                    event.replyEmbeds(embed.build())
                            .addActionRow(Button.primary("poker:call:" + event.getUser().getId(), "Call"),
                                    Button.danger("poker:fold:" + event.getUser().getId(), "Fold"))
                            .queue();
                } else {
                    // If the bot called, move to the next phase
                    dealNextPhase(event, game, embed);
                }

            } catch (NumberFormatException e) {
                event.reply("Invalid bet amount.").setEphemeral(true).queue();
            }
        }
    }

    // This function deals with the next phase based on bets
    // Existing dealNextPhase method for ModalInteractionEvent
    private void dealNextPhase(ModalInteractionEvent event, TexasHoldemGame game, EmbedBuilder embed) {
        if (game.getCommunityCards().size() < 5) {
            if (game.getCommunityCards().size() < 3) {
                game.dealFlop();
            } else if (game.getCommunityCards().size() == 3) {
                game.dealTurn();
            } else if (game.getCommunityCards().size() == 4) {
                game.dealRiver();
            }

            // Update game status after dealing the next card
            Objects.requireNonNull(event.getMessage()).editMessageEmbeds(game.getGameStatus(event.getUser(), false).build()).queue();
        } else {
            // End the game if all community cards have been dealt
            User winner = game.determineWinner(event.getUser());
            String resultText = (winner == event.getUser()) ? "You win!" : "Bot wins!";
            embed.addField("Result", resultText, false);

            // Disable the buttons once the game is over
            Objects.requireNonNull(event.getMessage()).editMessageEmbeds(embed.build())
                    .setComponents(ActionRow.of(
                            Button.primary("poker:bet:" + event.getUser().getId(), "Bet Again").asDisabled(),
                            Button.danger("poker:fold:" + event.getUser().getId(), "Fold").asDisabled()
                    )).queue();

            PokerCommand pokerCommand = (PokerCommand) bot.getBotCommands().getCommandByName("poker");
            pokerCommand.endGame(event.getUser());
        }
    }

    // Overload dealNextPhase for ButtonInteractionEvent calls
    private void dealNextPhase(ButtonInteractionEvent event, TexasHoldemGame game, EmbedBuilder embed) {
        if (game.getCommunityCards().size() < 5) {
            if (game.getCommunityCards().size() < 3) {
                game.dealFlop();
            } else if (game.getCommunityCards().size() == 3) {
                game.dealTurn();
            } else if (game.getCommunityCards().size() == 4) {
                game.dealRiver();
            }

            // Update game status after dealing the next card
            event.getHook().editOriginalEmbeds(game.getGameStatus(event.getUser(), false).build()).queue();
        } else {
            // End the game if all community cards have been dealt
            User winner = game.determineWinner(event.getUser());
            String resultText = (winner == event.getUser()) ? "You win!" : "Bot wins!";
            embed.addField("Result", resultText, false);

            // Disable the buttons once the game is over
            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(ActionRow.of(
                            Button.primary("poker:bet:" + event.getUser().getId(), "Bet Again").asDisabled(),
                            Button.danger("poker:fold:" + event.getUser().getId(), "Fold").asDisabled()
                    )).queue();

            PokerCommand pokerCommand = (PokerCommand) bot.getBotCommands().getCommandByName("poker");
            pokerCommand.endGame(event.getUser());
        }
    }

    /**
     * Adds reset buttons to a deferred reply message action.
     *
     * @param userID the ID of the user who is accessing this menu.
     * @param systemName the name of the system to reset.
     * @param action the WebhookMessageAction<Message> to add components to.
     */
    public static void sendResetMenu(String userID, String systemName, WebhookMessageCreateAction<Message> action) {
        String uuid = userID + ":" + UUID.randomUUID();
        List<Button> components = getResetButtons(uuid, systemName);
        buttons.put(uuid, components);
        action.addActionRow(components).queue(interactionHook -> ButtonListener.disableButtons(uuid, (InteractionHook) interactionHook));
    }

    /**
     * Get a list of buttons for reset embeds (selectable yes and no).
     *
     * @param uuid the unique ID generated for these buttons.
     * @param systemName the name of the system being reset.
     * @return A list of components to use on a reset embed.
     */
    private static List<Button> getResetButtons(String uuid, String systemName) {
        return Arrays.asList(
                Button.success("reset:yes:"+uuid+":"+systemName, Emoji.fromFormatted("\u2714")),
                Button.danger("reset:no:"+uuid+":"+systemName, Emoji.fromUnicode("\u2716"))
        );
    }
}
