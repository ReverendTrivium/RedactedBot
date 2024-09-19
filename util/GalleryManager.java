package org.redacted.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.redacted.util.embeds.EmbedColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryManager {
    public final Map<Long, List<String>> galleries = new HashMap<>();
    public final Map<Long, Integer> currentPage = new HashMap<>();

    public void addGallery(long messageId, List<String> urls) {
        galleries.put(messageId, urls);
        currentPage.put(messageId, 0);
    }

    public boolean isGalleryMessage(long messageId) {
        return galleries.containsKey(messageId);
    }

    public void handleReaction(MessageChannel channel, long messageId, String componentId) {
        List<String> urls = galleries.get(messageId);
        int page = currentPage.get(messageId);

        if (componentId.equals("prev")) {
            page = (page - 1 + urls.size()) % urls.size();
        } else if (componentId.equals("next")) {
            page = (page + 1) % urls.size();
        }

        currentPage.put(messageId, page);
        String url = urls.get(page);

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("Here's a random NSFW gallery image")
                .setImage(url)
                .setFooter("Page " + (page + 1) + "/" + urls.size());

        channel.editMessageEmbedsById(messageId, embed.build()).queue();
    }

    public void addButtons(Message message, int totalPages) {
        Button prevButton = Button.primary("prev", "Previous").asDisabled();
        Button nextButton = Button.primary("next", "Next");

        if (totalPages > 1) {
            nextButton = nextButton.asEnabled();
        }

        message.editMessageComponents(ActionRow.of(prevButton, Button.secondary("page", "1/" + totalPages).asDisabled(), nextButton)).queue();
    }

    public void updateButtons(Message message, int currentPage, int totalPages) {
        Button prevButton = Button.primary("prev", "Previous");
        Button nextButton = Button.primary("next", "Next");

        if (currentPage == 0) {
            prevButton = prevButton.asDisabled();
        }
        if (currentPage == totalPages - 1) {
            nextButton = nextButton.asDisabled();
        }

        message.editMessageComponents(ActionRow.of(prevButton, Button.secondary("page", (currentPage + 1) + "/" + totalPages).asDisabled(), nextButton)).queue();
    }
}
