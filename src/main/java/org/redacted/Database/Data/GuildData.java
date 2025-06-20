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

/**
 * GuildData Class
 * This class represents data and handlers for a specific Discord guild.
 * It manages configurations, suggestion handling, greeting handling, and economy handling.
 *
 * @author Derrick Eberlein
 */
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

    /**
     * Constructs a GuildData instance for the specified guild.
     * It initializes the guild ID, retrieves or creates the configuration,
     * and sets up the necessary handlers for suggestions, greetings, and economy.
     *
     * @param guild The guild for which this data is being created.
     * @param bot   The Redacted bot instance to pass to handlers
     */
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

    /**
     * Initializes the static database instance.
     * This method should be called once at the start of the application
     * to set up the database connection.
     *
     * @param db The Database instance to be used for all guild data operations.
     */
    public static void init(Database db) {
        database = db;
    }

    /**
     * Retrieves or creates a GuildData instance for the specified guild.
     * If the guild data already exists, it returns the existing instance.
     * Otherwise, it creates a new instance and stores it in the static map.
     *
     * @param guild The guild for which to retrieve or create GuildData.
     * @param bot   The Redacted bot instance to pass to handlers
     * @return The GuildData instance for the specified guild.
     */
    public static GuildData get(@NotNull Guild guild, Redacted bot) {
        return guilds.computeIfAbsent(Objects.requireNonNull(guild).getIdLong(), id -> new GuildData(guild, bot)); // Pass bot instance here
    }

    /**
     * Retrieves the MongoDB collection for the blacklist of this guild.
     * This collection is used to store blacklisted users or entities for the guild.
     *
     * @return The MongoCollection<Document> representing the blacklist for this guild.
     */
    public MongoCollection<Document> getBlacklistCollection() {
        return database.getGuildCollection(guildId, "blacklist");
    }

    /**
     * Retrieves the MongoDB collection for scheduled messages of this guild.
     * This collection is used to store messages that are scheduled to be sent at a later time.
     *
     * @return The MongoCollection<Document> representing the scheduled messages for this guild.
     */
    public MongoCollection<Document> getScheduledMessagesCollection() {
        return database.getGuildCollection(guildId, "scheduled_messages");
    }

    /**
     * Retrieves the MongoDB collection for sticky messages of this guild.
     * This collection is used to store messages that should remain pinned or highlighted in a channel.
     *
     * @return The MongoCollection<Document> representing the sticky messages for this guild.
     */
    public MongoCollection<Document> getStickyMessagesCollection() {
        return database.getGuildCollection(guildId, "sticky_messages");
    }

    /**
     * Retrieves the MongoDB collection for user introduction messages of this guild.
     * This collection is used to store messages that introduce users to the guild.
     *
     * @return The MongoCollection<Document> representing the user introduction messages for this guild.
     */
    public MongoCollection<Document> getUserIntroMessagesCollection() {
        return database.getGuildCollection(guildId, "user_intro_messages");
    }

    /**
     * Retrieves the guild ID as a String.
     * This method is useful for logging or displaying the guild ID in a user-friendly format.
     *
     * @return The guild ID as a String.
     */
    public String getGuildId() {
        return String.valueOf(guildId);
    }

    /**
     * Retrieves the MongoDB collection for economy data of this guild.
     * This collection is used to store economy-related information such as user balances, transactions, etc.
     *
     * @return The MongoCollection<Economy> representing the economy data for this guild.
     */
    public MongoCollection<Economy> getEconomyCollection() {
        // Use withDocumentClass to get the correct type of Economy instead of Document.
        return database.getGuildCollection(guildId, "economy").withDocumentClass(Economy.class);
    }

    /**
     * Retrieves the configuration for this guild.
     * If the configuration does not exist, it will throw an exception to catch the error early.
     *
     * @return The Config object for this guild.
     */
    public Config getConfig() {
        // Fetch the config for this guild
        Config fetchedConfig = database.getConfigCollection().find(new Document("guildId", guildId)).first();

        // If the config is still null, throw an exception to catch the error early
        if (fetchedConfig == null) {
            throw new IllegalStateException("Config for guild " + guildId + " could not be found or created.");
        }
        return fetchedConfig;
    }

    /**
     * Updates the configuration for this guild with the provided update.
     * This method updates the database and also refreshes the in-memory config object.
     *
     * @param update The Bson update to apply to the guild's configuration.
     */
    public void updateConfig(Bson update) {
        database.updateConfigForGuild(guildId, update);
        // Also update the in-memory config object if necessary
        this.config = database.getConfigForGuild(guildId);
    }

    /**
     * Retrieves the MongoDB collection for saved embeds of this guild.
     * This collection is used to store embeds that can be reused or referenced later.
     *
     * @return The MongoCollection<SavedEmbed> representing the saved embeds for this guild.
     */
    public MongoCollection<SavedEmbed> getSavedEmbedsCollection() {
        return database.getGuildCollection(guildId, "saved_embeds")
                .withDocumentClass(SavedEmbed.class);
    }

}
