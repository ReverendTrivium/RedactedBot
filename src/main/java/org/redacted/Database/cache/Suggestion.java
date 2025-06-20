package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion Class
 * This class represents a suggestion board for a Discord guild.
 * It includes fields for the guild ID, message IDs, authors, channel ID,
 * suggestion number, and settings for response DM and anonymity.
 *
 * @author Derrick Eberlein
 */
@Setter
@Getter
public class Suggestion {

    /** The ID of the guild this belongs to. */
    private long guild;

    /** Ordered list of suggestion message IDs. */
    private List<Long> messages;

    /** Ordered list of authors for each suggestion ID. */
    private List<Long> authors;

    /** The ID of the TextChannel where suggestions are displayed. */
    private Long channel;

    /** The number of the next suggestion. */
    private long number;

    /** Whether or not responses DM the suggestion author */
    @BsonProperty("response_dm")
    private boolean responseDM;

    /** Whether or not suggestions are anonymous */
    @BsonProperty("is_anonymous")
    private boolean isAnonymous;

    /**
     * Default constructor initializes the suggestion board with default values.
     * This is useful for creating a new instance without any specific guild ID.
     */
    public Suggestion() {
        this.messages = new ArrayList<>();
        this.authors = new ArrayList<>();
        this.number = 1;
        this.responseDM = false;
        this.isAnonymous = false;
    }

    /**
     * Creates a brand-new suggetion board without existing data.
     *
     * @param guild ID for the guild.
     */
    public Suggestion(long guild) {
        this.guild = guild;
        this.channel = null;
        this.number = 1;
        this.responseDM = false;
        this.isAnonymous = false;
        this.messages = new ArrayList<>();
        this.authors = new ArrayList<>();
    }

    /**
     * Returns whether the suggestion board is anonymous.
     *
     * @return true if suggestions are anonymous, false otherwise.
     */
    public boolean isAnonymous() {
        return isAnonymous;
    }

    /**
     * Sets whether suggestions are anonymous.
     *
     * @param anonymous true if suggestions should be anonymous, false otherwise.
     */
    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }
}
