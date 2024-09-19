package org.redacted.util.placeholders;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for placeholder creation.
 *
 * @author Sparky
 */
public interface PlaceholderFactory extends Supplier<Placeholder> {

    /**
     * Assemble factory privately. Since this is a utility class, this is blocked off.
     *
     * @param placeholder The placeholder to modify and supply.
     * @return The factory.
     */
    private static PlaceholderFactory assemble(Placeholder placeholder) {
        return () -> placeholder;
    }

    /**
     * Generate factory from an event and add supported capabilities.
     * Currently only works with select events. Add more as needed.
     *
     * @param event the event to generate capabilities from.
     */
    static @NotNull PlaceholderFactory fromEvent(GenericEvent event) {
        PlaceholderFactory factory = assemble(new Placeholder());
        if (event instanceof GenericGuildMemberEvent evt) { //Generic Member Events
            factory.withGuildCapabilities(evt.getGuild())
                    .withUserCapabilities(evt.getUser());
        } else if (event instanceof GuildMemberRemoveEvent evt) { //Member Leave Event
            factory.withGuildCapabilities(evt.getGuild())
                    .withUserCapabilities(evt.getUser());
        }
        return factory;
    }

    /**
     * Generate placeholder factory from a message.
     *
     * @param msg the message to generate capabilities from.
     */
    static @NotNull PlaceholderFactory fromMessage(@NotNull Message msg) {
        return assemble(new Placeholder())
                .withGuildCapabilities(msg.getGuild())
                .withChannelCapabilities(msg.getChannel())
                .withUserCapabilities(msg.getAuthor());
    }

    /**
     * Generate placeholder factory from slash command event.
     * Used /buy and /use commands for item reply message parsing.
     *
     * @param event the event to generate capabilities from.
     */
    static @NotNull PlaceholderFactory fromSlashCommand(SlashCommandInteractionEvent event) {
        PlaceholderFactory factory = assemble(new Placeholder());
        factory.withGuildCapabilities(Objects.requireNonNull(event.getGuild()))
                .withUserCapabilities(event.getUser())
                .withChannelCapabilities(event.getChannel())
                .withMemberCapabilities(Objects.requireNonNull(event.getMember()));
        return factory;
    }

    /**
     * Add voice channel capabilities with JDA.
     *
     * @param state The voice state of the guild.
     */
    @SuppressWarnings("unused")
    default @NotNull PlaceholderFactory withVoiceCapabilities(@NotNull GuildVoiceState state) {
        accept(placeholder -> {
            placeholder.add("voice_channel_name", Objects.requireNonNull(state.getChannel()).getName());
            placeholder.add("voice_channel_id", state.getChannel().getId());
            placeholder.add("voice_channel_bitrate", String.valueOf(state.getChannel().getBitrate()));
            placeholder.add("voice_channel_members", state.getChannel().getMembers().toString());
        });
        return this;
    }

    /**
     * Add user capabilities with JDA.
     *
     * @param user The specified Use entity.
     */
    default @NotNull PlaceholderFactory withUserCapabilities(@NotNull User user) {
        accept(placeholder -> {
            placeholder.add("user", user.getName());
            placeholder.add("target", user.getName());
            placeholder.add("username", user.getName());
            placeholder.add("user_id", user.getId());
            placeholder.add("user_mention", "<@!" + user.getId() + ">");
            placeholder.add("mention", "<@!" + user.getId() + ">");
            placeholder.add("avatar", user.getEffectiveAvatarUrl());
            placeholder.add("user_avatar", user.getEffectiveAvatarUrl());
            placeholder.add("user_icon", user.getEffectiveAvatarUrl());
        });
        return this;
    }

    /**
     * Add guild member capabilities with JDA.
     *
     * @param member The specified Member entity.
     */
    @SuppressWarnings("unused")
    default void withMemberCapabilities(@NotNull Member member) {
        withUserCapabilities(member.getUser())
                .withGuildCapabilities(member.getGuild())
                .accept(placeholder -> {
                    placeholder.add("member_name", member.getEffectiveName());
                    placeholder.add("member_nickname", member.getNickname());
                });
    }

    /**
     * Add guild capabilities with JDA.
     *
     * @param guild The specified Guild.
     */
    @SuppressWarnings("UnusedReturnValue")
    default @NotNull PlaceholderFactory withGuildCapabilities(@NotNull Guild guild) {
        accept(placeholder -> {
            placeholder.add("server", guild.getName());
            placeholder.add("guild", guild.getName());
            placeholder.add("guild_name", guild.getName());
            placeholder.add("guild_id", guild.getId());
            placeholder.add("guild_icon", guild.getIconUrl());
            placeholder.add("guild_banner", guild.getBannerUrl());
            placeholder.add("guild_owner_id", guild.getOwnerId());
        });
        return this;
    }

    /**
     * Add channel capabilities with JDA.
     *
     * @param channel The message channel.
     */
    @SuppressWarnings("UnusedReturnValue")
    default @NotNull PlaceholderFactory withChannelCapabilities(MessageChannelUnion channel) {
        if (channel instanceof GuildChannel gc) {
            withGuildCapabilities(gc.getGuild()).accept(placeholder -> {
                placeholder.add("channel_category_name", Objects.requireNonNull(gc.getPermissionContainer()).getName());
                placeholder.add("channel_category_id", gc.getPermissionContainer().getId());
            });
        }
        accept(placeholder -> {
            placeholder.add("channel", channel.getName());
            placeholder.add("channel_name", channel.getName());
            placeholder.add("channel_id", channel.getId());
            placeholder.add("channel_mention", "<#" + channel.getId() + ">");
        });
        return this;
    }

    /**
     * Accept the given consumer using the placeholder.
     *
     * @param consumer Placeholder consumer.
     */
    default void accept(Consumer<Placeholder> consumer) {
        consumer.accept(get());
    }
}
