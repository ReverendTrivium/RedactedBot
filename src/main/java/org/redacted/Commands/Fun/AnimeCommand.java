package org.redacted.Commands.Fun;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;
import org.redacted.Redacted;
import org.redacted.Commands.Command;
import org.redacted.Commands.Category;
import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.SocialMedia.Reddit.RedditClient;
import org.redacted.util.SocialMedia.Reddit.RedditOAuth;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class AnimeCommand extends Command {
    private RedditClient redditClient;
    private final RedditOAuth redditOAuth;
    private final Redacted bot;
    private static final int MAX_ATTEMPTS = 10;

    // Mapping of categories to subreddits
    private final Map<String, List<String>> categoryToSubreddits;

    public AnimeCommand(Redacted bot) {
        super(bot);
        this.bot = bot;
        System.out.println("Initializing AnimeCommand...");
        Dotenv config = bot.getConfig();

        String clientID = config.get("REDDIT_CLIENT_ID");
        String secretID = config.get("REDDIT_SECRET_ID");
        String username = config.get("REDDIT_USERNAME");
        String password = config.get("REDDIT_PASSWORD");

        // Initialize category to subreddit mapping
        categoryToSubreddits = new HashMap<>();
        categoryToSubreddits.put("anime", List.of("AnimeGirls", "Animemes", "anime", "CuteAnimeGirls", "awwnimate", "Cuteanimenekos", "cuteanimeboys", "headpats",  "pouts", "AnimeBlush", "MoeBlushing"));


        // Get Reddit API Token
        this.redditOAuth = new RedditOAuth(bot.httpClient, bot.gson);
        String token = getRedditToken(clientID, secretID, username, password);

        this.name = "anime";
        this.description = "Get a random anime image! :3";
        this.category = Category.FUN;

        // Initialize the RedditClient with your access token
        if (token != null) {
            System.out.println("Initializing RedditClient...");
            this.redditClient = new RedditClient(bot.httpClient, token);
            System.out.println("RedditClient initialized with token");
        } else {
            System.out.println("Token was null, RedditClient not initialized");
        }
    }

    private String refreshRedditToken(String clientId, String clientSecret, String username, String password) {
        System.out.println("Checking Reddit Token Status...");
        Document tokenDocument = bot.database.getRedditToken();
        if (tokenDocument != null) {
            System.out.println("Checking to see if Token is Expired...");
            Instant expiration = tokenDocument.getDate("expiration").toInstant();
            if (Instant.now().isBefore(expiration)) {
                System.out.println("Token Not Expired!!");
                return tokenDocument.getString("token");
            }
        }

        try {
            System.out.println("Setting new Reddit Token...");
            String token = redditOAuth.authenticate(clientId, clientSecret, username, password);
            Instant expiration = Instant.now().plusSeconds(24 * 60 * 60); // 24 hours
            bot.database.clearRedditToken();
            bot.database.storeRedditToken(token, expiration);
            return token;
        } catch (IOException e) {
            System.out.println("Failed to authenticate with Reddit API");
            e.printStackTrace();
        }

        return null;
    }

    private String getRedditToken(String clientId, String clientSecret, String username, String password) {
        Document tokenDocument = bot.database.getRedditToken();
        if (tokenDocument != null) {
            Instant expiration = tokenDocument.getDate("expiration").toInstant();
            if (Instant.now().isBefore(expiration)) {
                return tokenDocument.getString("token");
            }
        }

        try {
            System.out.println("Authenticating with Reddit...");
            String token = redditOAuth.authenticate(clientId, clientSecret, username, password);
            System.out.println("Reddit API Token: " + token);
            Instant expiration = Instant.now().plusSeconds(24 * 60 * 60); // 24 hours
            bot.database.clearRedditToken();
            bot.database.storeRedditToken(token, expiration);
            return token;
        } catch (IOException e) {
            System.out.println("Failed to authenticate with Reddit API");
            e.printStackTrace();
        }

        return null;
    }

    private String getRandomSubreddit(String category) {
        List<String> subreddits = categoryToSubreddits.get(category);
        if (subreddits == null || subreddits.isEmpty()) {
            return "anime"; // Fallback to a default subreddit
        }
        Random random = new Random();
        return subreddits.get(random.nextInt(subreddits.size()));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String category =  "anime";

        boolean includeVideos = true;
        event.deferReply().queue();
        fetchAndSendMedia(event, category, includeVideos, 0);
    }

    private void fetchAndSendMedia(SlashCommandInteractionEvent event, String category, boolean includeVideos, int attempt) {
        // Get Reddit Token Variables and Initialize Config
        Dotenv config = bot.getConfig();
        String clientID = config.get("REDDIT_CLIENT_ID");
        String secretID = config.get("REDDIT_SECRET_ID");
        String username = config.get("REDDIT_USERNAME");
        String password = config.get("REDDIT_PASSWORD");

        // Check to make sure Reddit Token isn't expired before running command.
        String token = refreshRedditToken(clientID, secretID, username, password);

        if (token == null) {
            event.getHook().sendMessage("RedditToken Refresh failed, contact Bot Administrator for support.").setEphemeral(true).queue();
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            event.getHook().sendMessage("Failed finding Images after multiple attempts, please try again later.").setEphemeral(true).queue();
            return;
        }

        String subreddit = getRandomSubreddit(category);

        try {
            String mediaUrl = null;
            boolean validMedia = false;

            // Try fetching a valid media URL with a maximum of 10 attempts
            while (!validMedia) {
                mediaUrl = redditClient.getRandomImage(subreddit);
                System.out.println("Media URL: " + mediaUrl);
                validMedia = redditClient.isValidUrl(mediaUrl);
                if (!includeVideos && (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com") || mediaUrl.contains("youtu.be") || mediaUrl.contains("youtube"))) {
                    validMedia = false; // Skip videos if not desired
                } else if (mediaUrl.contains("/comments") || mediaUrl.contains("imgur.com")) {
                    validMedia = false;
                }
            }

            if (mediaUrl.contains("docs.google")) {
                // Get new URL for Image
                fetchAndSendMedia(event, category, includeVideos, attempt);

            }

            if (mediaUrl.contains("redgifs.com/ifr")) {
                mediaUrl = mediaUrl.replace("ifr", "watch");
            }

            // Handling different media types
            if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("x.com")) {
                if (includeVideos) {
                    String message = String.format("**Here's a random video from r/%s:**\n||%s||", subreddit, mediaUrl);
                    event.getHook().sendMessage(message).queue();
                } else {
                    throw new IOException("Video found, but videos are not allowed.");
                }
            } else if (mediaUrl.endsWith(".gif") || mediaUrl.contains("v.redd.it")) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random gif from r/" + subreddit)
                        .setImage(mediaUrl);

                // Send the embed and add an error handler for failed image loading
                event.getHook().sendMessageEmbeds(embed.build()).queue(
                        message -> {
                            // Success callback
                        },
                        throwable -> {
                            // If image failed to load, retry fetching another image
                            System.out.println("Image failed to load, retrying...");
                            fetchAndSendMedia(event, category, includeVideos, attempt + 1);
                        }
                );
            } else if (mediaUrl.contains("reddit.com/gallery")) {
                List<String> galleryUrls = redditClient.getGalleryImages(mediaUrl);
                if (galleryUrls.isEmpty()) {
                    fetchAndSendMedia(event, category, includeVideos, attempt + 1); // Retry with a new media URL
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random gallery from r/" + subreddit)
                        .setImage(galleryUrls.get(0))
                        .setFooter("Page 1/" + galleryUrls.size());

                event.getHook().sendMessageEmbeds(embed.build()).queue(message -> {
                    bot.getGalleryManager().addGallery(message.getIdLong(), galleryUrls);
                    bot.getGalleryManager().addButtons(message, galleryUrls.size());
                });
            } else {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random image from r/" + subreddit)
                        .setImage(mediaUrl);

                // Send the embed and add an error handler for failed image loading
                event.getHook().sendMessageEmbeds(embed.build()).queue(
                        message -> {
                            // Success callback
                        },
                        throwable -> {
                            // If image failed to load, retry fetching another image
                            System.out.println("Image failed to load, retrying...");
                            fetchAndSendMedia(event, category, includeVideos, attempt + 1);
                        }
                );
            }
        } catch (IOException e) {
            System.out.println("Failed finding Images, Retrying...");
            fetchAndSendMedia(event, category, includeVideos, attempt + 1);
        }
    }

}
