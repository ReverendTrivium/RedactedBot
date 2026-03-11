package org.redacted.Database.models;

import lombok.Data;
import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * Ticket Class
 * This class represents a support ticket in the database.
 * It contains fields for ticket ID, reason, channel ID, guild ID, user ID,
 * creator username, status, opened and closed timestamps, and close reason.
 *
 * @author Derrick Eberlein
 */
@Data
@Getter
public class Ticket {

    @BsonProperty("ticketId")
    private int ticketId;

    @BsonProperty("reason")
    private String reason;

    @BsonProperty("channelId")
    private String channelId;

    @BsonProperty("guildId")
    private long guildId;

    @BsonProperty("userId")
    private long userId;

    @BsonProperty("creatorUsername")
    private String creatorUsername; // <-- New field

    @BsonProperty("status")
    private String status; // "open", "closed"

    @BsonProperty("openedAt")
    private Instant openedAt;

    @BsonProperty("closedAt")
    private Instant closedAt;

    @BsonProperty("closeReason")
    private String closeReason;

    /**
     * Returns the creator's tag (username#discriminator) if available,
     * otherwise returns the user ID as a string.
     *
     * @return The creator's tag or user ID as a string.
     */
    public String getCreatorTag() {
        return creatorUsername != null ? creatorUsername : String.valueOf(userId);
    }

    /**
     * Returns the ticket number (ticket ID). This is a unique identifier for the ticket.
     *
     * @return The ticket number (ticket ID).
     */
    public int getTicketNumber() {
        return ticketId;
    }

    /**
     * Sets the status of the ticket to "closed" or "open" based on the provided boolean value.
     *
     * @param closed If true, sets the status to "closed"; if false, sets it to "open".
     */
    public void setClosed(boolean closed) {
        this.status = closed ? "closed" : "open";
    }

    /**
     * Checks if the ticket is currently closed.
     * @return True if the ticket status is "closed", false otherwise.
     */
    public boolean isClosed() {
        return "closed".equalsIgnoreCase(status);
    }
}
