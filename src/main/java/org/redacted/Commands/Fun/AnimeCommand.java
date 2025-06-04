package org.redacted.Commands.Fun;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.SocialMedia.Reddit.RedditClient;
import org.redacted.util.SocialMedia.Reddit.RedditOAuth;
import org.redacted.util.SocialMedia.Reddit.RedditTokenManager;
import org.redacted.util.embeds.EmbedColor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AnimeCommand extends Command {
    private RedditClient redditClient;
    private final Redacted bot;
    private static final int MAX_ATTEMPTS = 10;
    private static final String[] SUBREDDITS = {
            "AnimeGirls", "Animemes", "anime", "CuteAnimeGirls", "awwnimate",
            "Cuteanimenekos", "headpats",  "pouts", "AnimeBlush", "MoeBlushing"
    };
    private final RedditTokenManager redditTokenManager;

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
        categoryToSubreddits.put("anime", List.of(SUBREDDITS));

        // Get Reddit API Token
        RedditOAuth redditOAuth = new RedditOAuth(bot.httpClient, bot.gson);

        this.name = "anime";
        this.description = "Get a random anime image! :3";
        this.category = Category.FUN;

        this.redditTokenManager = new RedditTokenManager(bot.getDatabase(), redditOAuth, clientID, secretID, username, password);
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
        // Set Category for Image Fetching
        String category =  "anime";

        // Set Boolean to include videos and defer reply
        boolean includeVideos = true;
        event.deferReply().queue();

        // Set Attempt Variable
        int attempt = 0;

        String token = redditTokenManager.getValidToken();
        // Initialize the RedditClient with your access token
        if (token!= null) {
            System.out.println("Initializing RedditClient...");
            this.redditClient = new RedditClient(bot.httpClient, redditTokenManager);
            System.out.println("RedditClient initialized with token");
        } else {
            System.out.println("Token was null, RedditClient not initialized");
        }

        //Call Image Fetching Function
        fetchAndSendMedia(event, category, includeVideos, attempt);
    }

    private void fetchAndSendMedia(SlashCommandInteractionEvent event, String category, boolean includeVideos, int attempt) {
        // Get Reddit Token Variables and Initialize Config
        Dotenv config = bot.getConfig();
        String clientID = config.get("REDDIT_CLIENT_ID");
        String secretID = config.get("REDDIT_SECRET_ID");
        String username = config.get("REDDIT_USERNAME");
        String password = config.get("REDDIT_PASSWORD");

        // Check to make sure Reddit Token isn't expired before running command.
        String token = redditTokenManager.getValidToken();

        if (token == null) {
            event.getHook().sendMessage("RedditToken Refresh failed, contact Bot Administrator for support.").setEphemeral(true).queue();
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            event.getHook().sendMessage("Failed finding Images after multiple attempts, please try again later.").setEphemeral(true).queue();
            return;
        }

        String subreddit = getRandomSubreddit(category);

        String mediaUrl = null;
        boolean validMedia = false;

        // Try fetching a valid media URL with a maximum of 10 attempts
        while (!validMedia) {
            try {
                // Fetch media from Reddit
                mediaUrl = redditClient.getRandomImage(subreddit);

                // Check for Forbidden (403) response
                if (mediaUrl == null || mediaUrl.contains("\"error\":403")) {
                    System.out.println("403 Forbidden encountered. Retrying with another subreddit...");
                    subreddit = getRandomSubreddit(category); // Pick another subreddit
                    continue;
                }

                System.out.println("Media URL: " + mediaUrl);
                validMedia = redditClient.isValidMediaUrl(mediaUrl, includeVideos);

                // Handle invalid media
                if (!includeVideos && (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com") || mediaUrl.contains("youtu.be") || mediaUrl.contains("youtube"))) {
                    validMedia = false; // Skip videos if not desired
                } else if (mediaUrl.contains("/comments") || mediaUrl.contains("imgur.com") || mediaUrl.contains("patreon.com")) {
                    validMedia = false;
                }
            } catch (IOException e) {
                System.out.println("Error fetching media. Retrying...");
                e.printStackTrace();
            }
        }

        if (mediaUrl.contains("redgifs.com/ifr")) {
            mediaUrl = mediaUrl.replace("ifr", "watch");
        }

        // Handling different media types
        if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("x.com") || mediaUrl.contains("https://v.redd.it/")) {
            if (includeVideos) {
                String message = String.format("**Here's a random video from r/%s:**\n%s", subreddit, mediaUrl);
                event.getHook().sendMessage(message).queue();
            } else {
                System.out.println("Video found, but videos are not allowed.");
                fetchAndSendMedia(event, category, includeVideos, attempt + 1);
            }
        } else if (mediaUrl.endsWith(".gif")) {
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
                    });
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
                    });
        }
    }
}
