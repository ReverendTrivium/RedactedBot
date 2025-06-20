package org.redacted.Handlers;

import com.mongodb.client.model.Filters;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.conversions.Bson;
import org.redacted.Database.cache.Config;
import org.redacted.Redacted;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * ConfigHandler Class
 * This class manages the configuration settings for a Discord guild.
 * It retrieves and updates the configuration from the database.
 *
 * @author Derrick Eberlein
 */
@Getter
public class ConfigHandler {

    private static final ScheduledExecutorService expireScheduler = Executors.newScheduledThreadPool(10);
    private static final Map<String, ScheduledFuture> expireTimers = new HashMap<>();
    private Config config;

    /**
     * Constructs a ConfigHandler for the specified guild.
     * It retrieves the configuration from the database or creates a new one if it doesn't exist.
     *
     * @param bot   The Redacted bot instance.
     * @param guild The guild for which the configuration is being created.
     */
    public ConfigHandler(Redacted bot, Guild guild) {

        // Get POJO object from database
        Bson filter = Filters.eq("guild", guild.getIdLong());
        this.config = bot.database.config.find(filter).first();
        if (this.config == null) {
            this.config = new Config(guild.getIdLong());
            bot.database.config.insertOne(config);
        }
    }
}
