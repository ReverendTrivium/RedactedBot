package org.redacted.Database.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * SavedEmbed Class
 * This class represents an embed message that has been saved in the database.
 * It contains fields for message ID, channel ID, guild ID, title, description,
 * author ID, timestamp, emoji-role mapping, image URL, and thumbnail URL.
 *
 * @author Derrick Eberlein
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedEmbed {
    @BsonProperty("messageId")
    private String messageId;

    @BsonProperty("channelId")
    private String channelId;

    @BsonProperty("guildId")
    private long guildId;

    @BsonProperty("title")
    private String title;

    @BsonProperty("description")
    private String description;

    @BsonProperty("authorId")
    private String authorId;

    @BsonProperty("timestamp")
    private Instant timestamp;

    @BsonProperty("emojiRoleMap")
    private Map<String, String> emojiRoleMap = new HashMap<>();

    @BsonProperty("imageUrl")
    private String imageUrl;

    @BsonProperty("thumbnailUrl")
    private String thumbnailUrl;
}
