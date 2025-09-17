package org.redacted.listeners;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redacted.Database.Data.GuildData;
import org.redacted.Handlers.MusicHandler;
import org.redacted.Redacted;
import org.redacted.util.SecurityUtils;
import org.redacted.util.embeds.EmbedUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Module for music player backend and voice channel events.
 *
 * @author Derrick Eberlein
 */
public class MusicListener extends ListenerAdapter {
    private final Redacted bot;
    private final @NotNull AudioPlayerManager playerManager;

    /** Music executor for handling music-related tasks asynchronously.
     * Core pool size of 2, maximum pool size of 8, and a keep-alive time of 60 seconds.
     * Function uses a linked blocking queue with a capacity of 200 tasks.
     * Threads are named "music-exec" and are set as daemon threads.
     */
    public static final ExecutorService MUSIC_EXECUTOR =
            new ThreadPoolExecutor(2, 8, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(200),
                    r -> {
                        Thread t = new Thread(r, "music-exec");
                        t.setDaemon(true);
                        return t;
                    });

    /**
     * Setup audio player manager.
     */
    public MusicListener(Redacted bot) {
        this.bot = bot;
        this.playerManager = new DefaultAudioPlayerManager();

        // Add YouTube support
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true)); // true = allow search

        // Add Apple Music support

        // Add audio player to source manager
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    /**
     * Formats track length into a readable string.
     *
     * @param trackLength numerical track length
     * @return string of track length (ex. 2:11)
     */
    public static @NotNull String formatTrackLength(long trackLength) {
        long hours = TimeUnit.MILLISECONDS.toHours(trackLength);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(trackLength) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(trackLength));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(trackLength) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(trackLength));
        String time = "";
        if (hours > 0) time += hours + ":";
        if (minutes < 10 && hours > 0) time += "0";
        time += minutes + ":";
        if (seconds < 10) time += "0";
        time += seconds;
        return time;
    }

    /**
     * Runs a number of validity checks to make sure music player
     * instance is valid before retrieval.
     *
     * @param event The slash command event containing command data.
     * @return Null if invalid status, otherwise music player instance.
     */
    @Nullable
    public MusicHandler getMusic(@NotNull SlashCommandInteractionEvent event, boolean skipQueueCheck) {
        GuildData settings = GuildData.get(event.getGuild(), bot);
        // Check if user is in voice the channel
        if (!inChannel(Objects.requireNonNull(event.getMember()))) {
            String text = "Please connect to a voice channel first!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return null;
        }
        // Bot should join a voice channel if not already in one.
        AudioChannel channel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();
        if (settings.musicHandler == null || !event.getGuild().getAudioManager().isConnected()) {
            assert channel != null;
            joinChannel(settings, channel, event.getChannel().asTextChannel());
        }
        // Check if music is playing in this guild
        if (!skipQueueCheck) {
            if (settings.musicHandler == null || settings.musicHandler.getQueue().isEmpty()) {
                String text = ":sound: There are no songs in the queue!";
                event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
                return null;
            }
            // Check if member is in the right voice channel
            if (settings.musicHandler.getPlayChannel() != channel) {
                String text = "You are not in the same voice channel as Redacted!";
                event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
                return null;
            }
        }
        return settings.musicHandler;
    }

    /**
     * Joins the specified voice channel and sets up the music handler.
     *
     * @param guildData The guild data for the current guild.
     * @param channel   The audio channel to join.
     * @param logChannel The text channel to log music events.
     */
    public void joinChannel(@NotNull GuildData guildData, @NotNull AudioChannel channel, TextChannel logChannel) {
        AudioManager manager = channel.getGuild().getAudioManager();
        if (guildData.musicHandler == null) {
            guildData.musicHandler = new MusicHandler(playerManager.createPlayer());
        }
        manager.setSendingHandler(guildData.musicHandler);
        Objects.requireNonNull(guildData.musicHandler).setLogChannel(logChannel);
        guildData.musicHandler.setPlayChannel(channel);
        manager.openAudioConnection(channel);
    }

    /**
     * Checks whether the specified member is in a voice channel.
     *
     * @param member The specified Member
     * @return True if this member is in a voice channel, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean inChannel(@NotNull Member member) {
        return member.getVoiceState() != null && member.getVoiceState().inAudioChannel();
    }

    /**
     * Add a track to the specified guild.
     *
     * @param event  A slash command event.
     * @param url    The track URL.
     * @param userID   The ID of the user that added this track.
     */
    // In MusicListener
    public void addTrackAsync(SlashCommandInteractionEvent event, String url, String userID) {
        // Basic checks
        if (url == null || url.isBlank()) {
            event.getHook().setEphemeral(true).editOriginal("Please provide a valid song name or link.").queue();
            return;
        }

        MusicHandler music = GuildData.get(event.getGuild(), bot).getMusicHandler();
        if (music == null) {
            System.out.println("MusicHandler is null in addTrackAsync");
            event.getHook().editOriginal("Music service is not available. Please try again later.").queue();
            return;
        }

        // Whitelist check (non-throwing)
        try {
            if (!SecurityUtils.isUrlWhitelisted(url)) {
                event.getHook().setEphemeral(true).editOriginal("That URL isn’t allowed.").queue();
                return;
            }
        } catch (MalformedURLException ignored) {}

        // Spotify handling (done on executor thread)
        if (url.startsWith("https://open.spotify.com/")) {
            String id = extractSpotifyId(url);
            try {
                SpotifyApi api = bot.getSpotifyAPI().getSpotifyApi();
                if (api == null) {
                    event.getHook().editOriginal("Spotify API is not initialized.").queue();
                    return;
                }

                if (url.contains("/track/")) {
                    Track track = api.getTrack(id).build().execute(); // OK here (off-thread)
                    url = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName();
                    // fall through to loadItemOrdered below
                } else if (url.contains("/playlist/")) {
                    Playlist playlist = api.getPlaylist(id).build().execute();
                    event.getHook().editOriginal(":notes: | Adding playlist `" + playlist.getName() + "`...").queue();

                    // Add each track asynchronously but in-order per guild
                    for (PlaylistTrack item : playlist.getTracks().getItems()) {
                        Track st = (Track) item.getTrack();
                        String ytQuery = "ytsearch:" + st.getName() + " " + st.getArtists()[0].getName();
                        enqueueQueryInOrder(event, music, ytQuery, userID, /*silent*/ true);
                    }
                    event.getChannel().sendMessage(":white_check_mark: Playlist queued!").queue();
                    return;
                } else {
                    event.getHook().editOriginal("Spotify link type not supported yet.").queue();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginal("Failed to resolve Spotify link.").queue();
                return;
            }
        }

        // Normal path: URL or ytsearch
        enqueueQueryInOrder(event, music, url, userID, /*silent*/ false);
    }

    /** Enqueues a query in order for the specified music handler.
     *
     * @param event   The slash command interaction event.
     * @param music   The music handler to enqueue the track in.
     * @param query   The search query or URL.
     * @param userID    The ID of the user that added this track.
     * @param silent  If true, suppresses user feedback messages.
     */
    private void enqueueQueryInOrder(SlashCommandInteractionEvent event,
                                     MusicHandler music,
                                     String query,
                                     String userID,
                                     boolean silent) {
        playerManager.loadItemOrdered(music, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(@NotNull AudioTrack audioTrack) {
                audioTrack.setUserData(userID);
                music.enqueue(audioTrack);
                if (!silent) {
                    event.getHook().editOriginal(":notes: | Added **" + audioTrack.getInfo().title + "** to the queue.").queue();
                }
            }

            /** Handles when a playlist is loaded.
             *
             * @param playlist The loaded AudioPlaylist.
             */
            @Override
            public void playlistLoaded(@NotNull AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().get(0));
                    return;
                }
                // Queue up to 100 in-order
                int limit = Math.min(100, playlist.getTracks().size());
                if (!silent) {
                    event.getHook().editOriginal(":notes: | Added **" + playlist.getName() + "** with `" + limit + "` songs to the queue.").queue();
                }
                int added = 0;
                for (AudioTrack t : playlist.getTracks()) {
                    if (added++ >= limit) break;
                    t.setUserData(userID);
                    music.enqueue(t);
                }
            }

            /** Handles no matches found.
             *
             */
            @Override
            public void noMatches() {
                if (!silent) {
                    event.getHook().setEphemeral(true).editOriginal("No results for that query.").queue();
                }
            }

            /** Handles load failures.
             *
             * @param e The FriendlyException containing error details.
             */
            @Override
            public void loadFailed(FriendlyException e) {
                if (!silent) {
                    event.getHook().setEphemeral(true).editOriginal("Load failed: " + e.getMessage()).queue();
                }
            }
        });
    }

    /**
     * Loads a track synchronously and returns it.
     *
     * @param url The URL or search query for the track.
     * @return The loaded AudioTrack, or null if loading failed.
     */
    private String extractSpotifyId(String url) {
        String[] parts = url.split("/");
        String idPart = parts[parts.length - 1];
        return idPart.contains("?") ? idPart.split("\\?")[0] : idPart;
    }
}
