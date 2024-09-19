package org.redacted.listeners;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Redacted;

import java.util.Objects;

public class GalleryReactionListener extends ListenerAdapter {
    private final Redacted bot;

    public GalleryReactionListener(Redacted bot) {
        this.bot = bot;
    }

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
