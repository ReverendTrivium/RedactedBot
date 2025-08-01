package org.redacted.Commands.Utility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.redacted.Commands.Category;
import org.redacted.Commands.Command;
import org.redacted.Redacted;
import org.redacted.util.embeds.EmbedColor;

/**
 * Ping command to check latency with Discord API.
 *
 * @author Derrick Eberlein
 */
public class Ping extends Command {

    /**
     * Constructor for the Ping command.
     * Initializes the command with its name, description, and category.
     *
     * @param bot The Redacted bot instance.
     */
    public Ping(Redacted bot) {
        super(bot);
        this.name = "ping";
        this.description = "Display bot latency.";
        this.category = Category.UTILITY;
    }

    /**
     * Executes the Ping command.
     * This method handles the interaction when the command is invoked.
     * It calculates the latency and sends a response with the ping information.
     *
     * @param event The SlashCommandInteractionEvent containing the command interaction data.
     */
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        long time = System.currentTimeMillis();
        event.getHook().sendMessage(":signal_strength: Ping").queue(m -> {
            long latency = System.currentTimeMillis() - time;
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(":ping_pong: Pong!");
            embed.addField("Latency", latency + "ms", false);
            embed.addField("Discord API", event.getJDA().getGatewayPing() + "ms", false);
            embed.setColor(EmbedColor.DEFAULT.color);
            m.editMessageEmbeds(embed.build()).queue();
        });
    }
}
