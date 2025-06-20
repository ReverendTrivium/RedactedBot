package org.redacted.Handlers.economy;

import lombok.Getter;
import lombok.Setter;

/**
 * UserTimeout Class
 * This class represents the timeouts for various user activities in the economy system.
 * It tracks the timeouts for work, crime, and rob actions for a specific user.
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

    /**
     * Constructs a UserTimeout with the specified user ID.
     *
     * @param user The ID of the user for whom the timeout is being created.
     */
    public UserTimeout(long user) {
        this.user = user;
    }
}
