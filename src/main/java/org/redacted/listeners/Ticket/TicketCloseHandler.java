package org.redacted.listeners.Ticket;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.models.Ticket;
import org.redacted.Redacted;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TicketCloseHandler extends ListenerAdapter {

    private final Redacted bot;

    public TicketCloseHandler(Redacted bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ", 3);
        if (!args[0].equalsIgnoreCase("-ticket") || !args[1].equalsIgnoreCase("close")) return;

        TextChannel channel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        Member author = event.getMember();

        String closeReason = args.length >= 3 ? args[2] : "No reason provided.";
        String channelName = channel.getName();
        if (!channelName.matches("\\d+-.*")) {
            channel.sendMessage("❌ This doesn't look like a ticket channel.").queue();
            return;
        }

        String ticketNumber = channelName.split("-")[0];

        // Fetch ticket data
        MongoCollection<Ticket> collection = GuildData.getDatabase().getTicketCollection(guild.getIdLong());
        Ticket ticket = collection.find(new Document("ticketNumber", Integer.parseInt(ticketNumber))).first();

        if (ticket == null) {
            channel.sendMessage("❌ Ticket data not found in the database.").queue();
            return;
        }

        // Format transcript
        channel.getHistory().retrievePast(100).queue(messages -> {
            messages.sort(Comparator.comparing(ISnowflake::getTimeCreated));

            StringBuilder transcript = new StringBuilder();
            transcript.append("Transcript of ticket #").append(ticketNumber)
                    .append(" – ").append(ticket.getReason())
                    .append(", opened by ").append(ticket.getCreatorTag()).append(".\n\n");

            for (Message msg : messages) {
                transcript.append("[").append(msg.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy MMM dd HH:mm:ss")))
                        .append("] ").append(msg.getAuthor().getName())
                        .append(" (").append(msg.getAuthor().getId()).append("): ")
                        .append(msg.getContentDisplay()).append("\n");
            }

            // Save transcript to file
            String transcriptName = "transcript-" + ticketNumber + "-" + ticket.getReason().replaceAll(" ", "_") + ".txt";
            File transcriptFile = new File(transcriptName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcriptFile))) {
                writer.write(transcript.toString());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Download media
            File mediaDir = new File("media-" + ticketNumber);
            mediaDir.mkdir();

            int counter = 0;
            for (Message msg : messages) {
                for (Message.Attachment att : msg.getAttachments()) {
                    try {
                        String fileExt = att.getFileExtension() != null ? "." + att.getFileExtension() : "";
                        String uniqueName = String.format("%03d_%s%s", counter++, att.getId(), fileExt);
                        att.getProxy().downloadToFile(new File(mediaDir, uniqueName)).get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Zip the media folder
            File zipFile = new File("media-" + ticketNumber + ".zip");
            zipDirectory(mediaDir, zipFile);

            // Send to logs channel
            TextChannel logChannel = guild.getTextChannelsByName("ticket-logs", true).stream().findFirst().orElse(null);
            if (logChannel != null) {
                logChannel.sendMessage("transcript-" + ticketNumber + "-" + ticket.getReason().replaceAll(" ", "_") + ".txt")
                        .addFiles(FileUpload.fromData(transcriptFile))
                        .queue();
                if (zipFile.length() > 0) {
                    logChannel.sendFiles(FileUpload.fromData(zipFile)).queue();
                }
            }

            // Cleanup: update DB + delete channel
            ticket.setStatus("closed");
            ticket.setClosedAt(Instant.now());
            ticket.setCloseReason(closeReason);
            collection.replaceOne(new Document("ticketNumber", ticket.getTicketNumber()), ticket);

            channel.delete().queue();
        });
    }

    private void zipDirectory(File sourceDir, File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            Path sourcePath = sourceDir.toPath();
            Files.walk(sourcePath).filter(Files::isRegularFile).forEach(path -> {
                ZipEntry entry = new ZipEntry(sourcePath.relativize(path).toString());
                try {
                    zos.putNextEntry(entry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
