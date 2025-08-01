package org.redacted.Commands.Fun;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.SocialMedia.Reddit.RedditClient;
import org.redacted.util.SocialMedia.Reddit.RedditOAuth;
import org.redacted.util.SocialMedia.Reddit.RedditTokenManager;
import org.redacted.util.embeds.EmbedColor;

import java.io.IOException;
import java.util.*;

/**
 * Command that fetches NSFW images from Reddit based on specified categories.
 * Utilizes the RedditClient to interact with the Reddit API and fetch media.
 * Supports both images and videos, with a maximum of 10 attempts to find valid media.
 *
 * @author Derrick Eberlein
 */
public class NSFWCommand extends Command {
    private RedditClient redditClient;
    private final Redacted bot;
    private static final int MAX_ATTEMPTS = 10;
    private final RedditTokenManager redditTokenManager;

    // Mapping of categories to subreddits
    private final Map<String, List<String>> categoryToSubreddits;

    /**
     * Constructor for the NSFWCommand.
     * Initializes the command with its name, description, and required arguments.
     *
     * @param bot The Redacted bot instance.
     */
    public NSFWCommand(Redacted bot) {
        super(bot);
        this.bot = bot;
        System.out.println("Initializing NSFWCommand...");
        Dotenv config = bot.getConfig();
        String clientID = config.get("REDDIT_CLIENT_ID");
        String secretID = config.get("REDDIT_SECRET_ID");
        String username = config.get("REDDIT_USERNAME");
        String password = config.get("REDDIT_PASSWORD");

        // Initialize category to subreddit mapping
        categoryToSubreddits = new HashMap<>();
        categoryToSubreddits.put("porn", List.of("porn", "nsfw", "RealGirls"));
        categoryToSubreddits.put("boobs", List.of("boobs", "ShakingBoobs", "YummyBoobs", "smallboobs", "PerfectBoobs", "BoobsAndTities", "Stacked"));
        categoryToSubreddits.put("lesbian", List.of("lesbians", "Lesbian_gifs", "girlskissing", "LesbianFantasy", "LesbianGoneWild", "RoughLesbianSex"));
        categoryToSubreddits.put("furry", List.of("yiff", "FurryOnHuman", "FurryPornHeaven", "Furry_Porn", "FurryAsses", "FurryPornSubreddit"));
        categoryToSubreddits.put("hentai", List.of("hentai", "HENTAI_GIF", "Hentai__videos", "rule34", "Hentai_Interracial", "HelplessHentai", "HentaiBreeding", "CumHentai", "thick_hentai", "netorare", "PublicHentai", "HentaiAndRoleplayy", "HentaiBeast", "MonsterGirl", "NTR", "HentaiBullying", "HentaiAnal", "HentaiCumsluts", "EmbarrassedHentai", "UpskirtHentai", "Naruto_Hentai", "MaidHentai", "YuriHentai", "Uniform_Hentai", "HentaiSchoolGirls"));
        categoryToSubreddits.put("public", List.of("public", "PublicFlashing", "PublicSexPorn", "RealPublicNudity", "PublicFuckTube", "Caught_in_public", "PUBLICNUDITY", "PublicFetish"));
        categoryToSubreddits.put("raven", List.of("RavenNSFW", "RavenCosplayNSFW"));
        categoryToSubreddits.put("mihoyo", List.of("ZenlessPorn", "HonkaiStarRailHentai", "HonkaiStarRail34", "HonkaiImpactR34", "GenshinImpactHentai", "GenshinImpactNSFW"));
        categoryToSubreddits.put("bg3", List.of("bg3r34", "BaldursGateR34", "dnd_nsfw"));
        categoryToSubreddits.put("cyberpunk", List.of("Lucy_Cyberpunk_r34", "Cyberpunk2077_R34", "CyberPunkNsfw"));
        categoryToSubreddits.put("milf", List.of("milf", "MilfBody", "maturemilf", "MilfPawg", "TotalpackageMILF", "HotMoms"));
        categoryToSubreddits.put("japanese", List.of("NSFW_Japanese", "JapanesePorn2", "AsiansGoneWild", "JapaneseAsses", "JapaneseKissing", "juicyasians", "japanese_adult_video", "JapaneseFacials", "JapaneseBreastSucking", "creampied_japanese", "JapaneseGoneWild_"));
        categoryToSubreddits.put("asian", List.of("AsiansGoneWild", "AsianPorn", "AsianCumsluts", "AsianBlowjobs", "AsianHotties", "SmallAsian", "SubmissiveAsianSluts", "Sexy_Asians"));
        categoryToSubreddits.put("black", List.of("BlackGirlsCentral", "UofBlack", "BlackPornMatters", "BlackHentai", "BlackTitties"));
        categoryToSubreddits.put("white", List.of("WhiteGirls", "thickwhitegirls", "CurvyWhiteGirls", "PhatAssWhiteGirl"));

        // Get Reddit API Token
        RedditOAuth redditOAuth = new RedditOAuth(bot.httpClient, bot.gson);
        this.redditTokenManager = new RedditTokenManager(bot.getDatabase(), redditOAuth, clientID, secretID, username, password);

        this.name = "nsfw";
        this.description = "Get an nsfw image [18+ only].";
        this.category = Category.FUN;
        this.permission = Permission.MANAGE_CHANNEL;
        this.args.add(new OptionData(OptionType.STRING, "category", "The type of nsfw image to generate")
                .addChoice("porn", "porn")
                .addChoice("boobs", "boobs")
                .addChoice("lesbian", "lesbian")
                .addChoice("furry", "furry")
                .addChoice("hentai", "hentai")
                .addChoice("public", "public")
                .addChoice("bg3", "bg3")
                .addChoice("raven", "raven")
                .addChoice("mihoyo", "mihoyo")
                .addChoice("cyberpunk", "cyberpunk")
                .addChoice("milf", "milf")
                .addChoice("japanese", "japanese")
                .addChoice("asian", "asian")
                .addChoice("black", "black")
                .addChoice("white", "white"));

        this.args.add(new OptionData(OptionType.BOOLEAN, "video", "Whether to include videos in the results").setRequired(false));
    }

    /**
     * Returns a random subreddit based on the specified category.
     * If the category is not found or has no subreddits, defaults to "nsfw".
     *
     * @param category The category for which to get a subreddit.
     * @return A random subreddit from the specified category.
     */
    private String getRandomSubreddit(String category) {
        List<String> subreddits = categoryToSubreddits.get(category);
        if (subreddits == null || subreddits.isEmpty()) {
            return "nsfw"; // Fallback to a default subreddit
        }
        Random random = new Random();
        return subreddits.get(random.nextInt(subreddits.size()));
    }

    /**
     * Executes the NSFW command.
     * Fetches a random NSFW image or video from Reddit based on the specified category.
     * Checks if the command is executed in an NSFW channel and handles the media fetching logic.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get Reddit Token Variables and Initialize Config
        String token = redditTokenManager.getValidToken();

        // Initialize the RedditClient with your access token
        if (token != null) {
            System.out.println("Initializing RedditClient...");
            this.redditClient = new RedditClient(bot.httpClient, redditTokenManager);
            System.out.println("RedditClient initialized with token");
        } else {
            System.out.println("Token was null, RedditClient not initialized");
        }

        OptionMapping categoryOption = event.getOption("category");
        String category = (categoryOption != null) ? categoryOption.getAsString() : "nsfw"; // Default category if none provided

        boolean includeVideos = event.getOption("video") != null && Objects.requireNonNull(event.getOption("video")).getAsBoolean();

        event.deferReply().queue();

        // Check to ensure this is an NSFW Channel
        if (!event.getChannel().asTextChannel().isNSFW()) {
            System.out.println("This is not an NSFW Channel");
            event.getHook().sendMessage("This is not an NSFW Channel, cannot run NSFW Command in this channel").queue();
            return;
        }

        // Set Attempt Count for fetching NSFW Image
        int attempt = 0;

        if (token != null) {
            fetchAndSendMedia(event, category, includeVideos, attempt);
        }
    }

    /**
     * Executes the NSFW command in a specific text channel with a given category.
     * This method is used for the LoopNSFWCommand to fetch and send media in a loop.
     *
     * @param channelId The TextChannel where the media will be sent.
     * @param category  The category of NSFW content to fetch.
     */
    public void executeCategory(TextChannel channelId, String category) {
        // Set Int Attempts before it stops trying to find Images for LoopNSFW command
        int attempt = 0;

        // Call Loop Command
        fetchAndSendMediaLoop(channelId, category, attempt);
    }

    /**
     * Fetches and sends a random NSFW media (image or video) from Reddit based on the specified category.
     * Handles retries and ensures valid media is sent.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     * @param category The category of NSFW content to fetch.
     * @param includeVideos Whether to include videos in the results.
     * @param attempt The current attempt number for fetching media.
     */
    private void fetchAndSendMedia(SlashCommandInteractionEvent event, String category, boolean includeVideos, int attempt) {
        // Ensure RedditClient is initialized
        ensureRedditClientInitialized();

        if (attempt >= MAX_ATTEMPTS) {
            event.getHook().sendMessage("Failed finding Images after multiple attempts, please try again later.").setEphemeral(true).queue();
            String stringID = event.getChannel().getId();
            LoopNSFWCommand.stopLoop(stringID);
            return;
        }

        String subreddit = getRandomSubreddit(category);

        String mediaUrl = null;
        boolean validMedia = false;

        // Try fetching a valid media URL with a maximum of 10 attempts
        while (!validMedia && attempt < 10) {
            attempt++;
            try {
                mediaUrl = redditClient.getRandomImageNSFW(subreddit);
                if (mediaUrl == null) {
                    event.getHook().sendMessage("No media could be fetched. Try again later.").queue();
                    return;
                }
                System.out.println("Attempt " + attempt + ": Media URL -> " + mediaUrl);

                // Skip full validation for Reddit galleries (.json endpoints)
                if (mediaUrl.contains("gallery") || mediaUrl.endsWith(".json")) {
                    validMedia = true;
                } else {
                    validMedia = redditClient.isValidMediaUrl(mediaUrl, includeVideos);
                }

            } catch (IOException e) {
                System.err.println("Error during media fetch attempt " + attempt + ": " + e.getMessage());
                e.printStackTrace();
                // Optional: continue silently or break depending on your tolerance
            }
        }

        assert mediaUrl != null;
        if (mediaUrl.contains("redgifs.com/ifr")) {
            mediaUrl = mediaUrl.replace("ifr", "watch");
        }

        if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("xhamster") || mediaUrl.contains("redtube") || mediaUrl.contains("pornhub") || mediaUrl.contains("video")) {
            if (includeVideos) {
                String message = String.format("**Here's a random NSFW video from r/%s:**\n%s", subreddit, mediaUrl);
                event.getHook().sendMessage(message).queue();
            } else {
                System.out.println("Video found, but videos are not allowed.");
                fetchAndSendMedia(event, category, false, attempt);
            }
        } else if (mediaUrl.endsWith(".gif")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW gif from r/" + subreddit)
                    .setImage(mediaUrl);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } else if (mediaUrl.contains("reddit.com/gallery")) {
            List<String> galleryUrls = redditClient.getGalleryImages(mediaUrl);
            if (galleryUrls.isEmpty()) {
                fetchAndSendMedia(event, category, includeVideos, attempt + 1); // Retry with a new media URL
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW gallery from r/" + subreddit)
                    .setImage(galleryUrls.get(0))
                    .setFooter("Page 1/" + galleryUrls.size());

            event.getHook().sendMessageEmbeds(embed.build()).queue(message -> {
                bot.getGalleryManager().addGallery(message.getIdLong(), galleryUrls);
                bot.getGalleryManager().addButtons(message, galleryUrls.size());
            });
        } else {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW image from r/" + subreddit)
                    .setImage(mediaUrl);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }
    }

    /**
     * Fetches and sends a random NSFW media (image or video) in a loop.
     * This method is used for the LoopNSFWCommand to continuously fetch and send media.
     *
     * @param channel The TextChannel where the media will be sent.
     * @param category The category of NSFW content to fetch.
     * @param attempt The current attempt number for fetching media.
     */
    private void fetchAndSendMediaLoop(TextChannel channel, String category, int attempt) {
        // Ensure RedditClient is initialized
        ensureRedditClientInitialized();

        // Check to make sure Reddit Token isn't expired before running command.
        String token = redditTokenManager.getValidToken();

        if (token == null) {
            channel.sendMessage("RedditToken Refresh failed, contact Bot Administrator for support.").queue();
            return;
        }

        if (attempt >= MAX_ATTEMPTS) {
            channel.sendMessage("Failed finding Images after multiple attempts, please try again later.").queue();
            String stringID = channel.getId();
            LoopNSFWCommand.stopLoop(stringID);
            return;
        }

        String subreddit = getRandomSubreddit(category);

        String mediaUrl = null;
        boolean validMedia = false;

        while (!validMedia && attempt < 10) {
            attempt++;
            try {
                mediaUrl = redditClient.getRandomImageNSFW(subreddit);
                if (mediaUrl == null) {
                    channel.sendMessage("No media could be fetched. Try again later.").queue();
                    return;
                }

                System.out.println("Attempt " + attempt + ": Media URL -> " + mediaUrl);

                // Accept Reddit JSON gallery endpoint directly
                if (mediaUrl.contains("gallery") || mediaUrl.endsWith(".json")) {
                    validMedia = true;
                } else {
                    validMedia = redditClient.isValidMediaUrl(mediaUrl, true);
                }

            } catch (IOException e) {
                System.err.println("Error during media fetch attempt " + attempt + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        assert mediaUrl != null;
        if (mediaUrl.contains("redgifs.com/ifr")) {
            mediaUrl = mediaUrl.replace("ifr", "watch");
        }

        if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("xhamster") || mediaUrl.contains("redtube") || mediaUrl.contains("pornhub") || mediaUrl.contains("video")) {
            String message = String.format("**Here's a random NSFW video from r/%s:**\n%s", subreddit, mediaUrl);
            channel.sendMessage(message).queue();
        } else if (mediaUrl.endsWith(".gif")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW gif from r/" + subreddit)
                    .setImage(mediaUrl);
            channel.sendMessageEmbeds(embed.build()).queue();
        } else if (mediaUrl.contains("reddit.com/gallery")) {
            List<String> galleryUrls = redditClient.getGalleryImages(mediaUrl);
            if (galleryUrls.isEmpty()) {
                fetchAndSendMediaLoop(channel, category, attempt + 1); // Retry with a new media URL
                return;
            }
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW gallery from r/" + subreddit)
                    .setImage(galleryUrls.get(0))
                    .setFooter("Page 1/" + galleryUrls.size());

            channel.sendMessageEmbeds(embed.build()).queue(message -> {
                bot.getGalleryManager().addGallery(message.getIdLong(), galleryUrls);
                bot.getGalleryManager().addButtons(message, galleryUrls.size());
            });
        } else {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Here's a random NSFW image from r/" + subreddit)
                    .setImage(mediaUrl);
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    /**
     * Ensures that the RedditClient is initialized.
     * This method is called lazily to avoid unnecessary initialization if the client is not used.
     */
    private void ensureRedditClientInitialized() {
        if (redditClient == null) {
            this.redditClient = new RedditClient(bot.httpClient, redditTokenManager);
            System.out.println("RedditClient lazily initialized");
        }
    }

}