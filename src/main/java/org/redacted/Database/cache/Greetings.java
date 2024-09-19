package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * POJO object that stores server greeting/farewell data.
 *
 * @author Derrick Eberlein
 */
@Setter
@Getter
public class Greetings {

    private long guild;

    @BsonProperty("welcome_channel")
    private Long welcomeChannel;

    private String Greeting;

    private String Farewell;

    @BsonProperty("join_dm")
    private String joinDM;

    /** No-argument constructor required by MongoDB POJO codec */
    public Greetings() {
        // Initialize fields with default values if needed
        this.welcomeChannel = null;
        this.Greeting = null;
        this.Farewell = null;
        this.joinDM = null;
    }

    /** Constructor that accepts guild ID */
    public Greetings(long guild) {
        this.guild = guild;
        this.welcomeChannel = null;
        this.Greeting = null;
        this.Farewell = null;
        this.joinDM = null;
    }
}
