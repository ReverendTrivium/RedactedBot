package org.redacted.listeners;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * GalleryReactionListener Class
 * This class listens for button interactions in gallery messages and handles reactions.
 * It updates the gallery message buttons based on the user's interaction.
 *
 * @author Derrick Eberlein
 */
public class GalleryReactionListener extends ListenerAdapter {
    private final Redacted bot;

    /**
     * Constructs a GalleryReactionListener with the provided Redacted bot instance.
     *
     * @param bot the Redacted bot instance
     */
    public GalleryReactionListener(Redacted bot) {
        this.bot = bot;
    }

    /**
     * Handles button interactions in gallery messages.
     * If the interaction is from a user (not a bot), it processes the reaction
     * and updates the gallery message buttons accordingly.
     *
     * @param event The ButtonInteractionEvent containing the interaction details.
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (Objects.requireNonNull(event.getUser()).isBot()) return;

        long messageId = event.getMessageIdLong();
        String componentId = event.getComponentId();

        if (bot.getGalleryManager().isGalleryMessage(messageId)) {
            bot.getGalleryManager().handleReaction(event.getChannel(), messageId, componentId);
            bot.getGalleryManager().updateButtons(event.getMessage(), bot.getGalleryManager().currentPage.get(messageId), bot.getGalleryManager().galleries.get(messageId).size());
            event.deferEdit().queue();
        }
    }
}
