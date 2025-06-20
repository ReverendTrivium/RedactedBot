package org.redacted.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.Ticket;
import org.redacted.Redacted;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TicketListener Class
 * This class listens for messages in a specific channel to open tickets for user feedback or reporting.
 * It creates a private text channel for the user and admins to discuss the ticket.
 *
 * @author Derrick Eberlein
 */
public class TicketListener extends ListenerAdapter {

    private static final Pattern TICKET_OPEN_PATTERN = Pattern.compile("^-ticket open (.+)$", Pattern.CASE_INSENSITIVE);
    private static final String SUBMIT_CHANNEL_NAME = "submit-ticket";
    private static final String CATEGORY_NAME = "Private Feedback / Reporting";
    private static final String ADMIN_ROLE_NAME = "Admin";

    /**
     * Constructs a TicketListener with the provided Redacted bot instance.
     *
     * @param bot the Redacted bot instance
     */
    public TicketListener(Redacted bot) {
    }

    /**
     * Handles incoming messages to check for the "-ticket open" command.
     * If the command is detected, it creates a new ticket channel for the user.
     *
     * @param event The MessageReceivedEvent containing the message and context.
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        TextChannel channel = event.getChannel().asTextChannel();
        if (!channel.getName().equalsIgnoreCase(SUBMIT_CHANNEL_NAME)) return;

        Matcher matcher = TICKET_OPEN_PATTERN.matcher(event.getMessage().getContentRaw());
        if (!matcher.matches()) return;

        String reasonRaw = matcher.group(1).trim();
        String sanitizedReason = reasonRaw.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (member == null) return;

        // Get category
        Category targetCategory = guild.getCategoriesByName(CATEGORY_NAME, true).stream().findFirst().orElse(null);
        if (targetCategory == null) {
            channel.sendMessage("❌ Could not find category: " + CATEGORY_NAME).queue();
            return;
        }

        // Get open ticket count
        long openCount = guild.getTextChannels().stream()
                .filter(tc -> tc.getParentCategory() != null && tc.getParentCategory().getName().equals(CATEGORY_NAME))
                .filter(tc -> tc.getName().matches("^\\d+-.*"))
                .count();

        int ticketNumber = (int) openCount + 1;
        String channelName = ticketNumber + "-" + sanitizedReason;

        // Setup permissions
        Role adminRole = guild.getRolesByName(ADMIN_ROLE_NAME, true).stream().findFirst().orElse(null);
        if (adminRole == null) {
            channel.sendMessage("❌ Admin role not found.").queue();
            return;
        }

        guild.createTextChannel(channelName, targetCategory)
                .addPermissionOverride(guild.getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null)
                .addPermissionOverride(adminRole, List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null)
                .queue(newChannel -> {

                    // Save to MongoDB
                    Ticket ticket = new Ticket();
                    ticket.setTicketId(ticketNumber);
                    ticket.setReason(reasonRaw);
                    ticket.setChannelId(newChannel.getId());
                    ticket.setGuildId(guild.getIdLong());
                    ticket.setUserId(member.getIdLong());
                    ticket.setCreatorUsername(member.getUser().getName() + "#" + member.getUser().getDiscriminator());
                    ticket.setStatus("open");
                    ticket.setOpenedAt(Instant.now());
                    ticket.setClosedAt(Instant.EPOCH);
                    ticket.setCloseReason("Not yet closed");

                    GuildData.getDatabase()
                            .getTicketCollection(guild.getIdLong())
                            .insertOne(ticket);

                    // Send styled embed
                    EmbedBuilder embed = getEmbedBuilder(member);
                    newChannel.sendMessageEmbeds(embed.build()).queue();
                });

        // Optionally delete the command message
        event.getMessage().delete().queue();
    }

    /**
     * Creates an EmbedBuilder with a welcome message for the ticket channel.
     *
     * @param member The Member who opened the ticket.
     * @return An EmbedBuilder with the welcome message.
     */
    private static @NotNull EmbedBuilder getEmbedBuilder(Member member) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 101, 242));
        embed.setDescription(String.format("""
        Welcome <@%s>

        Please describe the reasoning for opening this ticket, include any information you think may be relevant such as proof, other third parties and so on.

        **Use the following command to close the ticket**
        `-ticket close <reason>`

        **Use the following command to add users to the ticket**
        `-ticket adduser @user`

        **Use the following command to remove users from the ticket**
        `-ticket removeuser @user`
        """, member.getId()));
        return embed;
    }
}
