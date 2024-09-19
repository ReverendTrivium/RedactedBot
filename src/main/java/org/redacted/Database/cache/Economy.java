package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

/**
 * POJO object that stores server economy data for a user.
 */
@Setter
@Getter
public class Economy {
    private long guild;
    private Long user;
    private Long balance;
    private Long bank;
    private LinkedHashMap<String, Long> inventory;

    public Economy() { }

    public Economy(long guild) {
        this.guild = guild;
    }
}
