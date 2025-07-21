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
 * Database Class
 * This class handles the connection to the MongoDB database and provides methods to interact with various collections.
 * It initializes collections for guild-specific data and provides methods for global operations like storing Reddit tokens.
 *
 * @author Derrick Eberlein
 */
public class Database {

    private final MongoDatabase database;
    public @NotNull MongoCollection<Config> config;
    public @NotNull MongoCollection<Document> redditTokenCollection;

    /**
     * Constructor for Database class.
     * Initializes the MongoDB connection and sets up the necessary collections.
     *
     * @param uri The MongoDB connection URI.
     */
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

    /**
     * Get a guild-specific collection by guild ID and collection name.
     *
     * @param guildId The ID of the guild.
     * @param collectionName The name of the collection.
     * @return The MongoCollection for the specified guild and collection.
     */
    public MongoCollection<Document> getGuildCollection(long guildId, String collectionName) {
        return database.getCollection("guild_" + guildId + "_" + collectionName);
    }

    /**
     * Setup collections for a specific guild.
     * This method initializes various collections for a guild, such as blacklist, scheduled messages, sticky messages,
     * user intro messages, greetings, economy, saved embeds, tickets, and config.
     *
     * @param guildId The ID of the guild for which to set up collections.
     */
    public void setupCollectionsForGuild(long guildId) {
        MongoCollection<Document> blacklist = getGuildCollection(guildId, "blacklist");
        MongoCollection<Document> scheduledMessages = getGuildCollection(guildId, "scheduled_messages");
        MongoCollection<Document> stickyMessages = getGuildCollection(guildId, "sticky_messages");
        MongoCollection<Document> userIntroMessages = getGuildCollection(guildId, "user_intro_messages");
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        MongoCollection<Document> economy = getGuildCollection(guildId, "economy");

        // Create a collection for saved embeds
        MongoCollection<Document> savedEmbeds = getGuildCollection(guildId, "saved_embeds");

        // ✅ Add ticket collection
        MongoCollection<Document> tickets = getGuildCollection(guildId, "tickets");

        // Add config collection for guild
        MongoCollection<Document> config = getGuildCollection(guildId, "config");

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

        // Set up index for config collection if necessary
        config.createIndex(Indexes.descending("guildId"));

        // ✅ Add useful indexes for ticket lookup and status filtering
        tickets.createIndex(Indexes.ascending("ticketId"));
        tickets.createIndex(Indexes.ascending("status"));
    }

    /**
     * Global Methods for entire bot to function
     *
     * Store a Reddit token (global)
     * This method stores a Reddit token along with its expiration time in the database.
     *
     * @param token The Reddit token to store.
     * @param expiration The expiration time of the token.
     */
    public void storeRedditToken(String token, Instant expiration) {
        Document document = new Document("token", token)
                .append("expiration", expiration);
        redditTokenCollection.insertOne(document);
    }

    /**
     * Get the Reddit token (global)
     * This method retrieves the first Reddit token stored in the database.
     *
     * @return The first Reddit token document, or null if no token is found.
     */
    public Document getRedditToken() {
        return redditTokenCollection.find().first();
    }

    /**
     * Clear the Reddit token (global)
     * This method deletes all Reddit tokens stored in the database.
     */
    public void clearRedditToken() {
        redditTokenCollection.deleteMany(new Document());
    }

    /**
     * Get the configuration for a specific guild.
     * This method retrieves the configuration settings for a guild from the database.
     *
     * @param guildId The ID of the guild for which to retrieve the configuration.
     * @return The Config object containing the guild's configuration, or null if not found.
     */
    public Config getConfigForGuild(long guildId) {
        MongoCollection<Document> configCollection = getGuildCollection(guildId, "config");

        System.out.println("Querying config for guild ID: " + guildId);
        Document configDocument = configCollection.find(Filters.eq("guildId", guildId)).first();

        if (configDocument == null) {
            System.out.println("No config found for guild ID: " + guildId);
            return null;
        }

        // Convert Document to Config object
        Config config = new Config();
        config.setGuildId(configDocument.getLong("guildId"));
        config.setCurrency(configDocument.getString("currency"));
        config.setPrefix(configDocument.getString("prefix"));
        // Initialize other fields as necessary

        return config;
    }

    /**
     * Get the collection for calendar events for a specific guild.
     * This method retrieves the MongoDB collection for calendar events associated with a guild.
     *
     * @param guildId The ID of the guild for which to retrieve the calendar events collection.
     * @return The MongoCollection for calendar events, with the document class set to Document.
     */
    public MongoCollection<Document> getCalendarEventCollection(long guildId) {
        return getGuildCollection(guildId, "calendar_events");
    }

    /**
     * Get the collection for saved embeds for a specific guild.
     * This method retrieves the MongoDB collection for saved embeds associated with a guild.
     *
     * @param guildId The ID of the guild for which to retrieve the saved embeds collection.
     * @return The MongoCollection for saved embeds, with the document class set to SavedEmbed.
     */
    public MongoCollection<SavedEmbed> getSavedEmbedsCollection(long guildId) {
        return getGuildCollection(guildId, "saved_embeds").withDocumentClass(SavedEmbed.class);
    }

    /**
     * Update the configuration for a specific guild.
     * This method updates the configuration settings for a guild in the database.
     *
     * @param guildId The ID of the guild for which to update the configuration.
     * @param update The Bson object containing the update operations to apply.
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

    /**
     * Insert a new configuration for a specific guild.
     * This method inserts a new configuration document into the database for a guild.
     *
     * @param guildId The ID of the guild for which to insert the configuration.
     * @param config The Config object containing the configuration settings to insert.
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
     * Update the configuration for a specific guild using a Bson update object.
     * This method applies an update operation to the configuration document for a guild.
     *
     * @param guildId The ID of the guild for which to update the configuration.
     * @param update The Bson object containing the update operations to apply.
     */
    public void updateConfigForGuild(long guildId, Bson update) {
        Bson filter = Filters.eq("guildId", guildId);
        config.updateOne(filter, update);
    }

    /**
     * Get the collection for scheduled messages for a specific guild.
     * This method retrieves the MongoDB collection for scheduled messages associated with a guild.
     *
     * @param guildId The ID of the guild for which to retrieve the scheduled messages collection.
     * @return The MongoCollection for scheduled messages, with the document class set to Document.
     */
    public List<Document> getScheduledMessages(long guildId) {
        MongoCollection<Document> scheduledMessages = getGuildCollection(guildId, "scheduled_messages");
        return scheduledMessages.find().into(new ArrayList<>());
    }

    /**
     * Initialize greetings for a specific guild.
     * This method checks if greetings exist for the guild and creates a new Greetings object if not.
     *
     * @param guildId The ID of the guild for which to initialize greetings.
     */
    public void initializeGreetingsForGuild(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        if (getGreetings(guildId) == null) {
            Greetings greetingsObj = new Greetings(guildId);
            greetings.insertOne(greetingsObj);
        }
    }

    /**
     * Get the greetings for a specific guild.
     * This method retrieves the Greetings object for a guild from the database.
     *
     * @param guildId The ID of the guild for which to retrieve greetings.
     * @return The Greetings object containing the greeting and farewell messages, or null if not found.
     */
    public Greetings getGreetings(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        return greetings.find(new Document("guild", guildId)).first();
    }

    /**
     * Get the configuration collection.
     * This method retrieves the MongoDB collection for global configuration settings.
     *
     * @return The MongoCollection for global configuration, with the document class set to Config.
     */
    public MongoCollection<Config> getConfigCollection() {
        return config;
    }

    /**
     * Get the collection for tickets for a specific guild.
     * This method retrieves the MongoDB collection for tickets associated with a guild.
     *
     * @param guildId The ID of the guild for which to retrieve the ticket collection.
     * @return The MongoCollection for tickets, with the document class set to Ticket.
     */
    public MongoCollection<Ticket> getTicketCollection(long guildId) {
        return getGuildCollection(guildId, "tickets").withDocumentClass(Ticket.class);
    }
}
