package org.redacted.Commands.Fun.Gamba;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.redacted.util.enums.Cards;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a game of Texas Hold'em poker.
 * Manages the deck, player hands, community cards, and game logic.
 * Provides methods to deal cards, evaluate hands, and determine the winner.
 *
 * @author Derrick Eberlein
 */
@Getter
public class TexasHoldemGame {

    private final List<Cards> communityCards = new ArrayList<>();
    private final Map<User, List<Cards>> playerHands = new HashMap<>();
    private final List<Cards> botHand = new ArrayList<>();
    private final Stack<Cards> deck = new Stack<>();
    private final boolean botHasFolded = false;

    /**
     * Constructor for the TexasHoldemGame.
     * Initializes the deck, shuffles it, and deals cards to players and the bot.
     *
     * @param players List of users playing the game.
     */
    public TexasHoldemGame(List<User> players) {
        // Initialize and shuffle the deck
        List<Cards> allCards = Arrays.asList(Cards.values());
        deck.addAll(allCards);
        Collections.shuffle(deck);

        // Deal two cards to each player
        for (User player : players) {
            List<Cards> hand = new ArrayList<>();
            hand.add(deck.pop());
            hand.add(deck.pop());
            playerHands.put(player, hand);
        }

        // Deal two cards to the bot
        botHand.add(deck.pop());
        botHand.add(deck.pop());
    }

    /**
     * Deals the flop (first three community cards).
     * Adds three cards from the deck to the community cards.
     */
    public void dealFlop() {
        System.out.println("Drawing Flop Cards!!");
        communityCards.add(deck.pop());
        communityCards.add(deck.pop());
        communityCards.add(deck.pop());
    }

    /**
     * Deals the turn (fourth community card).
     * Adds one card from the deck to the community cards.
     */
    public void dealTurn() {
        System.out.println("Dealing Turn!!");
        communityCards.add(deck.pop());
    }

    /**
     * Deals the river (fifth community card).
     * Adds one card from the deck to the community cards.
     */
    public void dealRiver() {
        System.out.println("Dealing River!!");
        communityCards.add(deck.pop());
    }

    /**
     * Gets the current game status for a user.
     * Displays the player's hand, bot's hand (if revealed), and community cards.
     *
     * @param user The user whose game status is requested.
     * @param showBotHand Whether to show the bot's hand or not.
     * @return An EmbedBuilder containing the game status.
     */
    public EmbedBuilder getGameStatus(User user, boolean showBotHand) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Texas Hold'em Poker");

        // Show player's cards
        List<Cards> playerHand = playerHands.get(user);
        StringBuilder handText = new StringBuilder();
        for (Cards card : playerHand) {
            handText.append(card.emoji).append(" ");
        }
        embed.addField("Your Hand", handText.toString(), true);

        // Show bot's hand (only one card if not revealed)
        StringBuilder botHandText = new StringBuilder();
        if (showBotHand) {
            for (Cards card : botHand) {
                botHandText.append(card.emoji).append(" ");
            }
        } else {
            botHandText.append(botHand.get(0).emoji).append(" <:cardback:1283788387576320021>");
        }
        embed.addField("Bot's Hand", botHandText.toString(), true);

        // Show community cards
        StringBuilder communityText = new StringBuilder();
        for (Cards card : communityCards) {
            communityText.append(card.emoji).append(" ");
        }
        embed.addField("Community Cards", communityText.toString(), false);

        return embed;
    }

    /**
     * Determines the winner of the game based on the best hand.
     * Compares the player's best hand against the bot's best hand.
     *
     * @param player The user who is playing against the bot.
     * @return The winning user, or null if the bot wins.
     */
    public User determineWinner(User player) {
        List<Cards> playerBestHand = getBestHand(playerHands.get(player), communityCards);
        List<Cards> botBestHand = getBestHand(botHand, communityCards);

        int playerRank = evaluateHand(playerBestHand);
        int botRank = evaluateHand(botBestHand);

        // Compare the hand rankings
        if (playerRank > botRank) {
            return player;
        } else if (playerRank < botRank) {
            return null; // Bot wins
        } else {
            // If the ranks are the same, compare high cards
            return compareHighCards(playerBestHand, botBestHand) > 0 ? player : null;
        }
    }

    /**
     * Gets the best 5-card hand from the player's hole cards and community cards.
     * Generates all possible combinations of 5 cards and evaluates them to find the best hand.
     *
     * @param holeCards The player's hole cards.
     * @param communityCards The community cards.
     * @return The best 5-card hand as a list of Cards.
     */
    private List<Cards> getBestHand(List<Cards> holeCards, List<Cards> communityCards) {
        List<Cards> allCards = new ArrayList<>(holeCards);
        allCards.addAll(communityCards);

        // Generate all combinations of 5 cards from the 7 available
        List<List<Cards>> allCombinations = generateFiveCardCombinations(allCards);
        allCombinations.sort(Comparator.comparingInt(this::evaluateHand).reversed());

        // The highest-ranked combination is the best hand
        return allCombinations.get(0);
    }

    /**
     * Evaluates the hand ranking based on poker rules.
     * Returns an integer representing the hand's strength (higher is better).
     *
     * @param hand The 5-card hand to evaluate.
     * @return An integer representing the hand's rank.
     */
    private int evaluateHand(List<Cards> hand) {
        // Poker hand ranking logic goes here, e.g., check for Flush, Straight, etc.
        if (isRoyalFlush(hand)) return 10;
        if (isStraightFlush(hand)) return 9;
        if (isFourOfAKind(hand)) return 8;
        if (isFullHouse(hand)) return 7;
        if (isFlush(hand)) return 6;
        if (isStraight(hand)) return 5;
        if (isThreeOfAKind(hand)) return 4;
        if (isTwoPair(hand)) return 3;
        if (isOnePair(hand)) return 2;
        return getHighCardValue(hand); // Return high card value as fallback
    }

    /**
     * Compares the high cards of two hands to determine which is stronger.
     * Used when both hands have the same rank.
     *
     * @param hand1 The first hand to compare.
     * @param hand2 The second hand to compare.
     * @return A positive number if hand1 is stronger, negative if hand2 is stronger, or 0 if they are equal.
     */
    private int compareHighCards(List<Cards> hand1, List<Cards> hand2) {
        // Sort both hands by card value (Descending order)
        hand1.sort(Comparator.comparingInt(Cards::getValue).reversed());
        hand2.sort(Comparator.comparingInt(Cards::getValue).reversed());

        // Compare each card starting from the highest
        for (int i = 0; i < 5; i++) {
            int comparison = Integer.compare(hand1.get(i).getValue(), hand2.get(i).getValue());
            if (comparison != 0) return comparison;
        }
        return 0; // Hands are completely tied
    }

    /**
     * Generates all combinations of 5 cards from the given list of cards.
     * Uses a recursive approach to generate combinations.
     *
     * @param cards The list of cards to generate combinations from.
     * @return A list of lists, each containing a combination of 5 cards.
     */
    private List<List<Cards>> generateFiveCardCombinations(List<Cards> cards) {
        List<List<Cards>> combinations = new ArrayList<>();
        generateCombinations(cards, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    /**
     * Recursive helper method to generate combinations of 5 cards.
     *
     * @param cards The list of cards to choose from.
     * @param index The current index in the list of cards.
     * @param current The current combination being built.
     * @param combinations The list to store all valid combinations.
     */
    private void generateCombinations(List<Cards> cards, int index, List<Cards> current, List<List<Cards>> combinations) {
        if (current.size() == 5) {
            combinations.add(new ArrayList<>(current));
            return;
        }
        for (int i = index; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, i + 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Checks if the hand is a Royal Flush.
     * A Royal Flush is a Straight Flush with the highest card being an Ace.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand is a Royal Flush, false otherwise.
     */
    private boolean isRoyalFlush(List<Cards> hand) {
        return isStraightFlush(hand) && getHighCardValue(hand) == 14;
    }

    /**
     * Checks if the hand is a Straight Flush.
     * A Straight Flush is a Flush with all cards in consecutive order.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand is a Straight Flush, false otherwise.
     */
    private boolean isStraightFlush(List<Cards> hand) {
        return isFlush(hand) && isStraight(hand);
    }

    /**
     * Checks if the hand has N of a kind.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand has N of a kind, false otherwise.
     */
    private boolean isFourOfAKind(List<Cards> hand) {
        return hasNOfAKind(hand, 4);
    }

    /**
     * Checks if the hand is a Full House.
     * A Full House consists of three cards of one rank and two cards of another rank.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand is a Full House, false otherwise.
     */
    private boolean isFullHouse(List<Cards> hand) {
        return hasNOfAKind(hand, 3) && hasNOfAKind(hand, 2);
    }

    /**
     * Checks if the hand is a Flush.
     * A Flush consists of all cards of the same suit.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand is a Flush, false otherwise.
     */
    private boolean isFlush(List<Cards> hand) {
        return hand.stream().allMatch(card -> card.suit.equals(hand.get(0).suit));
    }

    /**
     * Checks if the hand is a Straight.
     * A Straight consists of all cards in consecutive order, regardless of suit.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand is a Straight, false otherwise.
     */
    private boolean isStraight(List<Cards> hand) {
        hand.sort(Comparator.comparingInt(card -> card.value));
        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).value + 1 != hand.get(i + 1).value) return false;
        }
        return true;
    }

    /**
     * Checks if the hand has three of a kind.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand has three of a kind, false otherwise.
     */
    private boolean isThreeOfAKind(List<Cards> hand) {
        return hasNOfAKind(hand, 3);
    }

    /**
     * Checks if the hand has two pairs.
     * A Two Pair consists of two cards of one rank and two cards of another rank.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand has two pairs, false otherwise.
     */
    private boolean isTwoPair(List<Cards> hand) {
        int pairs = 0;
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                if (hand.get(i).value == hand.get(j).value) pairs++;
            }
        }
        return pairs == 2;
    }

    /**
     * Checks if the hand has one pair.
     *
     * @param hand The 5-card hand to check.
     * @return True if the hand has one pair, false otherwise.
     */
    private boolean isOnePair(List<Cards> hand) {
        return hasNOfAKind(hand, 2);
    }

    /**
     * Checks if the hand has N of a kind.
     *
     * @param hand The 5-card hand to check.
     * @param n The number of cards of the same rank to check for.
     * @return True if the hand has N of a kind, false otherwise.
     */
    private boolean hasNOfAKind(List<Cards> hand, int n) {
        Map<Integer, Long> counts = hand.stream().collect(Collectors.groupingBy(card -> card.value, Collectors.counting()));
        return counts.containsValue((long) n);
    }

    /**
     * Gets the highest card value in the hand.
     *
     * @param hand The 5-card hand to check.
     * @return The value of the highest card in the hand.
     */
    private int getHighCardValue(List<Cards> hand) {
        return hand.stream().mapToInt(card -> card.value).max().orElse(0);
    }
}
