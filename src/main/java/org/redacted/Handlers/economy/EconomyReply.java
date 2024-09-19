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

    public EconomyReply(String response, int id) {
        this.response = response;
        this.id = id;
        this.isSuccess = true;
    }

    public EconomyReply(String response, int id, boolean isSuccess) {
        this.response = response;
        this.id = id;
        this.isSuccess = isSuccess;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
