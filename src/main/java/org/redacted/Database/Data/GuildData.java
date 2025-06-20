package org.redacted.Database.Data;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Database;
import org.redacted.Database.cache.Config;
import org.redacted.Database.cache.Economy;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Handlers.GreetingHandler;
import org.redacted.Handlers.SuggestionHandler;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Redacted;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class GuildData {

    private static final Map<Long, GuildData> guilds = new HashMap<>();
    @Getter
    private static Database database;
    private Config config;

    private final long guildId;
    private final SuggestionHandler suggestionHandler;
    private final GreetingHandler greetingHandler;
    // Method to retrieve the EconomyHandler
    @Getter
    private final EconomyHandler economyHandler;

    public GuildData(Guild guild, Redacted bot) {
        this.guildId = guild.getIdLong();

        // Ensure collections are set up for the guild
        database.setupCollectionsForGuild(guildId);

        // Retrieve the configuration for the guild
        this.config = database.getConfigForGuild(guildId);
        if (this.config == null) {
            System.out.println("Config for guild " + guildId + " is not found, creating a new one.");

            // Create default config for the guild with required values
            this.config = new Config(guildId);
            this.config.setCurrency(EconomyHandler.DEFAULT_CURRENCY);  // Set default currency, for example

            // Insert the default config into the database
            database.insertConfig(guildId, this.config);
            System.out.println("Inserted default configuration for guild: " + guild.getName());

            // Retrieve the config again after insertion to ensure it is properly inserted
            this.config = database.getConfigForGuild(guildId);
            if (this.config == null) {
                throw new IllegalStateException("Failed to create or retrieve config for guild " + guildId);
            }
        }

        // Ensure critical fields like 'currency' are initialized
        if (this.config.getCurrency() == null) {
            this.config.setCurrency(EconomyHandler.DEFAULT_CURRENCY);
            database.updateConfig(guildId, Updates.set("currency", EconomyHandler.DEFAULT_CURRENCY));
            System.out.println("Currency initialized to default for guild: " + guild.getIdLong());
        }

        // Now that config is initialized, initialize the EconomyHandler
        this.economyHandler = new EconomyHandler(guild, this);

        // Initialize other handlers with the correct parameters
        this.suggestionHandler = new SuggestionHandler(guild, bot, database);
        this.greetingHandler = new GreetingHandler(guild, database);
    }


    public static void init(Database db) {
        database = db;
    }

    public static GuildData get(@NotNull Guild guild, Redacted bot) {
        return guilds.computeIfAbsent(Objects.requireNonNull(guild).getIdLong(), id -> new GuildData(guild, bot)); // Pass bot instance here
    }

    public MongoCollection<Document> getBlacklistCollection() {
        return database.getGuildCollection(guildId, "blacklist");
    }

    public MongoCollection<Document> getScheduledMessagesCollection() {
        return database.getGuildCollection(guildId, "scheduled_messages");
    }

    public MongoCollection<Document> getStickyMessagesCollection() {
        return database.getGuildCollection(guildId, "sticky_messages");
    }

    public MongoCollection<Document> getUserIntroMessagesCollection() {
        return database.getGuildCollection(guildId, "user_intro_messages");
    }

    public String getGuildId() {
        return String.valueOf(guildId);
    }

    public MongoCollection<Economy> getEconomyCollection() {
        // Use withDocumentClass to get the correct type of Economy instead of Document.
        return database.getGuildCollection(guildId, "economy").withDocumentClass(Economy.class);
    }

    public Config getConfig() {
        // Fetch the config for this guild
        Config fetchedConfig = database.getConfigCollection().find(new Document("guildId", guildId)).first();

        // If the config is still null, throw an exception to catch the error early
        if (fetchedConfig == null) {
            throw new IllegalStateException("Config for guild " + guildId + " could not be found or created.");
        }
        return fetchedConfig;
    }


    public void updateConfig(Bson update) {
        database.updateConfigForGuild(guildId, update);
        // Also update the in-memory config object if necessary
        this.config = database.getConfigForGuild(guildId);
    }

    public MongoCollection<SavedEmbed> getSavedEmbedsCollection() {
        return database.getGuildCollection(guildId, "saved_embeds")
                .withDocumentClass(SavedEmbed.class);
    }

}
