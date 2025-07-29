package org.redacted.Database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.cache.Config;
import org.redacted.Database.cache.Greetings;
import org.redacted.Database.models.SavedEmbed;
import org.redacted.Database.models.Ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Database for the Redacted Bot
 *
 * @author Derrick Eberlein
 */
public class Database {
    private final MongoDatabase database;
    public @NotNull MongoCollection<Config> config;
    public @NotNull MongoCollection<Document> redditTokenCollection;


    public Database(String uri) {
        // Setup MongoDB database with URI.
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .codecRegistry(codecRegistry)
                .build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        this.database = mongoClient.getDatabase("Redacted");

        // Initialize collections if they don't exist.
        config = database.getCollection("config", Config.class);
        redditTokenCollection = database.getCollection("reddit_tokens");

        // Create indexes
        Bson guildIndex = Indexes.descending("guild");
        config.createIndex(guildIndex);
    }

    // Method to get a guild-specific collection
    public MongoCollection<Document> getGuildCollection(long guildId, String collectionName) {
        return database.getCollection("guild_" + guildId + "_" + collectionName);
    }

    // Initialize collections for a new guild
    public void setupCollectionsForGuild(long guildId) {
        MongoCollection<Document> blacklist = getGuildCollection(guildId, "blacklist");
        MongoCollection<Document> scheduledMessages = getGuildCollection(guildId, "scheduled_messages");
        MongoCollection<Document> stickyMessages = getGuildCollection(guildId, "sticky_messages");
        MongoCollection<Document> userIntroMessages = getGuildCollection(guildId, "user_intro_messages");
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        MongoCollection<Document> economy = getGuildCollection(guildId, "economy");

        // Create a NSFW Clean Toggle
        MongoCollection<Document> nsfwCleanToggle = getGuildCollection(guildId, "nsfwCleanToggle");

        // Create a collection for saved embeds
        MongoCollection<Document> savedEmbeds = getGuildCollection(guildId, "saved_embeds");

        // ✅ Add ticket collection
        MongoCollection<Document> tickets = getGuildCollection(guildId, "tickets");

        // Add a config collection for guild
        MongoCollection<Document> config = getGuildCollection(guildId, "config");

        // Add a Muted Users collection
        MongoCollection<Document> mutes = getGuildCollection(guildId, "mutes");

        // Add Google Calendar events collection
        MongoCollection<Document> calendarEvents = getGuildCollection(guildId, "calendar_events");

        // Setup indexes for each collection as needed
        blacklist.createIndex(Indexes.descending("userId"));
        scheduledMessages.createIndex(Indexes.descending("time"));
        stickyMessages.createIndex(Indexes.descending("channelId"));
        userIntroMessages.createIndex(Indexes.descending("userId"));
        greetings.createIndex(Indexes.descending("guild"));
        economy.createIndex(Indexes.descending("economy"));
        savedEmbeds.createIndex(Indexes.descending("messageId"));
        calendarEvents.createIndex(Indexes.descending("discordEventId"));
        mutes.createIndex(Indexes.ascending("guildId", "userId"));
        nsfwCleanToggle.createIndex(Indexes.ascending("guildId", "nsfwCleanToggle"));

        // Set up index for config collection if necessary
        config.createIndex(Indexes.descending("guildId"));

        // ✅ Add useful indexes for ticket lookup and status filtering
        tickets.createIndex(Indexes.ascending("ticketId"));
        tickets.createIndex(Indexes.ascending("status"));
    }

    /**
     * Global Methods for entire bot to function
     */
    // Store a Reddit token (global)
    public void storeRedditToken(String token, Instant expiration) {
        Document document = new Document("token", token)
                .append("expiration", expiration);
        redditTokenCollection.insertOne(document);
    }

    // Get a Reddit token (global)
    public Document getRedditToken() {
        return redditTokenCollection.find().first();
    }

    // Clear Reddit token (global)
    public void clearRedditToken() {
        redditTokenCollection.deleteMany(new Document());
    }

    /**
     * Guild-specific methods
     */
    public MongoCollection<Document> getCalendarEventCollection(long guildId) {
        return getGuildCollection(guildId, "calendar_events");
    }

    /**
     * Get the configuration for a specific guild.
     *
     * @param guildId The ID of the guild.
     * @return The Config object for the guild, or null if not found.
     */
    public Config getConfigForGuild(long guildId) {
        MongoCollection<Document> configCollection = getGuildCollection(guildId, "config");

        System.out.println("Querying config for guild ID: " + guildId);
        Document configDocument = configCollection.find(Filters.eq("guildId", guildId)).first();

        if (configDocument == null) {
            System.out.println("No config found for guild ID: " + guildId);
            return null;
        }

        // Convert Document to a Config object
        Config config = new Config();
        config.setGuildId(configDocument.getLong("guildId"));
        config.setCurrency(configDocument.getString("currency"));
        config.setPrefix(configDocument.getString("prefix"));
        // Initialize other fields as necessary

        return config;
    }

    /**
     * Get the collection for storing saved embeds in a guild.
     *
     * @param guildId The ID of the guild.
     * @return The MongoCollection for saved embeds.
     */
    public MongoCollection<SavedEmbed> getSavedEmbedsCollection(long guildId) {
        return getGuildCollection(guildId, "saved_embeds").withDocumentClass(SavedEmbed.class);
    }

    /**
     * Update the configuration for a specific guild.
     * This method updates the config document for the specified guild using the provided update operation.
     *
     * @param guildId The ID of the guild whose config is to be updated.
     * @param update  The Bson update operation to apply to the config document.
     */
    public void updateConfig(long guildId, Bson update) {
        // Get the collection where the config for the guild is stored
        MongoCollection<Document> configCollection = getGuildCollection(guildId, "config");

        // Create a filter to find the config document for the specified guild
        Bson filter = Filters.eq("guildId", guildId);

        // Perform the update
        configCollection.updateOne(filter, update);
        System.out.println("Updated config for guild: " + guildId);
    }

    /** Insert a new configuration for a specific guild.
     * This method creates a new config document and inserts it into the database.
     *
     * @param guildId The ID of the guild for which to insert the config.
     * @param config  The Config object containing the configuration details.
     */
    public void insertConfig(long guildId, Config config) {
        MongoCollection<Document> configCollection = getGuildCollection(guildId, "config");

        Document configDocument = new Document("guildId", config.getGuildId())
                .append("currency", config.getCurrency())
                .append("prefix", config.getPrefix());
        // Add other fields as needed

        configCollection.insertOne(configDocument);
        System.out.println("Config inserted for guild: " + guildId);
    }

    /**
     * Update the configuration for a specific guild using a Bson update operation.
     * This method applies the provided update operation to the config document for the specified guild.
     *
     * @param guildId The ID of the guild whose config is to be updated.
     * @param update  The Bson update operation to apply to the config document.
     */
    public void updateConfigForGuild(long guildId, Bson update) {
        Bson filter = Filters.eq("guildId", guildId);
        config.updateOne(filter, update);
    }

    /**
     * Get all scheduled messages for a guild.
     * This method retrieves all scheduled messages from the database for a specific guild.
     *
     * @param guildId The ID of the guild for which to retrieve scheduled messages.
     * @return A list of Document objects representing the scheduled messages.
     */
    public List<Document> getScheduledMessages(long guildId) {
        MongoCollection<Document> scheduledMessages = getGuildCollection(guildId, "scheduled_messages");
        return scheduledMessages.find().into(new ArrayList<>());
    }

    /**
     * Initialize greetings for a guild.
     * This method checks if greetings are already set up for the guild and initializes them if not.
     *
     * @param guildId The ID of the guild to initialize greetings for.
     */
    public void initializeGreetingsForGuild(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        if (getGreetings(guildId) == null) {
            Greetings greetingsObj = new Greetings(guildId);
            greetings.insertOne(greetingsObj);
        }
    }

    /**
     * Get the greetings configuration for a specific guild.
     * This method retrieves the greetings settings for the specified guild.
     *
     * @param guildId The ID of the guild to retrieve greetings for.
     * @return The Greetings object containing the greetings configuration, or null if not found.
     */
    public Greetings getGreetings(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        return greetings.find(new Document("guild", guildId)).first();
    }

    /**
     * Get the configuration collection for the bot.
     * This method retrieves the global configuration collection used by the bot.
     *
     * @return The MongoCollection for global configuration.
     */
    public MongoCollection<Config> getConfigCollection() {
        return config;
    }

    /**
     * Get the collection for managing tickets in a guild.
     * This method retrieves the MongoDB collection for tickets associated with a specific guild.
     *
     * @param guildId The ID of the guild for which to retrieve the ticket collection.
     * @return The MongoCollection for tickets in the specified guild.
     */
    public MongoCollection<Ticket> getTicketCollection(long guildId) {
        return getGuildCollection(guildId, "tickets").withDocumentClass(Ticket.class);
    }
}
