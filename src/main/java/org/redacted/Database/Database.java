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

        // Create a collection for saved embeds
        MongoCollection<Document> savedEmbeds = getGuildCollection(guildId, "saved_embeds");

        // Add config collection for guild
        MongoCollection<Document> config = getGuildCollection(guildId, "config");

        // Setup indexes for each collection as needed
        blacklist.createIndex(Indexes.descending("userId"));
        scheduledMessages.createIndex(Indexes.descending("time"));
        stickyMessages.createIndex(Indexes.descending("channelId"));
        userIntroMessages.createIndex(Indexes.descending("userId"));
        greetings.createIndex(Indexes.descending("guild"));
        economy.createIndex(Indexes.descending("economy"));
        savedEmbeds.createIndex(Indexes.descending("messageId"));

        // Set up index for config collection if necessary
        config.createIndex(Indexes.descending("guildId"));
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

    public MongoCollection<SavedEmbed> getSavedEmbedsCollection(long guildId) {
        return getGuildCollection(guildId, "saved_embeds").withDocumentClass(SavedEmbed.class);
    }

    /**
     * Method to update a previous Config object in the database
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
     * Method to insert a new Config object into the database
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

    // Method to update the configuration for a specific guild
    public void updateConfigForGuild(long guildId, Bson update) {
        Bson filter = Filters.eq("guildId", guildId);
        config.updateOne(filter, update);
    }

    public List<Document> getScheduledMessages(long guildId) {
        MongoCollection<Document> scheduledMessages = getGuildCollection(guildId, "scheduled_messages");
        return scheduledMessages.find().into(new ArrayList<>());
    }

    // Greetings methods
    public void initializeGreetingsForGuild(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        if (getGreetings(guildId) == null) {
            Greetings greetingsObj = new Greetings(guildId);
            greetings.insertOne(greetingsObj);
        }
    }

    public Greetings getGreetings(long guildId) {
        MongoCollection<Greetings> greetings = getGuildCollection(guildId, "greetings").withDocumentClass(Greetings.class);
        return greetings.find(new Document("guild", guildId)).first();
    }

    public MongoCollection<Config> getConfigCollection() {
        return config;
    }
}
