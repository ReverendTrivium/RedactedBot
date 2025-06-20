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


    public String getCreatorTag() {
        return creatorUsername != null ? creatorUsername : String.valueOf(userId);
    }

    public int getTicketNumber() {
        return ticketId;
    }

    public void setClosed(boolean closed) {
        this.status = closed ? "closed" : "open";
    }

    // Optional: Convenience method
    public boolean isClosed() {
        return "closed".equalsIgnoreCase(status);
    }
}
