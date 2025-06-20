package org.redacted.Database.cache;

import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.redacted.Handlers.economy.EconomyHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * POJO object that stores config data for a guild.
 *
 * @author Derrick Eberlein
 */
@Getter
@Setter
public class Config {

    @BsonProperty("guildId")
    private long guildId;

    private String prefix;

    private Long premium;

    @BsonProperty("leveling_channel")
    private Long levelingChannel;

    @BsonProperty("leveling_message")
    private String levelingMessage;

    @BsonProperty("leveling_dm")
    private boolean levelingDM;

    @BsonProperty("leveling_mod")
    private int levelingMod;

    @BsonProperty("leveling_mute")
    private boolean levelingMute;

    @BsonProperty("leveling_background")
    private String levelingBackground;

    private Map<String, Integer> rewards;

    @BsonProperty("auto_roles")
    private Set<Long> autoRoles;

    private String currency;

    private LinkedHashMap<String, String> shop; // Maps item names to ids

    /**
     * Default constructor initializes the autoRoles set and shop map.
     */
    public Config() {
        this.autoRoles = new HashSet<>();
        this.shop = new LinkedHashMap<>();
    }

    /**
     * Constructor that initializes the guild ID and sets default values for other fields.
     *
     * @param guild the guild ID
     */
    public Config(long guild) {
        this.guildId = guild;
        this.premium = null;
        this.prefix = null;
        this.currency = EconomyHandler.DEFAULT_CURRENCY;  // Ensure default currency is set
    }

    /**
     * Adds an auto role to the set of auto roles.
     *
     * @param roleID the ID of the role to add
     */
    public void addAutoRole(long roleID) {
        this.autoRoles.add(roleID);
    }

    /**
     * Removes an auto role from the set of auto roles.
     *
     * @param roleID the ID of the role to remove
     */
    public void removeAutoRole(long roleID) { this.autoRoles.remove(roleID); }
}
