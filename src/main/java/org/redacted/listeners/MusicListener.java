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
import java.util.concurrent.TimeUnit;

/**
 * Module for music player backend and voice channel events.
 *
 * @author Derrick Eberlein
 */
public class MusicListener extends ListenerAdapter {
    private final Redacted bot;
    private final @NotNull AudioPlayerManager playerManager;

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
    public void addTrack(SlashCommandInteractionEvent event, String url, String userID) {

        // Check URL validity
        System.out.println("Adding track: " + url);
        if (url == null || url.isBlank()) {
            event.getHook().setEphemeral(true).editOriginal("Please provide a valid song name or link.").queue();
            return;
        }

        // Checking if Code breaks for Debugging
        MusicHandler music = GuildData.get(event.getGuild(), bot).getMusicHandler();
        if (music == null) {
            System.out.println("MusicHandler is null, cannot add track.");
            event.getHook().editOriginal("Music service is not available. Please try again later.").queue();
            return;
        }

        System.out.println("Passed initial checks, proceeding to load track...");

        // Check for SSRF vulnerability with whitelist
        try {
            boolean isWhitelisted = SecurityUtils.isUrlWhitelisted(url);
            if(!isWhitelisted) {
                url = "";
            }
        } catch(MalformedURLException ignored) {
            System.out.println("URL is malformed: " + url);
        }

        // 🔍 Handle Spotify links
        if (url.startsWith("https://open.spotify.com/")) {
            String id = extractSpotifyId(url);
            try {
                if (url.contains("/track/")) {
                    //
                    SpotifyApi api = bot.getSpotifyAPI().getSpotifyApi();
                    // Check if api is null
                    if (api == null) {
                        event.getHook().editOriginal("Spotify API is not initialized.").queue();
                        return;
                    }
                    Track track = api.getTrack(id).build().execute();
                    String ytQuery = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName();
                    System.out.println("Spotify track resolved: " + ytQuery);
                    url = ytQuery;
                } else if (url.contains("/playlist/")) {
                    Playlist playlist = bot.getSpotifyAPI().getSpotifyApi().getPlaylist(id).build().execute();
                    event.getHook().editOriginal(":notes: | Adding playlist `" + playlist.getName() + "`...").queue();

                    for (PlaylistTrack item : playlist.getTracks().getItems()) {
                        Track track = (Track) item.getTrack();
                        String ytQuery = "ytsearch:" + track.getName() + " " + track.getArtists()[0].getName();
                        System.out.println("Spotify playlist track: " + ytQuery);
                        AudioTrack loadedTrack = loadTrackBlocking(ytQuery); // You can convert this to async if needed
                        if (loadedTrack != null) {
                            loadedTrack.setUserData(userID);
                            music.enqueue(loadedTrack);
                        }
                    }

                    event.getChannel().sendMessage(":white_check_mark: Playlist added to queue!").queue();
                    return;
                } else {
                    event.getHook().editOriginal("Spotify link type not supported yet.").queue();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginal("Failed to resolve Spotify track.").queue();
                return;
            }
        }

        // Debugging output
        System.out.println("Loading track: " + url);
        playerManager.loadItem(url, new AudioLoadResultHandler() {

            /**
             * Called when a track is successfully loaded.
             *
             * @param audioTrack The loaded audio track.
             */
            @Override
            public void trackLoaded(@NotNull AudioTrack audioTrack) {
                System.out.println("Track loaded: " + audioTrack.getInfo().title);
                audioTrack.setUserData(userID);
                music.enqueue(audioTrack);

                event.getHook().editOriginal(":notes: | Added **" + audioTrack.getInfo().title + "** to the queue.").queue();
            }

            /**
             * Called when a playlist is successfully loaded.
             *
             * @param audioPlaylist The loaded audio playlist.
             */
            @Override
            public void playlistLoaded(@NotNull AudioPlaylist audioPlaylist) {
                // Check if the playlist is a search result
                System.out.println("Playlist loaded: " + audioPlaylist.getName());
                // Queue first result if YouTube search
                if (audioPlaylist.isSearchResult()) {
                    trackLoaded(audioPlaylist.getTracks().get(0));
                    return;
                }

                // Otherwise load first 100 tracks from playlist
                int total = audioPlaylist.getTracks().size();
                if (total > 100) total = 100;
                event.getHook().editOriginal(":notes: | Added **"+audioPlaylist.getName()+"** with `"+total+"` songs to the queue.").queue();

                total = music.getQueue().size();
                for (AudioTrack track : audioPlaylist.getTracks()) {
                    if (total < 100) {
                        music.enqueue(track);
                    }
                    total++;
                }
            }

            /**
             * Called when no matches are found for the provided query.
             */
            @Override
            public void noMatches() {
                System.out.println("No matches found.");
                String msg = "That is not a valid song!";
                event.getHook().setEphemeral(true).editOriginal(msg).queue();
            }

            /**
             * Called when loading the track fails.
             *
             * @param e The exception that occurred during loading.
             */
            @Override
            public void loadFailed(FriendlyException e) {
                String msg = "That is not a valid link!";
                event.getHook().setEphemeral(true).editOriginal(msg).queue();
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

    /**
     * Loads a track synchronously and returns it.
     *
     * @param query The URL or search query for the track.
     * @return The loaded AudioTrack, or null if loading failed.
     */
    private AudioTrack loadTrackBlocking(String query) {
        final AudioTrack[] result = new AudioTrack[1];
        final Object lock = new Object();

        playerManager.loadItem(query, new AudioLoadResultHandler() {

            /**
             * Called when a track is successfully loaded.
             *
             * @param track The loaded audio track.
             */
            @Override
            public void trackLoaded(AudioTrack track) {
                result[0] = track;
                synchronized (lock) {
                    lock.notify();
                }
            }

            /**
             * Called when a playlist is successfully loaded.
             *
             * @param playlist The loaded audio playlist.
             */
            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                result[0] = playlist.getTracks().get(0);
                synchronized (lock) {
                    lock.notify();
                }
            }

            /**
             * Called when no matches are found for the provided query.
             */
            @Override
            public void noMatches() {
                synchronized (lock) {
                    lock.notify();
                }
            }

            /**
             * Called when loading the track fails.
             *
             * @param e The exception that occurred during loading.
             */
            @Override
            public void loadFailed(FriendlyException e) {
                e.printStackTrace();
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(5000); // 5 second timeout
            } catch (InterruptedException ignored) {}
        }

        return result[0];
    }
}
