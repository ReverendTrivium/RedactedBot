package org.redacted.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.redacted.Handlers.economy.EconomyHandler;
import org.redacted.Database.Data.GuildData;
import org.redacted.Redacted;
import org.redacted.util.channels.ChannelFinder;

import java.util.HashMap;
import java.util.Map;

public class MessageListener extends ListenerAdapter {

    private final Redacted bot;
    private final Map<Long, Long> userCooldowns;
    private final static long COOLDOWN_TIME = 60000;

    public MessageListener(Redacted bot) {
        this.bot = bot;
        this.userCooldowns = new HashMap<>();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // Ignore messages that are not from a guild
        if (!event.isFromGuild()) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();

        // Ignore messages from bots
        if (member == null || member.getUser().isBot()) {
            return;
        }

        long userId = event.getAuthor().getIdLong();
        long guildId = guild.getIdLong();

        // Get the current time
        long currentTime = System.currentTimeMillis();

        // Check if the user is on cooldown
        if (userCooldowns.containsKey(userId)) {
            long lastMessageTime = userCooldowns.get(userId);
            if (currentTime - lastMessageTime < COOLDOWN_TIME) {
                // User is still on cooldown, return early
                return;
            }
        }

        // Fetch GuildData and EconomyHandler
        GuildData guildData = GuildData.get(guild, bot);
        EconomyHandler economyHandler = guildData.getEconomyHandler();

        // User is not on cooldown, reward them with currency
        int rewardAmount = 10; // Example reward amount
        economyHandler.addMoney(userId, rewardAmount);

        // Send a message to a specific channel
        String rewardMessage = String.format("%s has been rewarded with %d %s for being active!", member.getAsMention(), rewardAmount, economyHandler.getCurrency());
        sendMessageToChannelByName(guild, rewardMessage);

        // Update the user's last message time to now
        userCooldowns.put(userId, currentTime);
    }

    private void sendMessageToChannelByName(Guild guild, String message) {
        Long channelId = ChannelFinder.getChannelIdByName(guild, "bot-notifications");
        if (channelId != null) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
            }
        } else {
            System.out.println("Channel with name 'bot-notifications' not found.");
        }
    }
}