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

/**
 * Manages galleries of images for Discord messages.
 * Allows adding galleries, handling reactions to navigate through images,
 * and updating message buttons for pagination.
 *
 * @author Derrick Eberlein
 */
public class GalleryManager {
    public final Map<Long, List<String>> galleries = new HashMap<>();
    public final Map<Long, Integer> currentPage = new HashMap<>();

    /**
     * Adds a gallery of images to a message.
     *
     * @param messageId the ID of the message to which the gallery is attached
     * @param urls      the list of image URLs in the gallery
     */
    public void addGallery(long messageId, List<String> urls) {
        galleries.put(messageId, urls);
        currentPage.put(messageId, 0);
    }

    /**
     * Checks if a message has an associated gallery.
     *
     * @param messageId the ID of the message to check
     * @return true if the message has a gallery, false otherwise
     */
    public boolean isGalleryMessage(long messageId) {
        return galleries.containsKey(messageId);
    }

    /**
     * Handles reactions to navigate through the gallery images.
     *
     * @param channel     the message channel where the reaction was added
     * @param messageId   the ID of the message with the gallery
     * @param componentId the ID of the button clicked (prev or next)
     */
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
                .setTitle("Here's a random gallery image")
                .setImage(url)
                .setFooter("Page " + (page + 1) + "/" + urls.size());

        channel.editMessageEmbedsById(messageId, embed.build()).queue();
    }

    /**
     * Adds pagination buttons to a message for navigating through the gallery.
     *
     * @param message     the message to which the buttons are added
     * @param totalPages  the total number of pages in the gallery
     */
    public void addButtons(Message message, int totalPages) {
        Button prevButton = Button.primary("prev", "Previous").asDisabled();
        Button nextButton = Button.primary("next", "Next");

        if (totalPages > 1) {
            nextButton = nextButton.asEnabled();
        }

        message.editMessageComponents(ActionRow.of(prevButton, Button.secondary("page", "1/" + totalPages).asDisabled(), nextButton)).queue();
    }

    /**
     * Updates the pagination buttons based on the current page and total pages.
     *
     * @param message      the message to update
     * @param currentPage  the current page index (0-based)
     * @param totalPages   the total number of pages in the gallery
     */
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
