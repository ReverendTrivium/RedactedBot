package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

/**
 * Economy Class
 * This class represents the economy data for a guild and optionally for a user.
 * It includes fields for guild ID, user ID, balance, bank, and an inventory of items.
 *
 * @author Derrick Eberlein
 */
@Setter
@Getter
public class Economy {
    private long guild;
    private Long user;
    private Long balance;
    private Long bank;
    private LinkedHashMap<String, Long> inventory;

    /**
     * Default constructor initializes the economy data.
     */
    public Economy() { }

    /**
     * Constructs an Economy instance for a specific guild.
     *
     * @param guild the ID of the guild
     */
    public Economy(long guild) {
        this.guild = guild;
    }
}
