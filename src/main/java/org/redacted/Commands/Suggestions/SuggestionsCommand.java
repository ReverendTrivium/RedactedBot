package org.redacted.Commands.Suggestions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
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
import org.redacted.Handlers.SuggestionHandler;
import org.redacted.Redacted;
import org.redacted.listeners.ButtonListener;
import org.redacted.util.embeds.EmbedUtils;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Admin command to setup and modify the suggestion board.
 *
 * @author Derrick Eberlein
 */
public class SuggestionsCommand extends Command {

    /**
     * Constructor for the SuggestionsCommand.
     * Initializes the command with its name, description, category, and required permissions.
     *
     * @param bot The Redacted bot instance.
     */
    public SuggestionsCommand(Redacted bot) {
        super(bot);
        this.name = "create-suggestions";
        this.description = "Setup and modify the suggestions config.";
        this.category = Category.SUGGESTIONS;
        this.permission = Permission.MANAGE_SERVER;
        this.subCommands.add(new SubcommandData("create", "Sets a channel to become the suggestion board.")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "The channel to set as the suggestion board")
                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)));
        this.subCommands.add(new SubcommandData("dm", "Toggle private messages on suggestion response."));
        this.subCommands.add(new SubcommandData("anonymous", "Toggle anonymous mode for suggestions."));
        this.subCommands.add(new SubcommandData("config", "Display the current suggestions config."));
        this.subCommands.add(new SubcommandData("reset", "Reset all suggestion board data and settings."));
    }

    /**
     * Executes the SuggestionsCommand.
     * This method handles the interaction when the command is invoked.
     * It processes the setup and modification of the suggestion board.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Guild guild = event.getGuild();
        SuggestionHandler suggestionHandler = GuildData.get(Objects.requireNonNull(guild), bot).getSuggestionHandler();

        String text = null;
        switch(Objects.requireNonNull(event.getSubcommandName())) {
            case "create" -> {
                // Setup suggestion board
                OptionMapping channelOption = event.getOption("channel");
                if (channelOption == null) {
                    // Create new suggestion channel
                    guild.createTextChannel("suggestions").queue(channel -> {
                        ArrayList<Permission> denyPerms = new ArrayList<>();
                        denyPerms.add(Permission.MESSAGE_ADD_REACTION);
                        denyPerms.add(Permission.MESSAGE_SEND);

                        ArrayList<Permission> allowPerms = new ArrayList<>();
                        allowPerms.add(Permission.VIEW_CHANNEL);
                        allowPerms.add(Permission.MESSAGE_HISTORY);

                        channel.upsertPermissionOverride(guild.getPublicRole()).deny(denyPerms).setAllowed(allowPerms).queue();
                        suggestionHandler.setChannel(channel.getIdLong());
                    });
                    text = EmbedUtils.BLUE_TICK + " Created a new suggestion channel!";
                } else {
                    // Set suggestion board to mentioned channel
                    try {
                        long channel = channelOption.getAsChannel().getIdLong();
                        String channelMention = "<#" + channel + ">";
                        suggestionHandler.setChannel(channel);
                        text = EmbedUtils.BLUE_TICK + " Set the suggestion channel to " + channelMention;
                    } catch (NullPointerException e) {
                        text = "You can only set a text channel as the suggestion board!";
                        event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
                        return;
                    }
                }
            }
            case "dm" -> {
                boolean isEnabled = suggestionHandler.toggleResponseDM();
                if (isEnabled) {
                    text = EmbedUtils.BLUE_TICK + " Response DMs have been **enabled** for suggestions!";
                } else {
                    text = EmbedUtils.BLUE_X + " Response DMs have been **disabled** for suggestions!";
                }
            }
            case "anonymous" -> {
                boolean isEnabled = suggestionHandler.toggleAnonymous();
                if (isEnabled) {
                    text = EmbedUtils.BLUE_TICK + " Anonymous mode has been **enabled** for suggestions!";
                } else {
                    text = EmbedUtils.BLUE_X + " Anonymous mode has been **disabled** for suggestions!";
                }
            }
            case "config" -> {
                text = "";
                if (suggestionHandler.getChannel() != null) {
                    text += "\n**Channel:** <#" + suggestionHandler.getChannel() + ">";
                } else {
                    text += "\n**Channel:** none";
                }
                text += "\n**DM on Response:** " + suggestionHandler.hasResponseDM();
                text += "\n**Anonymous Mode:** " + suggestionHandler.isAnonymous();
                event.getHook().sendMessage(text).queue();
                return;
            }
            case "reset" -> {
                text = "Would you like to reset the suggestions system?\nThis will delete **ALL** data!";
                WebhookMessageCreateAction<Message> action = event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text));
                ButtonListener.sendResetMenu(event.getUser().getId(), "Suggestion", action);
                return;
            }
        }
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
