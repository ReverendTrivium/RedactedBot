package org.redacted.Commands.Fun;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.Document;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.SocialMedia.Reddit.RedditClient;
import org.redacted.util.SocialMedia.Reddit.RedditOAuth;
import org.redacted.util.embeds.EmbedColor;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class NSFWCommand extends Command {
    private RedditClient redditClient;
    private final RedditOAuth redditOAuth;
    private final Redacted bot;
    private static final int MAX_ATTEMPTS = 10;

    // Mapping of categories to subreddits
    private final Map<String, List<String>> categoryToSubreddits;

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
        categoryToSubreddits.put("gay", List.of("GayGifs", "GayKink", "trapsarentgay", "FemBoys", "GayPorn_NSFW"));
        categoryToSubreddits.put("lesbian", List.of("lesbians", "Lesbian_gifs", "girlskissing", "lesbianOral", "LesbianFantasy", "LesbianGoneWild", "LesbianPegging", "RoughLesbianSex", "LesbianPee"));
        categoryToSubreddits.put("furry", List.of("yiff", "FurryOnHuman", "FurryPornHeaven", "Furry_Porn", "FurryAsses", "FurryPornSubreddit"));
        categoryToSubreddits.put("hentai", List.of("hentai", "HENTAI_GIF", "Hentai__videos", "rule34", "Hentai_Interracial", "FreeuseHenati", "HelplessHentai", "HentaiBreeding", "CumHentai", "thick_hentai", "netorare", "PublicHentai", "HentaiAndRoleplayy", "HentaiBeast", "nhentai", "MonsterGirl", "NTR", "HentaiBullying", "HentaiAnal", "HentaiCumsluts", "EmbarrassedHentai", "UpskirtHentai", "Naruto_Hentai", "MaidHentai", "YuriHentai", "Uniform_Hentai", "HentaiSchoolGirls"));
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
        categoryToSubreddits.put("india", List.of("downblouseIndia", "SuperModelIndia", "3SomeInIndia"));
        categoryToSubreddits.put("arab", List.of("ArabPorn", "Arab_goddess", "BDSM_Arab", "BrownHotties"));
        categoryToSubreddits.put("native", List.of("NativeAmericanGirls2", "NativeBums", "nativeamericanbabes"));

        // Get Reddit API Token
        this.redditOAuth = new RedditOAuth(bot.httpClient, bot.gson);
        String token = getRedditToken(clientID, secretID, username, password);

        this.name = "nsfw";
        this.description = "Get an nsfw image [18+ only].";
        this.category = Category.FUN;
        this.permission = Permission.MANAGE_CHANNEL;
        this.args.add(new OptionData(OptionType.STRING, "category", "The type of nsfw image to generate")
                .addChoice("porn", "porn")
                .addChoice("boobs", "boobs")
                .addChoice("gay", "gay")
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
                .addChoice("white", "white")
                .addChoice("india", "india")
                .addChoice("arab", "arab")
                .addChoice("native", "native"));

        this.args.add(new OptionData(OptionType.BOOLEAN, "video", "Whether to include videos in the results").setRequired(false));

        // Initialize the RedditClient with your access token
        if (token != null) {
            System.out.println("Initializing RedditClient...");
            this.redditClient = new RedditClient(bot.httpClient, token);
            System.out.println("RedditClient initialized with token");
        } else {
            System.out.println("Token was null, RedditClient not initialized");
        }
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
            return "nsfw"; // Fallback to a default subreddit
        }
        Random random = new Random();
        return subreddits.get(random.nextInt(subreddits.size()));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping categoryOption = event.getOption("category");
        String category = (categoryOption != null) ? categoryOption.getAsString() : "nsfw"; // Default category if none provided

        boolean includeVideos = event.getOption("video") != null && Objects.requireNonNull(event.getOption("video")).getAsBoolean();

        event.deferReply().queue();
        fetchAndSendMedia(event, category, includeVideos, 0);
    }

    public void executeCategory(String channelId, String category) {
        fetchAndSendMedia(channelId, category, 0);
    }


    private void fetchAndSendMedia(SlashCommandInteractionEvent event, String category, boolean includeVideos, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            event.getHook().sendMessage("Failed finding Images after multiple attempts, please try again later.").queue();
            LoopNSFWCommand.stopLoop();
            return;
        }

        String subreddit = getRandomSubreddit(category);

        try {
            String mediaUrl = null;
            boolean validMedia = false;
            int attempts = 0;

            // Try fetching a valid media URL with a maximum of 10 attempts
            while (!validMedia && attempts < 10) {
                attempts++;
                mediaUrl = redditClient.getRandomImage(subreddit);
                System.out.println("Media URL: " + mediaUrl);
                validMedia = redditClient.isValidUrl(mediaUrl);
                if (!includeVideos && (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com") || mediaUrl.contains("youtu.be") || mediaUrl.contains("youtube"))) {
                    validMedia = false; // Skip videos if not desired
                } else if (mediaUrl.contains("/comments") || mediaUrl.contains("imgur.com")) {
                    validMedia = false;
                }
            }

            if (mediaUrl.contains("redgifs.com/ifr")) {
                mediaUrl = mediaUrl.replace("ifr", "watch");
            }

            if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("xhamster") || mediaUrl.contains("redtube") || mediaUrl.contains("pornhub")) {
                if (includeVideos) {
                    String message = String.format("**Here's a random NSFW video from r/%s:**\n||%s||", subreddit, mediaUrl);
                    event.getHook().sendMessage(message).queue();
                } else {
                    throw new IOException("Video found, but videos are not allowed.");
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
        } catch (IOException e) {
            System.out.println("Failed finding Images, Retrying...");
            fetchAndSendMedia(event, category, includeVideos, attempt + 1);
        }
    }

    private void fetchAndSendMedia(String channelId, String category, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            Objects.requireNonNull(bot.getShardManager().getTextChannelById(channelId)).sendMessage("Failed finding Images after multiple attempts, please try again later.").queue();
            LoopNSFWCommand.stopLoop();
            return;
        }

        String subreddit = getRandomSubreddit(category);

        try {
            String mediaUrl = null;
            boolean validMedia = false;
            int attempts = 0;

            while (!validMedia && attempts < 10) {
                attempts++;
                mediaUrl = redditClient.getRandomImage(subreddit);
                System.out.println("Media URL: " + mediaUrl);

                //Make sure their are no Comment mediaURLs
                validMedia = redditClient.isValidUrl(mediaUrl);
                if (mediaUrl.contains("/comments") || mediaUrl.contains("imgur.com")) {
                    validMedia = false;
                }
            }

            if (mediaUrl.contains("redgifs.com/ifr")) {
                mediaUrl = mediaUrl.replace("ifr", "watch");
            }

            if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("v.redd.it") || mediaUrl.contains("redgifs.com/watch") || mediaUrl.contains("www.youtube.com/") || mediaUrl.contains("youtu.be") || mediaUrl.contains("xhamster") || mediaUrl.contains("redtube") || mediaUrl.contains("pornhub")) {
                String message = String.format("**Here's a random NSFW video from r/%s:**\n||%s||", subreddit, mediaUrl);
                Objects.requireNonNull(bot.getShardManager().getTextChannelById(channelId)).sendMessage(message).queue();
            } else if (mediaUrl.endsWith(".gif")) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random NSFW gif from r/" + subreddit)
                        .setImage(mediaUrl);
                Objects.requireNonNull(bot.getShardManager().getTextChannelById(channelId)).sendMessageEmbeds(embed.build()).queue();
            } else if (mediaUrl.contains("reddit.com/gallery")) {
                List<String> galleryUrls = redditClient.getGalleryImages(mediaUrl);
                if (galleryUrls.isEmpty()) {
                    fetchAndSendMedia(channelId, category, attempt + 1); // Retry with a new media URL
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random NSFW gallery from r/" + subreddit)
                        .setImage(galleryUrls.get(0))
                        .setFooter("Page 1/" + galleryUrls.size());

                Objects.requireNonNull(bot.getShardManager().getTextChannelById(channelId)).sendMessageEmbeds(embed.build()).queue(message -> {
                    bot.getGalleryManager().addGallery(message.getIdLong(), galleryUrls);
                    bot.getGalleryManager().addButtons(message, galleryUrls.size());
                });
            } else {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(EmbedColor.DEFAULT.color)
                        .setTitle("Here's a random NSFW image from r/" + subreddit)
                        .setImage(mediaUrl);
                Objects.requireNonNull(bot.getShardManager().getTextChannelById(channelId)).sendMessageEmbeds(embed.build()).queue();
            }
        } catch (IOException e) {
            System.out.println("Failed finding Images, Retrying...");
            fetchAndSendMedia(channelId, category, attempt + 1);
        }
    }
}