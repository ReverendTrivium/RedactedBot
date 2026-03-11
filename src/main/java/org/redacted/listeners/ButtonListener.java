package org.redacted.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.redacted.Commands.Fun.Gamba.BlackJackCommand;
import org.redacted.Commands.Fun.Gamba.PokerCommand;
import org.redacted.Commands.Fun.Gamba.TexasHoldemGame;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.components.label.Label;


import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for button input and handles all button backend.
 *
 * @author Derrick
 */
public class ButtonListener extends ListenerAdapter {

    public static final int MINUTES_TO_DISABLE = 3;
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(20);

    public static final Map<String, List<MessageEmbed>> menus = new HashMap<>();
    public static final Map<String, List<Button>> buttons = new HashMap<>();
    public static final Map<String, TempEmbed> tempEmbeds = new HashMap<>();

    private final Redacted bot;

    /**
     * Constructor for ButtonListener.
     *
     * @param bot The Redacted bot instance.
     */
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
        String token = UUID.randomUUID().toString();
        String key = userID + ":" + token;

        List<Button> components = getPaginationButtons(userID, token, 0, embeds.size());
        buttons.put(key, components);
        menus.put(key, embeds);

        action
                .setComponents(ActionRow.of(components)) // JDA 6
                .queue(hook -> {
                    if (embeds.size() > 1) {
                        disableButtons(key, hook);
                    }
                });
    }

    /**
     * Get a list of buttons for paginated embeds.
     *
     * @param userId the ID of the user who is accessing this menu.
     * @param token a unique token to identify this pagination session.
     * @param currentPage the current page index (0-based).
     * @param maxPages the total number of embed pages.
     * @return A list of components to use on a paginated embed.
     */
    private static List<Button> getPaginationButtons(String userId, String token, int currentPage, int maxPages) {
        Button previousButton = Button.primary("pagination:prev:" + userId + ":" + token + ":" + currentPage, "Previous");
        Button nextButton     = Button.primary("pagination:next:" + userId + ":" + token + ":" + currentPage, "Next");

        if (currentPage == 0) previousButton = previousButton.asDisabled();
        if (currentPage >= maxPages - 1) nextButton = nextButton.asDisabled();

        Button pageLabelButton = Button.of(
                ButtonStyle.SECONDARY,
                "pagination:page:" + userId + ":" + token + ":" + currentPage,
                (currentPage + 1) + "/" + maxPages
        ).asDisabled();

        return Arrays.asList(previousButton, pageLabelButton, nextButton);
    }

    /**
     * Schedules a timer task to disable buttons and clear cache after a set time.
     *
     * @param key  the map key for the components (userId:token).
     * @param hook an interaction hook pointing to original message.
     */
    public static void disableButtons(String key, InteractionHook hook) {
        Runnable task = () -> {
            List<Button> actionRow = ButtonListener.buttons.get(key);
            if (actionRow == null) return;

            List<Button> newActionRow = new ArrayList<>();
            for (Button button : actionRow) {
                newActionRow.add(button.asDisabled());
            }

            hook.editOriginalComponents(ActionRow.of(newActionRow))
                    .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));

            ButtonListener.buttons.remove(key);
            ButtonListener.menus.remove(key);
        };

        ButtonListener.executor.schedule(task, MINUTES_TO_DISABLE, TimeUnit.MINUTES);
    }

    /**
     * Handles button interactions.
     * This method processes button clicks, checks if the interaction is in an NSFW channel,
     * and determines the action based on the button ID.
     * It also ensures that the user interacting is the one involved in the action.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to edit the original message with the updated embeds and buttons.
     *              * @see ButtonInteractionEvent
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // Safer NSFW check (only for TextChannel)
        if (event.getChannel() instanceof TextChannel tc && tc.isNSFW()) {
            return;
        }

        String componentId = event.getComponentId();
        String[] pressedArgs = componentId.split(":");

        if (pressedArgs.length < 2) {
            event.deferEdit().queue();
            return;
        }

        String action = pressedArgs[0];

        // For most of your button types, you’ve embedded userId at index 2
        // We'll only enforce user check when the ID format matches.
        // Expected for pagination/reset/blackjack/poker/editembed: <action>:<subaction>:<userId>:...
        if (pressedArgs.length >= 3) {
            long expectedUserId;
            try {
                expectedUserId = Long.parseLong(pressedArgs[2]);
            } catch (NumberFormatException e) {
                // If this button isn't user-scoped, just ignore user check
                expectedUserId = -1;
            }

            if (expectedUserId != -1 && expectedUserId != event.getUser().getIdLong()) {
                // Optional: tell them it’s not their menu
                event.reply("❌ This menu isn't yours.").setEphemeral(true).queue();
                return;
            }
        }

        switch (action) {
            case "pagination" -> handlePagination(event, pressedArgs);
            case "reset"      -> handleReset(event, pressedArgs);
            case "blackjack"  -> {
                // old format assumed: blackjack:<subaction>:<userId>:<token>:<bet>
                // keep your existing uuid pattern if you already do it elsewhere
                if (pressedArgs.length >= 4) {
                    String uuidKey = pressedArgs[2] + ":" + pressedArgs[3];
                    handleBlackjack(event, pressedArgs, uuidKey);
                } else {
                    event.deferEdit().queue();
                }
            }
            case "poker"      -> {
                // poker:<subaction>:<userId>:...
                if (pressedArgs.length >= 4) {
                    String uuidKey = pressedArgs[2] + ":" + pressedArgs[3];
                    handlePoker(event, pressedArgs, uuidKey);
                } else {
                    handlePoker(event, pressedArgs, null);
                }
            }
            case "editembed"  -> handleEditEmbedConfirmation(event, pressedArgs);
            default           -> event.deferEdit().queue();
        }
    }

    /**
     * Handles the Pagination of Embedded Messages.
     * Expected pressedArgs format: pagination:<subaction>:<userId>:<token>:<currentPage>
     *
     * @param event an interaction event pointing to the original event.
     *              This event is used to edit the original message with the updated embeds and buttons.
     * @param pressedArgs the arguments passed in the button interaction, including action type and page number.
     *              This array contains the action type (e.g., "next", "prev") and the current page number.
     */
    private void handlePagination(ButtonInteractionEvent event, String[] pressedArgs) {
        if (pressedArgs.length < 5) {
            event.deferEdit().queue();
            return;
        }

        String subAction = pressedArgs[1];  // prev / next / page
        String userId    = pressedArgs[2];
        String token     = pressedArgs[3];
        int currentPage;

        try {
            currentPage = Integer.parseInt(pressedArgs[4]);
        } catch (NumberFormatException e) {
            event.deferEdit().queue();
            return;
        }

        String key = userId + ":" + token;

        List<Button> components = buttons.get(key);
        if (components == null) {
            event.deferEdit().queue();
            return;
        }

        List<MessageEmbed> embeds = menus.get(key);
        if (embeds == null || embeds.isEmpty()) {
            event.deferEdit().queue();
            return;
        }

        int targetPage = currentPage;
        if ("next".equals(subAction)) {
            targetPage = currentPage + 1;
        } else if ("prev".equals(subAction)) {
            targetPage = currentPage - 1;
        } else {
            // page label button - ignore
            event.deferEdit().queue();
            return;
        }

        if (targetPage < 0 || targetPage >= embeds.size()) {
            event.deferEdit().queue();
            return;
        }

        List<Button> newComponents = getPaginationButtons(userId, token, targetPage, embeds.size());
        buttons.put(key, newComponents);

        event.editComponents(ActionRow.of(newComponents))
                .setEmbeds(embeds.get(targetPage))
                .queue();
    }

    /**
     * Handles the reset of a system.
     * This method checks if the user confirmed the reset action and performs the reset accordingly.
     * It updates the guild data and sends a confirmation message to the user.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to reply to the user with the reset status.
     * @param pressedArgs the arguments passed in the button interaction, including action type and system name.
     *              This array contains the action type (e.g., "yes", "no") and the system name to reset.
     */
    private void handleReset(ButtonInteractionEvent event, String[] pressedArgs) {
        if (pressedArgs.length < 5) {
            event.deferEdit().queue();
            return;
        }

        String choice = pressedArgs[1];     // yes/no
        String systemName = pressedArgs[4];

        event.deferEdit().queue();

        if ("yes".equals(choice)) {
            GuildData data = GuildData.get(Objects.requireNonNull(event.getGuild()), bot);
            if (systemName.equalsIgnoreCase("Suggestion")) data.getSuggestionHandler().reset();
            else if (systemName.equalsIgnoreCase("Greeting")) data.getGreetingHandler().reset();

            MessageEmbed embed = EmbedUtils.createSuccess(systemName + " system was successfully reset!");
            event.getHook().editOriginalComponents(Collections.emptyList()).setEmbeds(embed).queue();
        } else if ("no".equals(choice)) {
            MessageEmbed embed = EmbedUtils.createError(systemName + " system was **NOT** reset!");
            event.getHook().editOriginalComponents(Collections.emptyList()).setEmbeds(embed).queue();
        }
    }

    /**
     * Handles blackjack button interactions.
     * This method processes the user's action in a blackjack game, such as hitting or standing.
     * It checks if there is an active game for the user, and if so, it updates the game state accordingly.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to reply to the user with the game status or actions.
     * @param pressedArgs the arguments passed in the button interaction, including action type and user IDs.
     *              This array contains the action type (e.g., "hit", "stand") and the user ID.
     * @param uuidKey the unique identifier for the interaction, used to manage the game state.
     *              This UUID is used to retrieve the game instance associated with the user.
     */
    private void handleBlackjack(ButtonInteractionEvent event, String[] pressedArgs, String uuidKey) {
        // Your existing logic assumes bet is pressedArgs[4]
        if (pressedArgs.length < 5) {
            event.deferEdit().queue();
            return;
        }

        long bet = Long.parseLong(pressedArgs[4]);
        MessageEmbed embed = null;

        if ("hit".equals(pressedArgs[1])) {
            embed = BlackJackCommand.hit(event.getGuild(), event.getUser(), bet, uuidKey, bot);
        } else if ("stand".equals(pressedArgs[1])) {
            embed = BlackJackCommand.stand(event.getGuild(), event.getUser(), bet, uuidKey, bot);
        }

        List<Button> row = buttons.get(uuidKey);
        if (row == null) {
            event.editMessageEmbeds(embed).queue();
            return;
        }

        event.editComponents(ActionRow.of(row)).setEmbeds(embed).queue();
    }

    /**
     * Handles poker button interactions.
     * This method processes the user's action in a poker game, such as betting, folding, calling, or raising.
     * It checks if there is an active game for the user, and if so, it updates the game state accordingly.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to reply to the user with the game status or actions.
     * @param pressedArgs the arguments passed in the button interaction, including action type and user IDs.
     *              This array contains the action type (e.g., "bet", "fold", "call", "raise") and the user ID.
     * @param uuid the unique identifier for the interaction, used to manage the game state.
     *              This UUID is used to retrieve the game instance associated with the user.
     */
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
                TextInput betInput = TextInput.create("bet_input", TextInputStyle.SHORT)
                        .setPlaceholder("Enter your bet amount")
                        .setRequired(true)
                        .build();

                event.replyModal(
                        Modal.create("bet_modal", "Place Your Bet")
                                .addComponents(Label.of("Bet Amount", betInput))
                                .build()
                ).queue();
            }
            case "fold" -> {
                event.reply(user.getName() + " folded.").queue();
                pokerCommand.endGame(user);
            }
            case "call" -> {
                EmbedBuilder embed = game.getGameStatus(user, false);
                event.getMessage().editMessageEmbeds(embed.build()).queue();
                dealNextPhase(event, game, embed);
            }
            case "raise" -> {
                TextInput raiseInput = TextInput.create("raise_input", TextInputStyle.SHORT)
                        .setPlaceholder("Enter your raise amount")
                        .setRequired(true)
                        .build();

                event.replyModal(
                        Modal.create("raise_modal", "Raise Amount")
                                .addComponents(Label.of("Raise Amount", raiseInput))
                                .build()
                ).queue();
            }
        }
    }

    /**
     * Handles modal interactions for betting or raising in poker.
     * This method processes the user's bet or raise amount, checks their balance, and updates the game state accordingly.
     * It also handles the bot's response based on the user's action.
     *
     * @param event the ModalInteractionEvent containing the interaction data.
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("bet_modal") || event.getModalId().equals("raise_modal")) {

            String key = event.getModalId().equals("raise_modal") ? "raise_input" : "bet_input";
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

                economyHandler.removeMoney(event.getUser().getIdLong(), userBetAmount);

                long botBalance = 1000;
                long botBetAmount = 0;
                boolean botRaised = false;
                String botAction;

                if (userBetAmount > botBalance / 3) {
                    botAction = "Bot folds!";
                    pokerCommand.endGame(event.getUser());
                } else if (userBetAmount <= botBalance / 3) {
                    botAction = "Bot calls your bet!";
                    botBetAmount = userBetAmount;
                } else {
                    long raiseAmount = (long) (userBetAmount * (0.1 + Math.random() * 0.2));
                    botBetAmount = userBetAmount + raiseAmount;
                    botAction = "Bot raises by " + raiseAmount + "!";
                    botRaised = true;
                }

                EmbedBuilder embed = game.getGameStatus(event.getUser(), false);
                embed.addField("Your Bet", String.valueOf(userBetAmount), true);
                embed.addField("Bot's Bet", String.valueOf(botBetAmount), true);
                embed.addField("Bot's Action", botAction, false);

                if (botRaised) {
                    event.replyEmbeds(embed.build())
                            .setComponents(ActionRow.of(
                                    Button.primary("poker:call:" + event.getUser().getId(), "Call"),
                                    Button.danger("poker:fold:" + event.getUser().getId(), "Fold")
                            ))
                            .queue();
                } else {
                    dealNextPhase(event, game, embed);
                }

            } catch (NumberFormatException e) {
                event.reply("Invalid bet amount.").setEphemeral(true).queue();
            }
        }
    }

    /**
     * Handles the next phase of the poker game after a modal interaction.
     * This method checks the number of community cards dealt and proceeds to deal the next card (flop, turn, or river).
     * If all community cards have been dealt, it determines the winner and updates the game status accordingly.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to edit the original message with the updated game status.
     */
    private void dealNextPhase(ModalInteractionEvent event, TexasHoldemGame game, EmbedBuilder embed) {
        if (game.getCommunityCards().size() < 5) {
            if (game.getCommunityCards().size() < 3) {
                game.dealFlop();
            } else if (game.getCommunityCards().size() == 3) {
                game.dealTurn();
            } else if (game.getCommunityCards().size() == 4) {
                game.dealRiver();
            }

            Objects.requireNonNull(event.getMessage())
                    .editMessageEmbeds(game.getGameStatus(event.getUser(), false).build())
                    .queue();
        } else {
            User winner = game.determineWinner(event.getUser());
            String resultText = (winner == event.getUser()) ? "You win!" : "Bot wins!";
            embed.addField("Result", resultText, false);

            Objects.requireNonNull(event.getMessage())
                    .editMessageEmbeds(embed.build())
                    .setComponents(ActionRow.of(
                            Button.primary("poker:bet:" + event.getUser().getId(), "Bet Again").asDisabled(),
                            Button.danger("poker:fold:" + event.getUser().getId(), "Fold").asDisabled()
                    ))
                    .queue();

            PokerCommand pokerCommand = (PokerCommand) bot.getBotCommands().getCommandByName("poker");
            pokerCommand.endGame(event.getUser());
        }
    }

    /**
     * Handles the next phase of the poker game after a button interaction.
     * This method checks the number of community cards and deals the next card accordingly.
     * If all community cards have been dealt, it determines the winner and updates the game status.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     *              This event is used to edit the original message with the updated game status.
     */
    private void dealNextPhase(ButtonInteractionEvent event, TexasHoldemGame game, EmbedBuilder embed) {
        if (game.getCommunityCards().size() < 5) {
            if (game.getCommunityCards().size() < 3) {
                game.dealFlop();
            } else if (game.getCommunityCards().size() == 3) {
                game.dealTurn();
            } else if (game.getCommunityCards().size() == 4) {
                game.dealRiver();
            }

            event.getHook().editOriginalEmbeds(game.getGameStatus(event.getUser(), false).build()).queue();
        } else {
            User winner = game.determineWinner(event.getUser());
            String resultText = (winner == event.getUser()) ? "You win!" : "Bot wins!";
            embed.addField("Result", resultText, false);

            event.getHook().editOriginalEmbeds(embed.build())
                    .setComponents(ActionRow.of(
                            Button.primary("poker:bet:" + event.getUser().getId(), "Bet Again").asDisabled(),
                            Button.danger("poker:fold:" + event.getUser().getId(), "Fold").asDisabled()
                    ))
                    .queue();

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
        String token = UUID.randomUUID().toString();
        String key = userID + ":" + token;

        List<Button> components = getResetButtons(userID, token, systemName);
        buttons.put(key, components);

        // JDA 6: set components explicitly
        action.setComponents(ActionRow.of(components))
                .queue(hook -> disableButtons(key, (InteractionHook) hook));
    }

    /**
     * Get a list of buttons for reset embeds (selectable yes and no).
     *
     * @param userId the unique ID of the user who is accessing this menu.
     * @param token a unique token to identify this reset session.
     * @param systemName the name of the system being reset.
     * @return A list of components to use on a reset embed.
     */
    private static List<Button> getResetButtons(String userId, String token, String systemName) {
        return Arrays.asList(
                Button.success("reset:yes:" + userId + ":" + token + ":" + systemName, Emoji.fromFormatted("\u2714")),
                Button.danger("reset:no:" + userId + ":" + token + ":" + systemName, Emoji.fromUnicode("\u2716"))
        );
    }

    /**
     * Handles the confirmation or cancellation of an embed edit.
     * This method checks if the user is the original editor and processes the confirmation or cancellation accordingly.
     *
     * @param event the ButtonInteractionEvent containing the interaction data.
     * @param args  the arguments passed in the button interaction, including action type and user IDs.
     */
    private void handleEditEmbedConfirmation(ButtonInteractionEvent event, String[] args) {
        String subAction = args[1]; // "confirm" or "cancel"
        String userId = args[2];
        String uuid = args[3];
        String messageId = args[4];

        if (!event.getUser().getId().equals(userId)) {
            event.reply("❌ Only the original editor can confirm or cancel this edit.").setEphemeral(true).queue();
            return;
        }

        TempEmbed draft = tempEmbeds.get(uuid);
        if (draft == null) {
            event.reply("⚠️ This embed confirmation has expired.").setEphemeral(true).queue();
            return;
        }

        if (subAction.equals("cancel")) {
            tempEmbeds.remove(uuid);
            event.reply("❎ Embed edit cancelled.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ Guild not found.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = guild.getTextChannelById(draft.original.getChannelId());
        if (channel == null) {
            event.reply("❌ Channel not found.").setEphemeral(true).queue();
            return;
        }

        channel.retrieveMessageById(messageId).queue(original -> {
            original.editMessageEmbeds(draft.embed.build()).queue();

            draft.original.setTitle(draft.embed.build().getTitle());
            draft.original.setDescription(draft.embed.build().getDescription());
            draft.original.setImageUrl(draft.imageUrl);
            draft.original.setThumbnailUrl(draft.thumbnailUrl);
            draft.original.setTimestamp(Instant.now());

            MongoCollection<SavedEmbed> collection = GuildData.getDatabase().getSavedEmbedsCollection(guild.getIdLong());
            collection.replaceOne(Filters.eq("messageId", messageId), draft.original);

            tempEmbeds.remove(uuid);
            event.reply("✅ Embed updated successfully.").setEphemeral(true).queue();
        }, fail -> event.reply("❌ Failed to update the original message.").setEphemeral(true).queue());
    }

    /**
     * Temporary embed class to hold embed data for editing.
     * This class is used to store the embed being edited, the original saved embed, and any image or thumbnail URLs.
     * It is used to facilitate the editing of embeds through button interactions.
     *
     * * @param embed the EmbedBuilder containing the embed data.
     * * @param original the original SavedEmbed object from the database.
     * * @param imageUrl the URL of the image to be displayed in the embed.
     * * @param thumbnailUrl the URL of the thumbnail to be displayed in the embed.
     */
    public static class TempEmbed {
        public final EmbedBuilder embed;
        public final SavedEmbed original;
        public final String imageUrl;
        public final String thumbnailUrl;

        /**
         * Constructor for TempEmbed.
         *
         * @param embed the EmbedBuilder containing the embed data.
         * @param original the original SavedEmbed object from the database.
         * @param imageUrl the URL of the image to be displayed in the embed.
         * @param thumbnailUrl the URL of the thumbnail to be displayed in the embed.
         */
        public TempEmbed(EmbedBuilder embed, SavedEmbed original, String imageUrl, String thumbnailUrl) {
            this.embed = embed;
            this.original = original;
            this.imageUrl = imageUrl;
            this.thumbnailUrl = thumbnailUrl;
        }
    }
}
