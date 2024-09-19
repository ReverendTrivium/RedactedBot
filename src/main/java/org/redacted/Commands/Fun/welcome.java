package org.redacted.Commands.Fun;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;

import java.util.Objects;

/**
 * Command welcomes a user to the server!
 *
 * @author Derrick Eberlein
 */
public class welcome extends Command {
    public welcome(Redacted bot) {
        super(bot);
        this.name = "welcome";
        this.description = "Get welcome by the bot!";
        this.category = Category.FUN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // run the '/welcome' command
        User user = event.getUser();
        String nickname = Objects.requireNonNull(event.getMember()).getNickname();
        if (nickname == null) {
            nickname = user.getName();
        }
        System.out.println("Nickname: " + nickname);
        event.reply("Welcome to the server **" + nickname + "**!").setEphemeral(true).queue();
    }
}
