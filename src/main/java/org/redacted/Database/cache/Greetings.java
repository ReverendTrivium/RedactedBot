package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Greetings Class
 * This class represents the greeting and farewell messages for a Discord guild.
 * It includes fields for the guild ID, welcome channel, greeting message, farewell message,
 * and a direct message sent to users upon joining.
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

    /**
     * Default constructor initializes the greetings object with default values.
     * This is useful for creating a new instance without any specific guild ID.
     */
    public Greetings() {
        // Initialize fields with default values if needed
        this.welcomeChannel = null;
        this.Greeting = null;
        this.Farewell = null;
        this.joinDM = null;
    }

    /**
     * Constructs a Greetings instance for a specific guild.
     *
     * @param guild the ID of the guild
     */
    public Greetings(long guild) {
        this.guild = guild;
        this.welcomeChannel = null;
        this.Greeting = null;
        this.Farewell = null;
        this.joinDM = null;
    }
}
