package org.redacted.Commands.Blacklist;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.Document;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command that adds users to the blacklist
 *
 * @author Derrick Eberlein
 */
public class BlacklistAddCommand extends Command {

    public BlacklistAddCommand(Redacted bot) {
        super(bot);
        this.name = "add";
        this.description = "Adds a user to the blacklist";
        this.category = Category.STAFF;
        this.args.add(new OptionData(OptionType.STRING, "firstname", "The first name of the user").setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "lastname", "The last name of the user").setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "social-media-handle", "The social media handle of the user").setRequired(true));
        this.args.add(new OptionData(OptionType.STRING, "platform", "The social media platform (instagram or facebook)").setRequired(true)
                .addChoice("Instagram", "instagram")
                .addChoice("Facebook", "facebook"));
        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String firstName = Objects.requireNonNull(event.getOption("firstname")).getAsString();
        String lastName = Objects.requireNonNull(event.getOption("lastname")).getAsString();
        String socialMediaHandle = Objects.requireNonNull(event.getOption("social-media-handle")).getAsString();
        String platform = Objects.requireNonNull(event.getOption("platform")).getAsString();

        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        MongoCollection<Document> blacklistCollection = bot.database.getGuildCollection(guildId, "blacklist");

        Document newBlacklistEntry = new Document("firstname", firstName)
                .append("lastname", lastName)
                .append("socialmedia", new Document(platform, socialMediaHandle));

        blacklistCollection.insertOne(newBlacklistEntry);

        event.reply(String.format("User %s %s with %s handle %s has been added to the blacklist.",
                firstName, lastName, platform, socialMediaHandle)).queue();
    }
}