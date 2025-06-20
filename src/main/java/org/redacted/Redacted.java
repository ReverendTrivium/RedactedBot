package org.redacted;

import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.OkHttpClient;
import org.redacted.Commands.BotCommands;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.Database;
import org.redacted.RedactedStartup.BotInitializer;
import org.redacted.RedactedStartup.SchedulerManager;
import org.redacted.RedactedStartup.ShardReadyListener;
import org.redacted.listeners.MessageSchedulerListener;
import org.redacted.util.GalleryManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class Redacted {
    public Gson gson;
    public OkHttpClient httpClient;
    public final Dotenv config;
    public final ShardManager shardManager;
    public final Database database;
    public final GalleryManager galleryManager;
    public final ScheduledExecutorService scheduler;
    private final BotCommands botCommands;

    @Getter
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // or CachedThreadPool

    /**
     * Main class for the Redacted bot.
     *
     * @throws LoginException if the bot token is invalid
     */
    public Redacted() throws LoginException {
        // Load configuration
        config = Dotenv.configure().ignoreIfMissing().load();

        // Initialize components
        gson = new Gson();
        httpClient = new OkHttpClient();

        // Initialize the database
        database = new Database(config.get("DATABASE"));

        // Initialize Guild Data
        System.out.println("Initializing GuildData...");
        GuildData.init(database); // Pass the database instance here
        System.out.println("GuildData initialized");

        galleryManager = new GalleryManager();
        shardManager = BotInitializer.initializeBot(config.get("TOKEN"));
        scheduler = SchedulerManager.initializeScheduler(shardManager, this);

        // Initialize bot commands here
        botCommands = new BotCommands(this);

        // Create the MessageSchedulerListener for general use
        MessageSchedulerListener schedulerListener = new MessageSchedulerListener(this, database);

        // Add the ShardReadyListener to load and reschedule messages after all shards are ready
        shardManager.addEventListener(new ShardReadyListener(this, schedulerListener));

        // Register other listeners
        BotInitializer.registerListeners(shardManager, this);
    }

    public static void main(String[] args) {
        try {
            java.util.logging.Logger.getLogger("org.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            new Redacted();
            System.out.println("Bot started successfully");
        } catch (LoginException e) {
            System.err.println("ERROR: Provided bot token is invalid!!");
        }
    }

    // Add shutdown hook if needed
    public void shutdown() {
        threadPool.shutdown();
    }
}