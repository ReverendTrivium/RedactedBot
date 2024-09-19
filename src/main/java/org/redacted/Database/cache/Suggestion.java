package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO object that stores data for a guild's suggestion board.
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

    /** No-argument constructor required by MongoDB POJO codec */
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

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }
}
