package org.redacted.Handlers.economy;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a reply to an economy command.
 * Used in embeds to display information about the command run.
 *
 * @author Derrick Eberlein
 */
@Getter
@Setter
public class EconomyReply {

    private final String response;
    private final int id;
    private final boolean isSuccess;

    /**
     * Constructs an EconomyReply with the provided response and ID.
     *
     * @param response The response message.
     * @param id       The ID of the reply.
     */
    public EconomyReply(String response, int id) {
        this.response = response;
        this.id = id;
        this.isSuccess = true;
    }

    /**
     * Constructs an EconomyReply with the provided response, ID, and success status.
     *
     * @param response  The response message.
     * @param id        The ID of the reply.
     * @param isSuccess Indicates if the reply is successful.
     */
    public EconomyReply(String response, int id, boolean isSuccess) {
        this.response = response;
        this.id = id;
        this.isSuccess = isSuccess;
    }

    /**
     * Checks if the reply is successful.
     *
     * @return true if the reply is successful, false otherwise.
     */
    public boolean isSuccess() {
        return isSuccess;
    }
}
