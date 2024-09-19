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
 * Handles config data for the guild and various modules.
 *
 * @author Derrick Eberlein
 */
@Getter
@SuppressWarnings("JavadocDeclaration")
public class ConfigHandler {

    private static final ScheduledExecutorService expireScheduler = Executors.newScheduledThreadPool(10);
    private static final Map<String, ScheduledFuture> expireTimers = new HashMap<>();

    /**
     * -- GETTER --
     *  Access the config cache.
     *
     * @return a cache instance of the Config from database.
     */
    private Config config;

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
