package org.redacted.Handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redacted.listeners.MusicListener;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.redacted.util.embeds.EmbedColor;
import org.redacted.util.embeds.EmbedUtils;

import java.nio.ByteBuffer;

/**
 * Handles music for each guild with a unique queue and audio player for each.
 *
 * @author Derrick Eberlein
 */
@Setter
@Getter
public class MusicHandler implements AudioSendHandler {
    /** Thread-safe queue of music tracks in FIFO order. */
    private final @NotNull Deque<AudioTrack> queue;

    /** LavaPlayer essentials. */
    public final @NotNull AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    /** The text channel in which the bot sends music-related messages. */
    private TextChannel logChannel;

    /** The voice channel in which the bot is playing music. */
    private @Nullable AudioChannel playChannel;
    private boolean isLoop;
    private boolean isSkip;

    /**
     * Constructor for the MusicHandler.
     *
     * @param audioPlayer the audio player to use for music playback.
     */
    public MusicHandler(@NotNull AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.queue = new ConcurrentLinkedDeque<>();
        this.isLoop = false;
        this.isSkip = false;
        TrackScheduler scheduler = new TrackScheduler(this);
        audioPlayer.addListener(scheduler);
    }

    /**
     * Queue a new song to the audio player.
     *
     * @param track audio track to be queued.
     */
    public void enqueue(AudioTrack track) {
        queue.addLast(track);
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(queue.peekFirst());
        }
    }

    /**
     * Pause audio player.
     */
    public void pause() {
        audioPlayer.setPaused(true);
    }

    /**
     * Resume audio player.
     */
    public void unpause() {
        audioPlayer.setPaused(false);
    }

    /**
     * Check if the audio player is paused.
     */
    public boolean isPaused() {
        return audioPlayer.isPaused();
    }

    /**
     * Disconnects from the voice channel and clears queue.
     */
    public void disconnect() {
        playChannel = null;
        queue.clear();
        audioPlayer.stopTrack();
    }

    /**
     * Sets the volume level of the audio player.
     *
     * @param volume volume level between 0-100
     */
    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    /**
     * Skips the current playing track.
     */
    public void skipTrack() {
        isSkip = true;
        AudioTrack playing = audioPlayer.getPlayingTrack();
        if (playing != null) {
            playing.setPosition(playing.getDuration());
        }
    }

    /**
     * Skips to a specific position in the queue.
     *
     * @param pos position in the queue to skip to (1-based index).
     */
    public void skipTo(int pos) {
        // If pos is 1 or less, skip the current track
        if (pos <= 1) {
            skipTrack();
            return;
        }
        // If the queue is empty, nothing to do
        if (queue.isEmpty()) {
            return;
        }

        // Collect up to (pos-1) elements *after* the head (current track)
        List<AudioTrack> toRemove = new ArrayList<>(pos - 1);
        Iterator<AudioTrack> it = queue.iterator();

        // Skip the head/current track
        if (it.hasNext()) it.next();

        // Collect the next (pos-1) tracks
        int count = 0;
        while (it.hasNext() && count < (pos - 1)) {
            toRemove.add(it.next());
            count++;
        }

        // Remove them from the deque
        for (AudioTrack t : toRemove) {
            queue.remove(t);
        }

        // Skip the current track to play the next one
        skipTrack();
    }

    /**
     * Sets the position of the current track.
     *
     * @param position position in current track in milliseconds.
     */
    public void seek(long position) {
        AudioTrack playing = audioPlayer.getPlayingTrack();
        if (playing != null && playing.isSeekable()) {
            audioPlayer.getPlayingTrack().setPosition(position);
        }
    }

    /**
     * Get the audio player queue.
     *
     * @return list of tracks in queue. Returns copy to avoid external modification.
     */
    public @NotNull LinkedList<AudioTrack> getQueue() {
        return new LinkedList<>(queue);
    }

    /**
     * Get the voice channel the bot is playing music in.
     *
     * @return voice channel playing music.
     */
    public @Nullable AudioChannel getPlayChannel() {
        return playChannel;
    }

    /**
     * Sets the music play channel.
     *
     * @param channel voice channel to set as play channel.
     */
    public void setPlayChannel(@Nullable AudioChannel channel) {
        playChannel = channel;
    }

    /**
     * Flips loop status like a switch.
     */
    public void loop() {
        isLoop = !isLoop;
    }

    /**
     * Checks if the audio player can provide audio data.
     *
     * @return true if audio data is available, false otherwise.
     */
    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    /**
     * Provides the audio data as a ByteBuffer.
     *
     * @return ByteBuffer containing the audio data.
     */
    @Override
    public boolean isOpus() {
        return true;
    }

    /**
     * Provides the audio data in 20ms chunks.
     *
     * @return ByteBuffer containing the audio data for the current frame.
     */
    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    /**
     * Manages audio events and schedules tracks.
     */
    public static class TrackScheduler extends AudioEventAdapter {

        private final MusicHandler handler;

        /**
         * Constructor for the TrackScheduler.
         *
         * @param handler the music handler instance to manage tracks.
         */
        public TrackScheduler(MusicHandler handler) {
            this.handler = handler;
        }

        /**
         * Creates an embed message when a track starts that displays relevant info.
         *
         * @param player the audio player
         * @param track  the track that is starting.
         */
        @Override
        public void onTrackStart(AudioPlayer player, @NotNull AudioTrack track) {
            handler.logChannel.sendMessageEmbeds(displayTrack(track, handler)).queue();
        }

        /**
         * Handles the end of a track, either looping it or playing the next track in the queue.
         *
         * @param player    the audio player
         * @param track     the track that ended
         * @param endReason the reason why the track ended
         */
        @Override
        public void onTrackEnd(@NotNull AudioPlayer player, @NotNull AudioTrack track, @NotNull AudioTrackEndReason endReason) {
            if (handler.isLoop() && !handler.isSkip) {
                handler.queue.pollFirst();                  // remove old head
                handler.queue.addFirst(track.makeClone());  // push clone
                player.playTrack(handler.queue.peekFirst());
                return;
            }
            handler.isSkip = false;
            handler.queue.pollFirst(); // remove the finished one
            if (endReason.mayStartNext) {
                AudioTrack next = handler.queue.peekFirst();
                if (next != null) player.playTrack(next);
            }
        }

        /**
         * Handles exceptions that occur during track playback.
         *
         * @param player   the audio player
         * @param track    the track that caused the exception
         * @param exception the exception that occurred
         */
        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, @NotNull FriendlyException exception) {
            String msg = "An error occurred! " + exception.getMessage();
            handler.logChannel.sendMessageEmbeds(EmbedUtils.createError(msg)).queue();
            exception.printStackTrace();
        }

        /**
         * Handles when a track gets stuck during playback.
         *
         * @param player      the audio player
         * @param track       the track that got stuck
         * @param thresholdMs the threshold in milliseconds after which the track is considered stuck
         */
        @Override
        public void onTrackStuck(@NotNull AudioPlayer player, AudioTrack track, long thresholdMs) {
            String msg = "Track got stuck, attempting to fix...";
            handler.logChannel.sendMessageEmbeds(EmbedUtils.createError(msg)).queue();
            handler.queue.remove(track);
            player.stopTrack();
            player.playTrack(handler.queue.getFirst());
        }
    }

    /**
     * Creates a thumbnail URL with the track image.
     * @param track the AudioTrack object from the music player.
     *
     * @return a URL to the song video thumbnail.
     */
    private static String getThumbnail(AudioTrack track) {
        // ToDO: Implement a method to retrieve the thumbnail URL from the track.
        return null;
    }

    /**
     * Creates an embed displaying details about a track.
     *
     * @param track the track to display details about.
     * @param handler the music handler instance.
     * @return a MessageEmbed displaying track details.
     */
    public static MessageEmbed displayTrack(AudioTrack track, MusicHandler handler) {
        String duration = MusicListener.formatTrackLength(track.getInfo().length);
        String repeat = (handler.isLoop()) ? "Enabled" : "Disabled";
        String userMention = "<@!"+track.getUserData(String.class)+">";
        return new EmbedBuilder()
                .setTitle("Now Playing")
                .setDescription("[" + track.getInfo().title + "](" + track.getInfo().uri + ")")
                .addField("Duration", "`"+duration+"`", true)
                .addField("Queue", "`"+(handler.queue.size()-1)+"`", true)
                .addField("Volume", "`"+handler.audioPlayer.getVolume()+"%`", true)
                .addField("Requester", userMention, true)
                .addField("Link", "[`Click Here`]("+track.getInfo().uri+")", true)
                .addField("Repeat", "`"+repeat+"`", true)
                .setColor(EmbedColor.DEFAULT.color)
                .setThumbnail(getThumbnail(track))
                .build();
    }
}