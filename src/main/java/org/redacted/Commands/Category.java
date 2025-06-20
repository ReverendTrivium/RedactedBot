package org.redacted.Commands;

/**
 * Category that represents a group of similar commands.
 * Each category has a name and an emoji.
 *
 * @author Derrick Eberlein
 */
public enum Category {
    STAFF(":computer:", "Staff"),
    MUSIC(":musical_note:", "Music"),
    FUN(":smile:", "Fun"),
    UTILITY(":tools:", "Utility"),
    GREETINGS(":wave:", "Greetings"),
    ECONOMY(":moneybag:", "Economy"),
    SUGGESTIONS(":thought_balloon:", "Suggestions"),
    GAMBA(":game_die:", "Gamba");

    public final String emoji;
    public final String name;

    /**
     * Constructor for the Category enum.
     *
     * @param emoji The emoji representing the category.
     * @param name  The name of the category.
     */
    Category(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }
}
