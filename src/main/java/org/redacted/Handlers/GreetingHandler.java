package org.redacted.Handlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.conversions.Bson;
import org.redacted.Database.Database;
import org.redacted.Database.cache.Greetings;

/**
 * Handles server messages (greeting, farewell, joinDM, etc).
 *
 * @author Derrick Eberlein
 */
@Getter
public class GreetingHandler {

    private final Guild guild;
    private final MongoCollection<Greetings> greetingsCollection;
    private final Bson filter;
    private Greetings greetings;

    public GreetingHandler(Guild guild, Database database) {
        this.guild = guild;

        // Access the guild-specific greetings collection
        this.greetingsCollection = database.getGuildCollection(guild.getIdLong(), "greetings").withDocumentClass(Greetings.class);

        // Create a filter to select the current guild's greetings data
        filter = Filters.eq("guild", guild.getIdLong());

        // Retrieve or initialize the greetings data for this guild
        this.greetings = greetingsCollection.find(filter).first();
        if (greetings == null) {
            greetings = new Greetings(guild.getIdLong());
            greetingsCollection.insertOne(greetings);
        }
    }

    /**
     * Set a greeting message for this server.
     *
     * @param msg the message to send on join.
     */
    public void setGreet(String msg) {
        greetings.setGreeting(msg);
        greetingsCollection.updateOne(filter, Updates.set("greeting", msg));
    }

    /**
     * Remove greeting message from this server.
     */
    public void removeGreet() {
        greetings.setGreeting(null);
        greetingsCollection.updateOne(filter, Updates.unset("greeting"));
    }

    /**
     * Set a farewell message for this server.
     *
     * @param msg the message to send on member leave.
     */
    public void setFarewell(String msg) {
        greetings.setFarewell(msg);
        greetingsCollection.updateOne(filter, Updates.set("farewell", msg));
    }

    /**
     * Remove farewell message from this server.
     */
    public void removeFarewell() {
        greetings.setFarewell(null);
        greetingsCollection.updateOne(filter, Updates.unset("farewell"));
    }

    /**
     * Set a join DM message for this server.
     *
     * @param msg the message to send on member join.
     */
    public void setJoinDM(String msg) {
        greetings.setJoinDM(msg);
        greetingsCollection.updateOne(filter, Updates.set("join_dm", msg));
    }

    /**
     * Remove join DM message from this server.
     */
    public void removeJoinDM() {
        greetings.setJoinDM(null);
        greetingsCollection.updateOne(filter, Updates.unset("join_dm"));
    }

    /**
     * Set the welcome channel.
     *
     * @param channelID the ID of the channel to set.
     */
    public void setChannel(Long channelID) {
        greetings.setWelcomeChannel(channelID);
        greetingsCollection.updateOne(filter, Updates.set("welcome_channel", channelID));
    }

    /**
     * Remove the welcome channel.
     */
    public void removeChannel() {
        greetings.setWelcomeChannel(null);
        greetingsCollection.updateOne(filter, Updates.unset("welcome_channel"));
    }

    /**
     * Gets the ID of the welcome channel.
     *
     * @return long id of the welcome channel.
     */
    public Long getChannel() {
        return greetings.getWelcomeChannel();
    }

    /**
     * Getter for greetings config POJO object.
     *
     * @return instance of greetings config POJO.
     */
    public Greetings getConfig() {
        return greetings;
    }

    /**
     * Resets all config data for greeting system.
     */
    public void reset() {
        greetings = new Greetings(guild.getIdLong());
        greetingsCollection.replaceOne(filter, greetings);
    }
}
