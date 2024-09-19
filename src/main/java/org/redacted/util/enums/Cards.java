package org.redacted.util.enums;

import lombok.Getter;

/**
 * Represents playing cards with a value, suit, and emoji.
 *
 * @author Derrick Eberlein
 */
@Getter
public enum Cards {
    TWO_HEARTS(2, "Hearts", "<:2H:1283783757807091776>"),
    THREE_HEARTS(3, "Hearts", "<:3H:1283783755705618433>"),
    FOUR_HEARTS(4, "Hearts", "<:4H:1283783728904146966>"),
    FIVE_HEARTS(5, "Hearts", "<:5H:1283783726911721573>"),
    SIX_HEARTS(6, "Hearts", "<:6H:1283783749867409418>"),
    SEVEN_HEARTS(7, "Hearts", "<:7H:1283783747568926725>"),
    EIGHT_HEARTS(8, "Hearts", "<:8H:1283783724072173712>"),
    NINE_HEARTS(9, "Hearts", "<:9H:1283783740287483924>"),
    TEN_HEARTS(10, "Hearts", "<:10H:1283783753658794099>"),
    JACK_HEARTS(10, "Hearts", "<:jH:1283783732943130705>"),
    QUEEN_HEARTS(10, "Hearts", "<:qH:1283783745404670025>"),
    KING_HEARTS(10, "Hearts", "<:kH:1283783737351344198>"),
    ACE_HEARTS(11, "Hearts", "<:aH:1283783721807380510>", true),

    TWO_SPADES(2, "Spades", "<:2S:1283780091909767189>"),
    THREE_SPADES(3, "Spades", "<:3S:1283780222705209344>"),
    FOUR_SPADES(4, "Spades", "<:4S:1283780284482981959>"),
    FIVE_SPADES(5, "Spades", "<:5S:1283780350195142738>"),
    SIX_SPADES(6, "Spades", "<:6S:1283780409594740808>"),
    SEVEN_SPADES(7, "Spades", "<:7S:1283780456311029782>"),
    EIGHT_SPADES(8, "Spades", "<:8S:1283780524594298912>"),
    NINE_SPADES(9, "Spades", "<:9S:1283780578465808496>"),
    TEN_SPADES(10, "Spades", "<:10S:1283780616583905320>"),
    JACK_SPADES(10, "Spades", "<:jS:1283780676197290048>"),
    QUEEN_SPADES(10, "Spades", "<:qS:1283780773383504005>"),
    KING_SPADES(10, "Spades", "<:kS:1283780808729038848>"),
    ACE_SPADES(11, "Spades", "<:aS:1283780845634715698>", true),

    TWO_CLUBS(2, "Clubs", "<:2C:1283784239342682235>"),
    THREE_CLUBS(3, "Clubs", "<:3C:1283784237203329104>"),
    FOUR_CLUBS(4, "Clubs", "<:4C:1283784234166784030>"),
    FIVE_CLUBS(5, "Clubs", "<:5C:1283784231251742854>"),
    SIX_CLUBS(6, "Clubs", "<:6C:1283784228466720879>"),
    SEVEN_CLUBS(7, "Clubs", "<:7C:1283784225891549194>"),
    EIGHT_CLUBS(8, "Clubs", "<:8C:1283784223945392241>"),
    NINE_CLUBS(9, "Clubs", "<:9C:1283784222296903710>"),
    TEN_CLUBS(10, "Clubs", "<:10C:1283784220115865601>"),
    JACK_CLUBS(10, "Clubs", "<:jC:1283784218077564968>"),
    QUEEN_CLUBS(10, "Clubs", "<:qC:1283784214587904071>"),
    KING_CLUBS(10, "Clubs", "<:kC:1283784211131666473>"),
    ACE_CLUBS(11, "Clubs", "<:aC:1283784198477582449>", true),

    TWO_DIAMONDS(2, "Diamonds", "<:2D:1283785086331785289>"),
    THREE_DIAMONDS(3, "Diamonds", "<:3D:1283785084276572237>"),
    FOUR_DIAMONDS(4, "Diamonds", "<:4D:1283785082095534121>"),
    FIVE_DIAMONDS(5, "Diamonds", "<:5D:1283785079952379954>"),
    SIX_DIAMONDS(6, "Diamonds", "<:6D:1283785077444050994>"),
    SEVEN_DIAMONDS(7, "Diamonds", "<:7D:1283785074403311679>"),
    EIGHT_DIAMONDS(8, "Diamonds", "<:8D:1283785072364884081>"),
    NINE_DIAMONDS(9, "Diamonds", "<:9D:1283785069709885450>"),
    TEN_DIAMONDS(10, "Diamonds", "<:10D:1283785066966814803>"),
    JACK_DIAMONDS(10, "Diamonds", "<:jD:1283785064198705174>"),
    QUEEN_DIAMONDS(10, "Diamonds", "<:qD:1283785060360785941>"),
    KING_DIAMONDS(10, "Diamonds", "<:kD:1283785056162156565>"),
    ACE_DIAMONDS(11, "Diamonds", "<:aD:1283785051535835176>", true);

    public final int value;
    public final String suit;  // New field for suit
    public final String emoji;
    public final boolean isAce;

    /**
     * Constructor for making non-ace playing card
     * @param value the value that this card holds.
     * @param suit the suit of the card.
     * @param emoji the custom emoji for this card.
     */
    Cards(int value, String suit, String emoji) {
        this.value = value;
        this.suit = suit;
        this.emoji = emoji;
        this.isAce = false;
    }

    /**
     * Constructor for making an ace playing card
     * @param value the value that this card holds.
     * @param suit the suit of the card.
     * @param emoji the custom emoji for this card.
     * @param isAce whether this card is an ace.
     */
    Cards(int value, String suit, String emoji, boolean isAce) {
        this.value = value;
        this.suit = suit;
        this.emoji = emoji;
        this.isAce = isAce;
    }
}
