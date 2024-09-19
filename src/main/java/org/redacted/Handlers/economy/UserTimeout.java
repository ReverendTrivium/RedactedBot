package org.redacted.Handlers.economy;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents timeout timestamps for economy backend.
 *
 * @author Derrick Eberlein
 */
@Getter
@Setter
public class UserTimeout {

    private final long user;
    private Long workTimeout;
    private Long crimeTimeout;
    private Long robTimeout;

    public UserTimeout(long user) {
        this.user = user;
    }
}
