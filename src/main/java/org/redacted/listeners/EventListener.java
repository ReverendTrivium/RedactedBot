package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Listens to all events handled on server
 * and sends responses to all actions specified.
 *
 * @author Derrick Eberlein
 */
public class EventListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        for(Guild g : event.getJDA().getGuilds()) {
            System.out.println("Testing getID ServerID: " + g.getId());
            System.out.println("Testing Server Name: " + g.getName());
        }
    }
}

