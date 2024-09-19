package org.redacted.Commands.Fun.Gamba;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.redacted.util.enums.Cards;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Texas Hold'em Poker game logic.
 */
@Getter
public class TexasHoldemGame {

    private final List<Cards> communityCards = new ArrayList<>();
    private final Map<User, List<Cards>> playerHands = new HashMap<>();
    private final List<Cards> botHand = new ArrayList<>();
    private final Stack<Cards> deck = new Stack<>();
    private final boolean botHasFolded = false;

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

    public void dealFlop() {
        System.out.println("Drawing Flop Cards!!");
        communityCards.add(deck.pop());
        communityCards.add(deck.pop());
        communityCards.add(deck.pop());
    }

    public void dealTurn() {
        System.out.println("Dealing Turn!!");
        communityCards.add(deck.pop());
    }

    public void dealRiver() {
        System.out.println("Dealing River!!");
        communityCards.add(deck.pop());
    }

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

    // Step 1: Find the best hand for a given set of cards
    private List<Cards> getBestHand(List<Cards> holeCards, List<Cards> communityCards) {
        List<Cards> allCards = new ArrayList<>(holeCards);
        allCards.addAll(communityCards);

        // Generate all combinations of 5 cards from the 7 available
        List<List<Cards>> allCombinations = generateFiveCardCombinations(allCards);
        allCombinations.sort(Comparator.comparingInt(this::evaluateHand).reversed());

        // The highest-ranked combination is the best hand
        return allCombinations.get(0);
    }

    // Step 2: Evaluate the strength of a 5-card hand based on poker hand rankings
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

    // Step 3: Compare high cards if hand rankings are tied
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

    // Helper function to generate all 5-card combinations from a set of 7 cards
    private List<List<Cards>> generateFiveCardCombinations(List<Cards> cards) {
        List<List<Cards>> combinations = new ArrayList<>();
        generateCombinations(cards, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    // Recursive helper for combination generation
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

    // Helper functions to determine poker hands
    private boolean isRoyalFlush(List<Cards> hand) {
        return isStraightFlush(hand) && getHighCardValue(hand) == 14;
    }

    private boolean isStraightFlush(List<Cards> hand) {
        return isFlush(hand) && isStraight(hand);
    }

    private boolean isFourOfAKind(List<Cards> hand) {
        return hasNOfAKind(hand, 4);
    }

    private boolean isFullHouse(List<Cards> hand) {
        return hasNOfAKind(hand, 3) && hasNOfAKind(hand, 2);
    }

    private boolean isFlush(List<Cards> hand) {
        return hand.stream().allMatch(card -> card.suit.equals(hand.get(0).suit));
    }

    private boolean isStraight(List<Cards> hand) {
        hand.sort(Comparator.comparingInt(card -> card.value));
        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).value + 1 != hand.get(i + 1).value) return false;
        }
        return true;
    }

    private boolean isThreeOfAKind(List<Cards> hand) {
        return hasNOfAKind(hand, 3);
    }

    private boolean isTwoPair(List<Cards> hand) {
        int pairs = 0;
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                if (hand.get(i).value == hand.get(j).value) pairs++;
            }
        }
        return pairs == 2;
    }

    private boolean isOnePair(List<Cards> hand) {
        return hasNOfAKind(hand, 2);
    }

    private boolean hasNOfAKind(List<Cards> hand, int n) {
        Map<Integer, Long> counts = hand.stream().collect(Collectors.groupingBy(card -> card.value, Collectors.counting()));
        return counts.containsValue((long) n);
    }

    private int getHighCardValue(List<Cards> hand) {
        return hand.stream().mapToInt(card -> card.value).max().orElse(0);
    }
}
