package org.redacted.Handlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.Database;
import org.redacted.Database.cache.Suggestion;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedUtils;

import java.util.List;
import java.util.Objects;

/**
 * Handles the suggestion board for a guild.
 *
 * @author Derrick Eberlein
 */
public class SuggestionHandler {

    private final Redacted bot;
    private final Guild guild;
    private final MongoCollection<Suggestion> suggestionsCollection;
    private Suggestion suggestions;

    /**
     * Sets up the local cache for this guild's suggestions from MongoDB.
     *
     * @param guild Instance of the guild this handler is for.
     * @param database The database instance to interact with.
     */
    public SuggestionHandler(Guild guild, Redacted bot, Database database) { // Accept bot instance here
        this.bot = bot; // Directly assign the bot instance
        this.guild = guild;

        // Get or create the suggestions collection for the guild
        this.suggestionsCollection = database.getGuildCollection(guild.getIdLong(), "suggestions").withDocumentClass(Suggestion.class);

        // Fetch the suggestions document for this guild or create a new one
        this.suggestions = suggestionsCollection.find(Filters.eq("guild", guild.getIdLong())).first();
        if (suggestions == null) {
            suggestions = new Suggestion(guild.getIdLong());
            suggestionsCollection.insertOne(suggestions);
        }
    }

    /**
     * Sets the suggestion board channel.
     *
     * @param channelID ID of the new channel.
     */
    public void setChannel(long channelID) {
        suggestions.setChannel(channelID);
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.set("channel", channelID));
    }

    /**
     * Adds a suggestion message to the list.
     *
     * @param messageID the ID of the suggestion embed.
     * @param author the ID of the author of the suggestion.
     */
    public void add(long messageID, long author) {
        // Update local cache
        suggestions.getMessages().add(messageID);
        suggestions.getAuthors().add(author);
        suggestions.setNumber(suggestions.getNumber() + 1);

        // Update MongoDB data file
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.push("messages", messageID));
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.push("authors", author));
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.inc("number", 1));
    }

    /**
     * Resets all suggestion data locally and in MongoDB.
     */
    public void reset() {
        suggestions = new Suggestion(guild.getIdLong());
        suggestionsCollection.replaceOne(Filters.eq("guild", guild.getIdLong()), suggestions);
    }

    /**
     * Checks if the suggestion board has a channel set.
     *
     * @return true if channel set, otherwise false.
     */
    public boolean isSetup() {
        return suggestions.getChannel() != null;
    }

    /**
     * Checks if anonymous mode is turned on/off.
     *
     * @return anonymous mode boolean.
     */
    public boolean isAnonymous() {
        return suggestions.isAnonymous();
    }

    /**
     * Checks if response DMs are enabled/disabled.
     *
     * @return response DMs boolean.
     */
    public boolean hasResponseDM() {
        return suggestions.isResponseDM();
    }

    /**
     * Gets the number of the next suggestion.
     *
     * @return Next suggestion number.
     */
    public long getNumber() {
        return suggestions.getNumber();
    }

    /**
     * Gets the channel ID of the suggestion board.
     *
     * @return ID of the suggestion channel.
     */
    public Long getChannel() {
        return suggestions.getChannel();
    }

    /**
     * Gets the list of suggestion message IDs.
     *
     * @return list of suggestion message IDs.
     */
    public List<Long> getMessages() {
        return suggestions.getMessages();
    }

    /**
     * Switches on/off anonymous mode and returns the result.
     *
     * @return the resulting boolean of toggling anonymous mode.
     */
    public boolean toggleAnonymous() {
        boolean result = !suggestions.isAnonymous();
        suggestions.setAnonymous(result);
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.set("is_anonymous", result));
        return result;
    }

    /**
     * Switches on/off response DMs.
     *
     * @return the resulting boolean of toggling DMs.
     */
    public boolean toggleResponseDM() {
        boolean result = !suggestions.isResponseDM();
        suggestions.setResponseDM(result);
        suggestionsCollection.updateOne(Filters.eq("guild", guild.getIdLong()), Updates.set("response_dm", result));
        return result;
    }

    /**
     * Responds to a suggestion by editing the embed and responding to the author.
     *
     * @param event The slash command event that triggered this method.
     * @param id the id number of the suggestion to respond to.
     * @param reasonOption the reason option passed in by user.
     * @param responseType the type of response (approve, deny, etc).
     */
    public void respond(SlashCommandInteractionEvent event, int id, OptionMapping reasonOption, SuggestionResponse responseType) {
        String reason = (reasonOption != null) ? reasonOption.getAsString() : "No reason given";
        try {
            // Use the updated method signature to get GuildData
            SuggestionHandler suggestionHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getSuggestionHandler();
            TextChannel channel = event.getGuild().getTextChannelById(suggestionHandler.getChannel());
            if (channel == null) {
                throw new NullPointerException();
            }

            // Edit suggestion embed
            Message suggestionMessage = channel.retrieveMessageById(suggestionHandler.getMessages().get(id)).complete();
            MessageEmbed embed = suggestionMessage.getEmbeds().get(0);
            MessageEmbed editedEmbed = new EmbedBuilder()
                    .setAuthor(Objects.requireNonNull(embed.getAuthor()).getName(), embed.getUrl(), embed.getAuthor().getIconUrl())
                    .setTitle("Suggestion #" + (id + 1) + " " + responseType.response)
                    .setDescription(embed.getDescription())
                    .addField("Reason from " + event.getUser().getName(), reason, false)
                    .setColor(responseType.color)
                    .build();
            suggestionMessage.editMessageEmbeds(editedEmbed).queue();

            String lowercaseResponse = responseType.response.toLowerCase();
            String text = "Suggestion #" + (id + 1) + " has been " + lowercaseResponse + "!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();

            // DM Author if response DMs are turned on
            if (suggestions.isResponseDM()) {
                User author = event.getJDA().getUserById(suggestions.getAuthors().get(id));
                if (author != null) {
                    author.openPrivateChannel().queue(dm -> {
                        String dmText = "Your suggestion has been " + lowercaseResponse + " by " + event.getUser().getName();
                        dm.sendMessage(dmText).setEmbeds(editedEmbed).queue();
                    });
                }
            }

        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Invalid ID format
            String text = "Could not find a suggestion with that id number.";
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
        } catch (ErrorResponseException | NullPointerException e) {
            // Invalid channel
            String text = "Could not find that message, was the channel deleted or changed?";
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
        }
    }

    /**
     * Suggestion Response Types.
     * Includes the correct color scheme and wording.
     */
    public enum SuggestionResponse {
        APPROVE("Approved", 0xd2ffd0),
        DENY("Denied", 0xffd0ce),
        CONSIDER("Considered", 0xfdff91),
        IMPLEMENT("Implemented", 0x91fbff);

        private final String response;
        private final int color;

        SuggestionResponse(String response, int color) {
            this.response = response;
            this.color = color;
        }
    }
}
