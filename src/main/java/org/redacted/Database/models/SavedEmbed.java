package org.redacted.Database.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
