package org.redacted.Commands.Greetings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Database.Data.GuildData;
import org.redacted.Database.cache.Greetings;
import org.redacted.Handlers.GreetingHandler;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;
import org.redacted.util.embeds.EmbedUtils;

import java.util.Objects;


/**
 * Command that displays and modifies greetings config.
 *
 * @author Derrick Eberlein
 */
public class GreetingsCommand extends Command {

    /**
     * Constructor for the GreetingsCommand.
     * Initializes the command with its name, description, category, subcommands, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public GreetingsCommand(Redacted bot) {
        super(bot);
        this.name = "greetings-config";
        this.description = "Modify this server's greetings config.";
        this.category = Category.GREETINGS;
        this.permission = Permission.MANAGE_SERVER;
        this.subCommands.add(new SubcommandData("channel", "Set a channel to send welcome messages to.")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "The channel to send welcome messages to")
                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)));
        this.subCommands.add(new SubcommandData("config", "Display the greetings config for this server."));
        this.subCommands.add(new SubcommandData("reset", "Reset all greetings data and settings."));
    }

    /**
     * Executes the greetings command.
     * This method handles the interaction when the command is invoked.
     * It allows users to set or remove a welcome channel, display the current greetings config, or reset the greetings system.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        GreetingHandler greetingHandler = GuildData.get(Objects.requireNonNull(event.getGuild()), bot).getGreetingHandler();

        String text = "";
        switch(Objects.requireNonNull(event.getSubcommandName())) {
            case "channel" -> {
                OptionMapping channelOption = event.getOption("channel");
                if (channelOption == null) {
                    // Remove welcome channel if not specified
                    greetingHandler.removeChannel();
                    text = EmbedUtils.BLUE_X + " Welcome channel successfully removed!";
                } else {
                    // Set welcome channel
                    Long channelID = channelOption.getAsChannel().getIdLong();
                    greetingHandler.setChannel(channelID);
                    text = EmbedUtils.BLUE_X + " Welcome channel set to <#" + channelID + ">";
                }
            }
            case "config" -> {
                text = configToString(greetingHandler.getConfig());
                event.getHook().sendMessage(text).queue();
                return;
            }
            case "reset" -> {
                text = "Would you like to reset the greeting system?\nThis will delete **ALL** data!";
                WebhookMessageCreateAction<Message> action = event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text));
                ButtonListener.sendResetMenu(event.getUser().getId(), "Greeting", action);
                return;
            }
        }
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }

    /**
     * Converts the greetings config into a readable string
     *
     * @param greetings an instance of the guild greetings config.
     * @return Stringified config (greetings only).
     */
    private String configToString(Greetings greetings) {
        String text = "";
        if (greetings.getWelcomeChannel() == null) {
            text += "**Welcome Channel:** none\n";
        } else {
            text += "**Welcome Channel:** <#" + greetings.getWelcomeChannel() + ">\n";
        }
        if (greetings.getGreeting() == null) {
            text += "**Greeting:** none\n";
        } else {
            text += "**Greeting:** " + greetings.getGreeting() + "\n";
        }
        if (greetings.getFarewell() == null) {
            text += "**Farewell:** none\n";
        } else {
            text += "**Farewell:** " + greetings.getFarewell() + "\n";
        }
        if (greetings.getJoinDM() == null) {
            text += "**Join DM:** none\n";
        } else {
            text += "**Join DM:** " + greetings.getJoinDM() + "\n";
        }
        return text;
    }
}
